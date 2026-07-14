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
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Order(100)
public class LaPosteTrackingClient implements CarrierTrackingClient {

  private static final String PROVIDER = "LA_POSTE_OKAPI";
  private static final Pattern LA_POSTE_LIKE = Pattern.compile(
      "([A-Z]{2}\\d{9}[A-Z]{2}|\\d[A-Z]\\d{11}|\\d{13}|[A-Z0-9]{13,16})",
      Pattern.CASE_INSENSITIVE
  );
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final String apiKey;
  private final String baseUrl;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public LaPosteTrackingClient(
      @Value("${app.delivery.laposte-api-key:}") String apiKey,
      @Value("${app.delivery.laposte-tracking-base-url:https://api.laposte.fr/suivi/v2/idships}") String baseUrl,
      ObjectMapper objectMapper
  ) {
    this.apiKey = apiKey;
    this.baseUrl = trimTrailingSlash(baseUrl);
    this.objectMapper = objectMapper;
    this.restClient = RestClient.builder().build();
  }

  @Override
  public boolean supports(Parcel parcel) {
    String carrier = normalizedCarrier(parcel);
    if (carrier.equals("colissimo") || carrier.equals("laposte") || carrier.equals("la-poste")
        || carrier.equals("chronopost")) {
      return true;
    }
    String tracking = normalizedTracking(parcel);
    return LA_POSTE_LIKE.matcher(tracking).matches();
  }

  @Override
  public boolean isConfigured() {
    return apiKey != null && !apiKey.isBlank();
  }

  @Override
  public Optional<TrackingSnapshot> fetchTracking(Parcel parcel) {
    if (!isConfigured()) {
      return Optional.empty();
    }

    Map<String, Object> response = restClient.get()
        .uri(trackingUri(parcel))
        .header("Accept", "application/json")
        .header("X-Okapi-Key", apiKey)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});

    if (response == null || !isSuccess(response)) {
      return Optional.empty();
    }

    return Optional.of(toSnapshot(parcel, response));
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
    String carrier = existingCarrier == null ? "" : existingCarrier.toLowerCase(Locale.ROOT);
    String rawProduct = product == null ? "" : product.toLowerCase(Locale.ROOT);
    if (carrier.contains("chronopost") || rawProduct.contains("chrono")) {
      return "chronopost";
    }
    if (carrier.contains("poste") || carrier.contains("colissimo") || rawProduct.contains("colissimo")) {
      return carrier.isBlank() || carrier.equals("unknown") ? "colissimo" : carrier;
    }
    return carrier.isBlank() || carrier.equals("unknown") ? "colissimo" : carrier;
  }

  private String fallbackTrackingUrl(Parcel parcel) {
    return fallbackTrackingUrl(parcel, null);
  }

  private String fallbackTrackingUrl(Parcel parcel, String product) {
    String carrier = canonicalCarrier(parcel == null ? null : parcel.getCarrierSlug(), product);
    String baseUrl = "chronopost".equals(carrier)
        ? "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT="
        : "https://www.laposte.fr/outils/suivre-vos-envois?code=";
    return baseUrl + URLEncoder.encode(parcel.getTrackingNumber().trim(), StandardCharsets.UTF_8);
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
      case UNKNOWN, PENDING -> 0;
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
}
