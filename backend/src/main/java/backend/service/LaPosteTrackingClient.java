package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@Service
@Order(100)
public class LaPosteTrackingClient implements CarrierTrackingClient {

  private static final Logger log = LoggerFactory.getLogger(LaPosteTrackingClient.class);
  private static final String CHECKPOINT_PREFIX = "[TRACKING][CHECKPOINT]";
  private static final String PROVIDER = "LA_POSTE_OKAPI";
  private static final String BROWSER_PROVIDER = "LA_POSTE_BROWSER_PAGE";
  private static final String BROWSER_SCRIPT = "laposte-browser-scrape.mjs";
  private static final String BASE_URL = "https://www.laposte.fr";
  private static final String BROWSER_USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
          + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final Pattern DELIVERY_DATE_JSON_PATTERN = Pattern.compile("\"deliveryDate\"\\s*:\\s*\"([^\"]+)\"");
  private static final Pattern DELIVERY_LINE_PATTERN = Pattern.compile("(?im)^livraison\\s*:\\s*(.+)$");
  private static final Pattern ISO_TIMESTAMP_PATTERN = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:\\d{2})\\b");

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final Environment environment;

  @Autowired
  public LaPosteTrackingClient(
      ObjectMapper objectMapper,
      Environment environment
  ) {
    this(RestClient.builder().baseUrl(BASE_URL).build(), objectMapper, environment);
  }

  LaPosteTrackingClient(
      RestClient restClient,
      ObjectMapper objectMapper,
      Environment environment
  ) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.environment = environment;
  }

  @PostConstruct
  void validateProductionConfig() {
    for (String profile : environment.getActiveProfiles()) {
      if ("prod".equalsIgnoreCase(profile) && !BrowserTrackingScriptRunner.isAvailable(BROWSER_SCRIPT)) {
        String reason = BrowserTrackingScriptRunner.unavailableReason(BROWSER_SCRIPT);
        log.info(
            "La Poste browser fallback is unavailable in prod, direct HTTP tracking remains enabled: {}",
            reason == null ? "no browser runtime detected" : reason
        );
        return;
      }
    }
  }

  @Override
  public boolean supports(Parcel parcel) {
    String trackingNumber = normalizedTracking(parcel);
    if (trackingNumber.isBlank()) {
      return false;
    }

    String carrier = resolvedCarrier(parcel);
    if (TrackingCarrierRules.isSupportedCarrier(carrier)) {
      return TrackingCarrierRules.isValidForCarrier(trackingNumber, carrier);
    }

    String inferredCarrier = TrackingCarrierRules.inferSupportedCarrier(trackingNumber);
    return TrackingCarrierRules.isSupportedCarrier(inferredCarrier);
  }

  @Override
  public boolean isConfigured() {
    return true;
  }

  String unavailableReason() {
    return null;
  }

  @Override
  public Optional<TrackingSnapshot> fetchTracking(Parcel parcel) {
    log.info(
        "{} step=laposte.fetch.start parcelId={} carrier={} tracking={} profiles={} browserAvailable={} browserReason={}",
        CHECKPOINT_PREFIX,
        parcelId(parcel),
        resolvedCarrier(parcel),
        normalizedTracking(parcel),
        activeProfiles(),
        BrowserTrackingScriptRunner.isAvailable(BROWSER_SCRIPT),
        BrowserTrackingScriptRunner.unavailableReason(BROWSER_SCRIPT)
    );
    FetchAttempt directAttempt = fetchFromUnifiedEndpoint(parcel);
    if (directAttempt.snapshot().isPresent()) {
      TrackingSnapshot snapshot = directAttempt.snapshot().get();
      log.info(
          "{} step=laposte.fetch.direct_success parcelId={} carrier={} tracking={} provider={} status={} label={} events={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          normalizedTracking(parcel),
          snapshot.provider(),
          snapshot.status(),
          snapshot.statusLabel(),
          snapshot.events() == null ? 0 : snapshot.events().size()
      );
      return directAttempt.snapshot();
    }
    log.warn(
        "{} step=laposte.fetch.direct_empty parcelId={} carrier={} tracking={} failureCode={} failureDetail={}",
        CHECKPOINT_PREFIX,
        parcelId(parcel),
        resolvedCarrier(parcel),
        normalizedTracking(parcel),
        directAttempt.failureCode(),
        excerpt(directAttempt.failureDetail(), 240)
    );

    FetchAttempt browserAttempt = fetchFromBrowserPage(parcel);
    if (browserAttempt.snapshot().isPresent()) {
      TrackingSnapshot snapshot = browserAttempt.snapshot().get();
      log.info(
          "{} step=laposte.fetch.browser_success parcelId={} carrier={} tracking={} provider={} status={} label={} events={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          normalizedTracking(parcel),
          snapshot.provider(),
          snapshot.status(),
          snapshot.statusLabel(),
          snapshot.events() == null ? 0 : snapshot.events().size()
      );
      return browserAttempt.snapshot();
    }
    log.warn(
        "{} step=laposte.fetch.browser_empty parcelId={} carrier={} tracking={} failureCode={} failureDetail={}",
        CHECKPOINT_PREFIX,
        parcelId(parcel),
        resolvedCarrier(parcel),
        normalizedTracking(parcel),
        browserAttempt.failureCode(),
        excerpt(browserAttempt.failureDetail(), 240)
    );

    if ("remote_access_denied".equals(directAttempt.failureCode())) {
      log.warn(
          "{} step=laposte.fetch.infrastructure_fallback parcelId={} carrier={} tracking={} directFailure={} browserFailure={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          normalizedTracking(parcel),
          directAttempt.failureCode(),
          browserAttempt.failureCode()
      );
      return Optional.of(buildInfrastructureFallbackSnapshot(
          parcel,
          "La Poste bloque actuellement les requetes serveur en production. Consulte le lien transporteur pour voir le suivi detaille.",
          directAttempt,
          browserAttempt
      ));
    }

    log.warn(
        "{} step=laposte.fetch.no_snapshot parcelId={} carrier={} tracking={} directFailure={} browserFailure={}",
        CHECKPOINT_PREFIX,
        parcelId(parcel),
        resolvedCarrier(parcel),
        normalizedTracking(parcel),
        directAttempt.failureCode(),
        browserAttempt.failureCode()
    );
    return Optional.empty();
  }

  private FetchAttempt fetchFromUnifiedEndpoint(Parcel parcel) {
    String trackingNumber = normalizedTracking(parcel);
    if (trackingNumber.isBlank()) {
      log.warn(
          "{} step=laposte.http.skip_missing_tracking parcelId={} carrier={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel)
      );
      return FetchAttempt.empty("missing_tracking_number", "numero de suivi vide");
    }

    try {
      String trackingUrl = fallbackTrackingUrl(parcel);
      log.info(
          "{} step=laposte.http.request_start parcelId={} carrier={} tracking={} url={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          trackingNumber,
          trackingUrl
      );
      String body = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/ssu/sun/back/suivi-unifie/{trackingNumber}")
              .queryParam("lang", "fr")
              .build(trackingNumber))
          .header(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
          .header(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
          .header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
          .header(HttpHeaders.CACHE_CONTROL, "no-cache")
          .header("Pragma", "no-cache")
          .header("Origin", BASE_URL)
          .header(HttpHeaders.REFERER, trackingUrl)
          .retrieve()
          .body(String.class);
      log.info(
          "{} step=laposte.http.response_received parcelId={} carrier={} tracking={} bodyLength={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          trackingNumber,
          body == null ? 0 : body.length()
      );

      Optional<Map<String, Object>> parsedResponse = parseTrackingResponse(body);
      if (parsedResponse.isEmpty()) {
        log.warn(
            "{} step=laposte.http.parse_failed parcelId={} carrier={} tracking={} bodyExcerpt={}",
            CHECKPOINT_PREFIX,
            parcelId(parcel),
            resolvedCarrier(parcel),
            trackingNumber,
            excerpt(body, 320)
        );
        log.warn(
            "La Poste unified tracking endpoint returned an unreadable payload for parcel {} ({}): {}",
            parcel == null ? null : parcel.getId(),
            parcel == null ? null : parcel.getTrackingNumber(),
            excerpt(body, 320)
        );
        return FetchAttempt.empty("unreadable_payload", "payload HTTP La Poste illisible");
      }

      Map<String, Object> response = parsedResponse.get();
      if (!isSuccess(response)) {
        log.warn(
            "{} step=laposte.http.non_success parcelId={} carrier={} tracking={} returnCode={} bodyExcerpt={}",
            CHECKPOINT_PREFIX,
            parcelId(parcel),
            resolvedCarrier(parcel),
            trackingNumber,
            response.get("returnCode"),
            excerpt(body, 320)
        );
        log.warn(
            "La Poste unified tracking endpoint returned a non-success payload for parcel {} ({}): {}",
            parcel == null ? null : parcel.getId(),
            parcel == null ? null : parcel.getTrackingNumber(),
            excerpt(body, 320)
        );
        return FetchAttempt.empty("non_success_payload", "payload HTTP La Poste non exploitable");
      }

      TrackingSnapshot snapshot = toSnapshot(parcel, response);
      log.info(
          "{} step=laposte.http.snapshot_built parcelId={} carrier={} tracking={} provider={} status={} label={} events={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          snapshot.carrierSlug(),
          trackingNumber,
          snapshot.provider(),
          snapshot.status(),
          snapshot.statusLabel(),
          snapshot.events() == null ? 0 : snapshot.events().size()
      );
      return FetchAttempt.success(snapshot);
    } catch (HttpClientErrorException.Forbidden ex) {
      String body = ex.getResponseBodyAsString();
      String failureCode = looksLikeRemoteAccessDenied(body) ? "remote_access_denied" : "http_forbidden";
      log.warn(
          "{} step=laposte.http.forbidden parcelId={} carrier={} tracking={} failureCode={} bodyExcerpt={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          trackingNumber,
          failureCode,
          excerpt(body, 240)
      );
      log.warn(
          "La Poste unified tracking endpoint failed for parcel {} ({})",
          parcel == null ? null : parcel.getId(),
          parcel == null ? null : parcel.getTrackingNumber(),
          ex
      );
      return FetchAttempt.empty(failureCode, excerpt(body, 240));
    } catch (Exception ex) {
      log.warn(
          "{} step=laposte.http.exception parcelId={} carrier={} tracking={} errorType={} message={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          trackingNumber,
          ex.getClass().getSimpleName(),
          excerpt(ex.getMessage(), 240)
      );
      log.warn(
          "La Poste unified tracking endpoint failed for parcel {} ({})",
          parcel == null ? null : parcel.getId(),
          parcel == null ? null : parcel.getTrackingNumber(),
          ex
      );
      return FetchAttempt.empty("http_failure", ex.getClass().getSimpleName());
    }
  }

  private TrackingSnapshot toSnapshot(Parcel parcel, Map<String, Object> response) {
    Map<String, Object> shipment = mapValue(response.get("shipment"));
    List<TrackingEventSnapshot> events = new ArrayList<>();
    List<String> timelineLabels = new ArrayList<>();
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
    Object rawTimeline = shipment.get("timeline");
    if (rawTimeline instanceof List<?> list) {
      for (Object item : list) {
        Map<String, Object> rawStep = mapValue(item);
        if (rawStep.isEmpty()) {
          continue;
        }
        String label = firstNonBlank(
            stringValue(rawStep.get("shortLabel")),
            stringValue(rawStep.get("longLabel")),
            stringValue(rawStep.get("label"))
        );
        if (label != null) {
          timelineLabels.add(label);
        }
      }
    }

    TrackingEventSnapshot latest = latestEvent(events);
    OffsetDateTime deliveredAt = parseDateTime(stringValue(shipment.get("deliveryDate")));
    String shortLabel = stringValue(shipment.get("shortLabel"));
    String longLabel = stringValue(shipment.get("longLabel"));
    ResolvedStatus resolvedStatus = resolveStatus(
        events,
        shortLabel,
        longLabel,
        timelineLabels,
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
        parseDateTime(firstNonBlank(
            stringValue(shipment.get("estimatedDeliveryDate")),
            stringValue(shipment.get("estimDate"))
        )),
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
      List<String> timelineLabels,
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
    if (timelineLabels != null) {
      for (String timelineLabel : timelineLabels) {
        if (timelineLabel != null && !timelineLabel.isBlank()) {
          candidates.add(new ResolvedStatus(normalizeStatusStatic(timelineLabel), timelineLabel));
        }
      }
    }

    ResolvedStatus resolved = candidates.stream()
        .filter(candidate -> candidate.status() != null && candidate.status() != ParcelStatus.UNKNOWN)
        .max(Comparator.comparingInt(candidate -> statusPriority(candidate.status())))
        .orElseGet(() -> {
          TrackingEventSnapshot latest = latestEvent(events == null ? List.of() : events);
          String fallbackLabel = firstNonBlankStatic(
              latest == null ? null : latest.description(),
              shortLabel,
              longLabel,
              latestNonBlank(timelineLabels)
          );
          return new ResolvedStatus(normalizeStatusStatic(fallbackLabel), fallbackLabel);
        });
    return preferSummaryLabel(resolved, shortLabel, longLabel, latestNonBlank(timelineLabels));
  }

  private String canonicalCarrier(String existingCarrier, String product) {
    return canonicalCarrierStatic(existingCarrier, product);
  }

  private String fallbackTrackingUrl(Parcel parcel) {
    return fallbackTrackingUrl(parcel, null);
  }

  private String fallbackTrackingUrl(Parcel parcel, String product) {
    String carrier = canonicalCarrier(parcel == null ? null : parcel.getCarrierSlug(), product);
    String trustedUrl = rawTrackingUrl(parcel);
    if (TrackingLinkResolver.isTrustedTrackingUrl(trustedUrl, carrier)) {
      return trustedUrl.trim();
    }
    return TrackingLinkResolver.fallbackTrackingUrl(
        carrier,
        parcel == null ? null : TrackingBrowserPageSupport.firstNonBlank(
            parcel.getTrackingNumber(),
            parcel.getNormalizedTrackingNumber()
        )
    );
  }

  private static String browserFallbackTrackingUrl(Parcel parcel) {
    String carrier = browserResolvedCarrier(parcel);
    String trustedUrl = rawTrackingUrlStatic(parcel);
    if (TrackingLinkResolver.isTrustedTrackingUrl(trustedUrl, carrier)) {
      return trustedUrl.trim();
    }
    String trackingNumber = parcel == null ? null : TrackingBrowserPageSupport.firstNonBlank(
        parcel.getTrackingNumber(),
        parcel.getNormalizedTrackingNumber()
    );
    return TrackingLinkResolver.fallbackTrackingUrl(carrier, trackingNumber);
  }

  private FetchAttempt fetchFromBrowserPage(Parcel parcel) {
    if (!BrowserTrackingScriptRunner.isAvailable(BROWSER_SCRIPT)) {
      log.warn(
          "{} step=laposte.browser.unavailable parcelId={} carrier={} tracking={} reason={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          normalizedTracking(parcel),
          BrowserTrackingScriptRunner.unavailableReason(BROWSER_SCRIPT)
      );
      return FetchAttempt.empty(
          "browser_unavailable",
          BrowserTrackingScriptRunner.unavailableReason(BROWSER_SCRIPT)
      );
    }

    String browserUrl = browserFallbackTrackingUrl(parcel);
    log.info(
        "{} step=laposte.browser.request_start parcelId={} carrier={} tracking={} url={}",
        CHECKPOINT_PREFIX,
        parcelId(parcel),
        resolvedCarrier(parcel),
        normalizedTracking(parcel),
        browserUrl
    );
    Optional<BrowserTrackingScriptRunner.BrowserPagePayload> payloadResult =
        BrowserTrackingScriptRunner.run(BROWSER_SCRIPT, browserUrl);
    if (payloadResult.isEmpty()) {
      log.warn(
          "{} step=laposte.browser.empty_payload parcelId={} carrier={} tracking={} url={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          normalizedTracking(parcel),
          browserUrl
      );
      return FetchAttempt.empty("browser_failed", "script navigateur vide ou en echec");
    }

    BrowserTrackingScriptRunner.BrowserPagePayload payload = payloadResult.get();
    log.info(
        "{} step=laposte.browser.payload_received parcelId={} carrier={} tracking={} currentUrl={} source={} title={} htmlLength={} textLength={}",
        CHECKPOINT_PREFIX,
        parcelId(parcel),
        resolvedCarrier(parcel),
        normalizedTracking(parcel),
        excerpt(payload.currentUrl(), 240),
        payload.source(),
        excerpt(payload.title(), 180),
        payload.html() == null ? 0 : payload.html().length(),
        payload.text() == null ? 0 : payload.text().length()
    );
    if (PublicTrackingPageClient.looksLikeBotChallenge(payload.html())) {
      log.warn(
          "{} step=laposte.browser.bot_challenge parcelId={} carrier={} tracking={} currentUrl={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          resolvedCarrier(parcel),
          normalizedTracking(parcel),
          excerpt(payload.currentUrl(), 240)
      );
      log.warn(
          "La Poste browser tracking hit a bot challenge for parcel {} ({}) at {}",
          parcel == null ? null : parcel.getId(),
          parcel == null ? null : parcel.getTrackingNumber(),
          payload.currentUrl()
      );
      return FetchAttempt.empty("browser_bot_challenge", excerpt(payload.currentUrl(), 240));
    }

    Optional<TrackingSnapshot> structuredSnapshot = toBrowserStructuredSnapshot(parcel, payload);
    if (structuredSnapshot.isPresent()) {
      TrackingSnapshot snapshot = structuredSnapshot.get();
      log.info(
          "{} step=laposte.browser.structured_snapshot parcelId={} carrier={} tracking={} provider={} status={} label={} events={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          snapshot.carrierSlug(),
          normalizedTracking(parcel),
          snapshot.provider(),
          snapshot.status(),
          snapshot.statusLabel(),
          snapshot.events() == null ? 0 : snapshot.events().size()
      );
      return FetchAttempt.success(snapshot);
    }

    TrackingSnapshot fallbackSnapshot = toBrowserSnapshot(parcel, payload);
    if (fallbackSnapshot.status() != ParcelStatus.UNKNOWN || fallbackSnapshot.statusLabel() != null) {
      log.info(
          "{} step=laposte.browser.fallback_snapshot parcelId={} carrier={} tracking={} provider={} status={} label={} events={}",
          CHECKPOINT_PREFIX,
          parcelId(parcel),
          fallbackSnapshot.carrierSlug(),
          normalizedTracking(parcel),
          fallbackSnapshot.provider(),
          fallbackSnapshot.status(),
          fallbackSnapshot.statusLabel(),
          fallbackSnapshot.events() == null ? 0 : fallbackSnapshot.events().size()
      );
      return FetchAttempt.success(fallbackSnapshot);
    }

    log.warn(
        "{} step=laposte.browser.no_status parcelId={} carrier={} tracking={} title={} currentUrl={} textExcerpt={}",
        CHECKPOINT_PREFIX,
        parcelId(parcel),
        resolvedCarrier(parcel),
        normalizedTracking(parcel),
        excerpt(payload.title(), 160),
        excerpt(payload.currentUrl(), 240),
        excerpt(payload.text(), 320)
    );
    log.warn(
        "La Poste browser fallback returned no usable status for parcel {} ({}), title='{}', url='{}', text='{}'",
        parcel == null ? null : parcel.getId(),
        parcel == null ? null : parcel.getTrackingNumber(),
        excerpt(payload.title(), 160),
        excerpt(payload.currentUrl(), 300),
        excerpt(payload.text(), 320)
    );
    return FetchAttempt.empty("browser_no_status", excerpt(payload.currentUrl(), 240));
  }

  private TrackingSnapshot buildInfrastructureFallbackSnapshot(
      Parcel parcel,
      String label,
      FetchAttempt directAttempt,
      FetchAttempt browserAttempt
  ) {
    Map<String, Object> rawPayload = new HashMap<>();
    rawPayload.put("mode", "carrier_unavailable_fallback");
    rawPayload.put("tracking_health_code", "laposte_prod_access_denied");
    rawPayload.put("tracking_health_message", label);
    putIfPresent(rawPayload, "tracking_url", fallbackTrackingUrl(parcel));
    putIfPresent(rawPayload, "transport_failure_code", directAttempt.failureCode());
    putIfPresent(rawPayload, "transport_failure_detail", directAttempt.failureDetail());
    putIfPresent(rawPayload, "browser_failure_code", browserAttempt.failureCode());
    putIfPresent(rawPayload, "browser_failure_detail", browserAttempt.failureDetail());

    return new TrackingSnapshot(
        firstNonBlank(parcel == null ? null : parcel.getAggregator(), DirectCarrierTrackingService.PROVIDER),
        firstNonBlank(parcel == null ? null : parcel.getAggregatorTrackingId(), normalizedTracking(parcel)),
        resolvedCarrier(parcel),
        parcel == null || parcel.getStatus() == null ? ParcelStatus.REGISTERED : parcel.getStatus(),
        label,
        parcel == null ? null : parcel.getEstimatedDeliveryAt(),
        parcel == null ? null : parcel.getDeliveredAt(),
        fallbackTrackingUrl(parcel),
        null,
        null,
        "Colissimo / La Poste",
        null,
        rawPayload,
        List.of()
    );
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
        browserShipmentType(parcel, payload),
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
    return parseTrackingResponse(rawJson);
  }

  private Optional<Map<String, Object>> parseTrackingResponse(String rawJson) {
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
    return parcel == null ? "" : resolvedCarrier(parcel);
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

  private static ResolvedStatus preferSummaryLabel(
      ResolvedStatus resolved,
      String primaryLabel,
      String secondaryLabel,
      String timelineLabel
  ) {
    if (resolved == null || resolved.status() == null) {
      return resolved;
    }
    if (primaryLabel != null && normalizeStatusStatic(primaryLabel) == resolved.status()) {
      return new ResolvedStatus(resolved.status(), primaryLabel);
    }
    if (secondaryLabel != null && normalizeStatusStatic(secondaryLabel) == resolved.status()) {
      return new ResolvedStatus(resolved.status(), secondaryLabel);
    }
    if (timelineLabel != null && normalizeStatusStatic(timelineLabel) == resolved.status()) {
      return new ResolvedStatus(resolved.status(), timelineLabel);
    }
    return resolved;
  }

  private static String latestNonBlank(List<String> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    for (int index = values.size() - 1; index >= 0; index--) {
      String value = values.get(index);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
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
    return canonicalCarrierStatic(browserResolvedCarrier(parcel), extractBrowserProduct(payload));
  }

  private static String extractBrowserProduct(BrowserTrackingScriptRunner.BrowserPagePayload payload) {
    String html = payload == null || payload.html() == null ? "" : payload.html().toLowerCase(Locale.ROOT);
    String text = payload == null || payload.text() == null ? "" : payload.text().toLowerCase(Locale.ROOT);
    if (html.contains("data-product=\"chronopost\"") || html.contains("\"product\":\"chronopost\"")) {
      return "chronopost";
    }
    if (text.contains("produit: chronopost")) {
      return "chronopost";
    }
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

  private String resolvedCarrier(Parcel parcel) {
    String normalized = TrackingCarrierRules.normalizeCarrierSlug(parcel == null ? null : parcel.getCarrierSlug());
    if (TrackingCarrierRules.isSupportedCarrier(normalized)) {
      return normalized;
    }
    return browserResolvedCarrier(parcel);
  }

  private static String browserResolvedCarrier(Parcel parcel) {
    String normalized = TrackingCarrierRules.normalizeCarrierSlug(parcel == null ? null : parcel.getCarrierSlug());
    if (TrackingCarrierRules.isSupportedCarrier(normalized)) {
      return normalized;
    }

    String detectedFromUrl = TrackingLinkResolver.detectCarrierSlug(rawTrackingUrlStatic(parcel));
    if (TrackingCarrierRules.isSupportedCarrier(detectedFromUrl)) {
      return detectedFromUrl;
    }

    String trackingNumber = parcel == null ? null : TrackingBrowserPageSupport.firstNonBlank(
        parcel.getNormalizedTrackingNumber(),
        parcel.getTrackingNumber()
    );
    String inferred = TrackingCarrierRules.inferSupportedCarrier(trackingNumber);
    return TrackingCarrierRules.isSupportedCarrier(inferred) ? inferred : "colissimo";
  }

  private static String canonicalCarrierStatic(String existingCarrier, String product) {
    String normalizedProduct = TrackingCarrierRules.normalizeCarrierSlug(product);
    if (TrackingCarrierRules.isSupportedCarrier(normalizedProduct)) {
      return normalizedProduct;
    }

    String normalizedExisting = TrackingCarrierRules.normalizeCarrierSlug(existingCarrier);
    if (TrackingCarrierRules.isSupportedCarrier(normalizedExisting)) {
      return normalizedExisting;
    }

    String productText = product == null ? "" : product.toLowerCase(Locale.ROOT);
    if (productText.contains("chronopost")) {
      return "chronopost";
    }
    return "colissimo";
  }

  private static String browserShipmentType(Parcel parcel, BrowserTrackingScriptRunner.BrowserPagePayload payload) {
    String carrier = canonicalBrowserCarrier(parcel, payload);
    return "chronopost".equals(carrier) ? "Chronopost" : "Colissimo / La Poste";
  }

  private static boolean looksLikeRemoteAccessDenied(String value) {
    String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
    return normalized.contains("access denied")
        || normalized.contains("you don't have permission")
        || normalized.contains("you dont have permission")
        || normalized.contains("errors.edgesuite.net");
  }

  private String activeProfiles() {
    String[] profiles = environment.getActiveProfiles();
    return profiles.length == 0 ? "default" : String.join(",", profiles);
  }

  private Long parcelId(Parcel parcel) {
    return parcel == null ? null : parcel.getId();
  }

  private record FetchAttempt(
      Optional<TrackingSnapshot> snapshot,
      String failureCode,
      String failureDetail
  ) {
    private static FetchAttempt success(TrackingSnapshot snapshot) {
      return new FetchAttempt(Optional.ofNullable(snapshot), null, null);
    }

    private static FetchAttempt empty(String failureCode, String failureDetail) {
      return new FetchAttempt(Optional.empty(), failureCode, failureDetail);
    }
  }
}
