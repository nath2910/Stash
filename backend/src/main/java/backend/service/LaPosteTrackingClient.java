package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@Service
@Order(100)
public class LaPosteTrackingClient implements CarrierTrackingClient {

  private static final String PROVIDER = "LA_POSTE_OKAPI";
  private static final String BROWSER_PROVIDER = "LA_POSTE_BROWSER_PAGE";
  private static final String BROWSER_SCRIPT = "laposte-browser-scrape.mjs";
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final Pattern DELIVERY_DATE_JSON_PATTERN = Pattern.compile("\"deliveryDate\"\\s*:\\s*\"([^\"]+)\"");
  private static final Pattern DELIVERY_LINE_PATTERN = Pattern.compile("(?im)^livraison\\s*:\\s*(.+)$");
  private static final Pattern ISO_TIMESTAMP_PATTERN = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:\\d{2})\\b");

  private final String apiKey;
  private final String baseUrl;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final Environment environment;

  public LaPosteTrackingClient(
      @Value("${app.delivery.laposte-api-key:}") String apiKey,
      @Value("${app.delivery.laposte-tracking-base-url:https://api.laposte.fr/suivi/v2/idships}") String baseUrl,
      ObjectMapper objectMapper,
      Environment environment
  ) {
    this.apiKey = apiKey;
    this.baseUrl = trimTrailingSlash(baseUrl);
    this.objectMapper = objectMapper;
    this.environment = environment;
    this.restClient = RestClient.builder().build();
  }

  @PostConstruct
  void validateProductionConfig() {
    for (String profile : environment.getActiveProfiles()) {
      if ("prod".equalsIgnoreCase(profile) && !isConfigured()) {
        throw new IllegalStateException(
            "LAPOSTE_API_KEY or a supported local browser runtime is required in prod for Colissimo tracking"
        );
      }
    }
  }

  @Override
  public boolean supports(Parcel parcel) {
    return TrackingCarrierRules.isValidForCarrier(normalizedTracking(parcel), "colissimo");
  }

  @Override
  public boolean isConfigured() {
    return (apiKey != null && !apiKey.isBlank()) || BrowserTrackingScriptRunner.isAvailable(BROWSER_SCRIPT);
  }

  @Override
  public Optional<TrackingSnapshot> fetchTracking(Parcel parcel) {
    boolean apiConfigured = apiKey != null && !apiKey.isBlank();
    if (!apiConfigured && !BrowserTrackingScriptRunner.isAvailable(BROWSER_SCRIPT)) {
      return Optional.empty();
    }
    if (apiConfigured) {
      try {
        Map<String, Object> response = restClient.get()
            .uri(trackingUri(parcel))
            .header("Accept", "application/json")
            .header("X-Okapi-Key", apiKey)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        if (response != null && isSuccess(response)) {
          return Optional.of(toSnapshot(parcel, response));
        }
      } catch (Exception ignored) {
        // Fall back to the browser script below.
      }
    }
    return fetchFromBrowserPage(parcel);
  }

  private TrackingSnapshot toSnapshot(Parcel parcel, Map<String, Object> response) {
    Map<String, Object> shipment = mapValue(response.get("shipment"));
    List<TrackingEventSnapshot> events = new ArrayList<>();
    Object rawEvents = shipment.get("event");
    if (rawEvents instanceof List<?> list) {
      for (Object item : list) {
        Map<String, Object> rawEvent = mapValue(item);
        if (rawEvent.isEmpty()) {
          continue;
        }
        String label = firstNonBlank(stringValue(rawEvent.get("label")), stringValue(rawEvent.get("message")));
        String code = stringValue(rawEvent.get("code"));
        OffsetDateTime eventTime = parseDateTime(stringValue(rawEvent.get("date")));
        events.add(new TrackingEventSnapshot(
            normalizeStatus(label + " " + code),
            code,
            label,
            stringValue(rawEvent.get("site")),
            eventTime,
            rawEvent
        ));
      }
    }

    TrackingEventSnapshot latest = latestEvent(events);
    OffsetDateTime deliveredAt = parseDateTime(stringValue(shipment.get("deliveryDate")));
    ResolvedStatus resolvedStatus = resolveStatus(
        events,
        stringValue(shipment.get("shortLabel")),
        stringValue(shipment.get("longLabel")),
        deliveredAt
    );
    String statusLabel = resolvedStatus.label();
    ParcelStatus status = resolvedStatus.status();

    if (deliveredAt == null && status == ParcelStatus.DELIVERED && latest != null) {
      deliveredAt = parseDateTime(String.valueOf(latest.eventTime()));
    }

    Map<String, Object> context = mapValue(shipment.get("contextData"));
    String origin = stringValue(context.get("originCountry"));
    String destination = stringValue(context.get("arrivalCountry"));

    Map<String, Object> rawPayload = new HashMap<>(shipment);
    rawPayload.put("provider_response", safeForStorage(response));

    return new TrackingSnapshot(
        PROVIDER,
        stringValue(shipment.get("idShip")),
        canonicalCarrier(parcel.getCarrierSlug(), stringValue(shipment.get("product"))),
        status,
        statusLabel,
        parseDateTime(stringValue(shipment.get("estimatedDeliveryDate"))),
        deliveredAt,
        firstNonBlank(stringValue(shipment.get("url")), fallbackTrackingUrl(parcel, stringValue(shipment.get("product")))),
        origin,
        destination,
        stringValue(shipment.get("product")),
        null,
        rawPayload,
        events
    );
  }

  private URI trackingUri(Parcel parcel) {
    String tracking = URLEncoder.encode(parcel.getTrackingNumber().trim(), StandardCharsets.UTF_8);
    return URI.create(baseUrl + "/" + tracking + "?lang=fr_FR");
  }

  private boolean isSuccess(Map<String, Object> response) {
    Object returnCode = response.get("returnCode");
    if (returnCode instanceof Number number) {
      return number.intValue() == 200;
    }
    return "200".equals(String.valueOf(returnCode));
  }

  private ParcelStatus normalizeStatus(String value) {
    return normalizeStatusStatic(value);
  }

  static TrackingEventSnapshot latestEvent(List<TrackingEventSnapshot> events) {
    if (events == null || events.isEmpty()) {
      return null;
    }
    return events.stream()
        .filter(event -> event != null && event.eventTime() != null)
        .max(Comparator.comparing(TrackingEventSnapshot::eventTime))
        .orElse(events.get(events.size() - 1));
  }

  static ResolvedStatus resolveStatus(
      List<TrackingEventSnapshot> events,
      String shortLabel,
      String longLabel,
      OffsetDateTime deliveredAt
  ) {
    List<ResolvedStatus> candidates = new ArrayList<>();
    if (events != null) {
      for (TrackingEventSnapshot event : events) {
        if (event != null) {
          candidates.add(new ResolvedStatus(event.status(), event.description()));
        }
      }
    }
    if (deliveredAt != null) {
      candidates.add(new ResolvedStatus(ParcelStatus.DELIVERED, firstNonBlankStatic(shortLabel, longLabel, "Livre")));
    }
    if (shortLabel != null && !shortLabel.isBlank()) {
      candidates.add(new ResolvedStatus(normalizeStatusStatic(shortLabel), shortLabel));
    }
    if (longLabel != null && !longLabel.isBlank()) {
      candidates.add(new ResolvedStatus(normalizeStatusStatic(longLabel), longLabel));
    }

    ResolvedStatus resolved = candidates.stream()
        .filter(candidate -> candidate.status() != null && candidate.status() != ParcelStatus.UNKNOWN)
        .max(Comparator.comparingInt(candidate -> statusPriority(candidate.status())))
        .orElseGet(() -> {
          TrackingEventSnapshot latest = latestEvent(events == null ? List.of() : events);
          String fallbackLabel = firstNonBlankStatic(
              latest == null ? null : latest.description(),
              shortLabel,
              longLabel
          );
          return new ResolvedStatus(normalizeStatusStatic(fallbackLabel), fallbackLabel);
        });
    return preferSummaryLabel(resolved, shortLabel, longLabel);
  }

  private String canonicalCarrier(String existingCarrier, String product) {
    return "colissimo";
  }

  private String fallbackTrackingUrl(Parcel parcel) {
    return fallbackTrackingUrl(parcel, null);
  }

  private String fallbackTrackingUrl(Parcel parcel, String product) {
    String trustedUrl = rawTrackingUrl(parcel);
    if (TrackingLinkResolver.isTrustedTrackingUrl(trustedUrl, "colissimo")) {
      return trustedUrl.trim();
    }
    return TrackingLinkResolver.fallbackTrackingUrl("colissimo", parcel.getTrackingNumber());
  }

  private static String browserFallbackTrackingUrl(Parcel parcel) {
    String trustedUrl = rawTrackingUrlStatic(parcel);
    if (TrackingLinkResolver.isTrustedTrackingUrl(trustedUrl, "colissimo")) {
      return trustedUrl.trim();
    }
    String trackingNumber = parcel == null ? null : TrackingBrowserPageSupport.firstNonBlank(
        parcel.getTrackingNumber(),
        parcel.getNormalizedTrackingNumber()
    );
    return TrackingLinkResolver.fallbackTrackingUrl("colissimo", trackingNumber);
  }

  private Optional<TrackingSnapshot> fetchFromBrowserPage(Parcel parcel) {
    return BrowserTrackingScriptRunner.run(BROWSER_SCRIPT, browserFallbackTrackingUrl(parcel))
        .filter(payload -> !PublicTrackingPageClient.looksLikeBotChallenge(payload.html()))
        .map(payload -> toBrowserStructuredSnapshot(parcel, payload).orElseGet(() -> toBrowserSnapshot(parcel, payload)))
        .filter(snapshot -> snapshot.status() != ParcelStatus.UNKNOWN || snapshot.statusLabel() != null);
  }

  private Optional<TrackingSnapshot> toBrowserStructuredSnapshot(
      Parcel parcel,
      BrowserTrackingScriptRunner.BrowserPagePayload payload
  ) {
    return extractBrowserResponse(payload)
        .filter(this::isSuccess)
        .map(response -> {
          TrackingSnapshot snapshot = toSnapshot(parcel, response);
          Map<String, Object> rawPayload = new HashMap<>();
          if (snapshot.rawPayload() != null) {
            rawPayload.putAll(snapshot.rawPayload());
          }
          rawPayload.put("mode", "browser_tracking_page");
          putIfPresent(rawPayload, "page_title", payload.title());
          putIfPresent(rawPayload, "page_text_excerpt", excerpt(payload.text(), 4000));
          putIfPresent(rawPayload, "page_html_excerpt", excerpt(payload.html(), 8000));
          putIfPresent(rawPayload, "source", payload.source());

          return new TrackingSnapshot(
              BROWSER_PROVIDER,
              snapshot.providerTrackingId(),
              snapshot.carrierSlug(),
              snapshot.status(),
              snapshot.statusLabel(),
              snapshot.estimatedDeliveryAt(),
              snapshot.deliveredAt(),
              TrackingBrowserPageSupport.firstNonBlank(payload.currentUrl(), snapshot.trackingUrl()),
              snapshot.originAddress(),
              snapshot.destinationAddress(),
              snapshot.shipmentType(),
              snapshot.signedBy(),
              rawPayload,
              snapshot.events()
          );
        });
  }

  static TrackingSnapshot toBrowserSnapshot(Parcel parcel, BrowserTrackingScriptRunner.BrowserPagePayload payload) {
    List<TrackingEventSnapshot> events = TrackingBrowserPageSupport.extractEuropeanEvents(payload.text());
    String statusLabel = TrackingBrowserPageSupport.bestStatusLabel(payload.text(), payload.title());
    ParcelStatus status = TrackingBrowserPageSupport.resolveBestStatus(payload.html(), payload.text(), events);
    if (status == ParcelStatus.UNKNOWN && TrackingBrowserPageSupport.isTerminalCompletedLabel(statusLabel)) {
      status = ParcelStatus.DELIVERED;
    } else if (status == ParcelStatus.UNKNOWN && TrackingBrowserPageSupport.isNotFoundLabel(statusLabel)) {
      status = ParcelStatus.EXCEPTION;
    }
    OffsetDateTime deliveredAt = TrackingBrowserPageSupport.latestDeliveredAt(events);
    if (deliveredAt == null && status == ParcelStatus.DELIVERED) {
      deliveredAt = extractBrowserDeliveredAt(payload);
    }
    if (deliveredAt == null && status == ParcelStatus.DELIVERED) {
      deliveredAt = TrackingBrowserPageSupport.latestEventTime(events);
    }

    Map<String, Object> rawPayload = new HashMap<>();
    rawPayload.put("mode", "browser_tracking_page");
    rawPayload.put("tracking_url", TrackingBrowserPageSupport.firstNonBlank(payload.currentUrl(), browserFallbackTrackingUrl(parcel)));
    putIfPresent(rawPayload, "page_title", payload.title());
    putIfPresent(rawPayload, "page_text_excerpt", excerpt(payload.text(), 4000));
    putIfPresent(rawPayload, "page_html_excerpt", excerpt(payload.html(), 8000));
    putIfPresent(rawPayload, "source", payload.source());

    return new TrackingSnapshot(
        BROWSER_PROVIDER,
        TrackingBrowserPageSupport.firstNonBlank(parcel.getNormalizedTrackingNumber(), parcel.getTrackingNumber()),
        canonicalBrowserCarrier(parcel, payload),
        status,
        statusLabel,
        null,
        deliveredAt,
        TrackingBrowserPageSupport.firstNonBlank(payload.currentUrl(), browserFallbackTrackingUrl(parcel)),
        null,
        null,
        "Colissimo / La Poste",
        null,
        rawPayload,
        events
    );
  }

  private Optional<Map<String, Object>> extractBrowserResponse(BrowserTrackingScriptRunner.BrowserPagePayload payload) {
    if (payload == null || payload.html() == null || payload.html().isBlank()) {
      return Optional.empty();
    }

    Document document = Jsoup.parse(payload.html());
    Element responseElement = document.selectFirst("script[data-role=laposte-tracking-response]");
    if (responseElement == null) {
      return Optional.empty();
    }

    String rawJson = firstNonBlank(responseElement.data(), responseElement.html(), responseElement.text());
    if (rawJson == null || rawJson.isBlank()) {
      return Optional.empty();
    }

    try {
      List<?> list = objectMapper.readValue(rawJson, List.class);
      if (!list.isEmpty() && list.get(0) instanceof Map<?, ?> responseMap) {
        return Optional.of(objectMapper.convertValue(responseMap, MAP_TYPE));
      }
    } catch (Exception ignored) {
      // Fall through to direct object parsing.
    }

    try {
      Map<?, ?> map = objectMapper.readValue(rawJson, Map.class);
      return Optional.of(objectMapper.convertValue(map, MAP_TYPE));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mapValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return Map.of();
  }

  private Map<String, Object> safeForStorage(Map<String, Object> response) {
    return objectMapper.convertValue(response, MAP_TYPE);
  }

  private OffsetDateTime parseDateTime(String value) {
    if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value);
    } catch (Exception ignored) {
      try {
        return OffsetDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
      } catch (Exception ignoredAgain) {
        try {
          return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
        } catch (Exception ignoredThird) {
          try {
            return LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.UTC);
          } catch (Exception ignoredFourth) {
            return null;
          }
        }
      }
    }
  }

  private boolean containsAny(String value, String... needles) {
    for (String needle : needles) {
      if (value.contains(needle)) {
        return true;
      }
    }
    return false;
  }

  private String normalizedCarrier(Parcel parcel) {
    return parcel.getCarrierSlug() == null ? "" : parcel.getCarrierSlug().trim().toLowerCase(Locale.ROOT);
  }

  private String rawTrackingUrl(Parcel parcel) {
    return rawTrackingUrlStatic(parcel);
  }

  private static String rawTrackingUrlStatic(Parcel parcel) {
    if (parcel == null || parcel.getRawCurrentPayload() == null) {
      return null;
    }
    Object rawUrl = parcel.getRawCurrentPayload().get("tracking_url");
    return rawUrl == null ? null : String.valueOf(rawUrl);
  }

  private String normalizedTracking(Parcel parcel) {
    return parcel.getNormalizedTrackingNumber() == null
        ? ""
        : parcel.getNormalizedTrackingNumber().trim().toUpperCase(Locale.ROOT);
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
        return value;
      }
    }
    return null;
  }

  private String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return "https://api.laposte.fr/suivi/v2/idships";
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private static ParcelStatus normalizeStatusStatic(String value) {
    return CarrierStatusResolver.resolve(value);
  }

  private static String firstNonBlankStatic(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
        return value;
      }
    }
    return null;
  }

  private static int statusPriority(ParcelStatus status) {
    if (status == null) {
      return 0;
    }
    return switch (status) {
      case INCOMPLETE, UNKNOWN, PENDING -> 0;
      case REGISTERED -> 1;
      case IN_TRANSIT -> 2;
      case OUT_FOR_DELIVERY -> 3;
      case EXCEPTION -> 4;
      case DELIVERED -> 5;
    };
  }

  private static ResolvedStatus preferSummaryLabel(ResolvedStatus resolved, String primaryLabel, String secondaryLabel) {
    if (resolved == null || resolved.status() == null) {
      return resolved;
    }
    if (primaryLabel != null && normalizeStatusStatic(primaryLabel) == resolved.status()) {
      return new ResolvedStatus(resolved.status(), primaryLabel);
    }
    if (secondaryLabel != null && normalizeStatusStatic(secondaryLabel) == resolved.status()) {
      return new ResolvedStatus(resolved.status(), secondaryLabel);
    }
    return resolved;
  }

  record ResolvedStatus(ParcelStatus status, String label) {
  }

  private static String excerpt(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String compact = TrackingBrowserPageSupport.compact(value);
    return compact.length() <= maxLength ? compact : compact.substring(0, maxLength);
  }

  private static String canonicalBrowserCarrier(Parcel parcel, BrowserTrackingScriptRunner.BrowserPagePayload payload) {
    return "colissimo";
  }

  private static String extractBrowserProduct(BrowserTrackingScriptRunner.BrowserPagePayload payload) {
    String html = payload == null || payload.html() == null ? "" : payload.html().toLowerCase(Locale.ROOT);
    String text = payload == null || payload.text() == null ? "" : payload.text().toLowerCase(Locale.ROOT);
    if (html.contains("data-product=\"colissimo\"") || html.contains("\"product\":\"colissimo\"")) {
      return "colissimo";
    }
    if (text.contains("produit: colissimo")) {
      return "colissimo";
    }
    return "";
  }

  private static OffsetDateTime extractBrowserDeliveredAt(BrowserTrackingScriptRunner.BrowserPagePayload payload) {
    if (payload == null) {
      return null;
    }
    OffsetDateTime deliveredAt = extractPatternDate(payload.html(), DELIVERY_DATE_JSON_PATTERN);
    if (deliveredAt != null) {
      return deliveredAt;
    }
    deliveredAt = extractPatternDate(payload.text(), DELIVERY_LINE_PATTERN);
    if (deliveredAt != null) {
      return deliveredAt;
    }
    return extractPatternDate(payload.text(), ISO_TIMESTAMP_PATTERN);
  }

  private static OffsetDateTime extractPatternDate(String value, Pattern pattern) {
    if (value == null || value.isBlank() || pattern == null) {
      return null;
    }
    Matcher matcher = pattern.matcher(value);
    if (!matcher.find()) {
      return null;
    }
    String candidate = matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
    if (candidate == null || candidate.isBlank()) {
      return null;
    }
    return parseDateTimeStatic(candidate.trim());
  }

  private static OffsetDateTime parseDateTimeStatic(String value) {
    if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value);
    } catch (Exception ignored) {
      try {
        return OffsetDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
      } catch (Exception ignoredAgain) {
        try {
          return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
        } catch (Exception ignoredThird) {
          try {
            return LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.UTC);
          } catch (Exception ignoredFourth) {
            return null;
          }
        }
      }
    }
  }

  private static void putIfPresent(Map<String, Object> target, String key, String value) {
    if (value != null && !value.isBlank()) {
      target.put(key, value);
    }
  }
}
