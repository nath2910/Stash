package backend.service;

import backend.dto.TrackingWebhookResponse;
import backend.entity.Parcel;
import backend.entity.ParcelEvent;
import backend.entity.ParcelStatus;
import backend.entity.TrackingWebhookEvent;
import backend.repository.ParcelEventRepository;
import backend.repository.ParcelRepository;
import backend.repository.TrackingWebhookEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AfterShipTrackingClient implements TrackingAggregatorService {

  private static final Logger log = LoggerFactory.getLogger(AfterShipTrackingClient.class);
  private static final String AGGREGATOR = "AFTERSHIP";
  private static final String AFTERSHIP_TRACKINGS_URL =
      "https://api.aftership.com/tracking/2026-01/trackings";
  private static final URI AFTERSHIP_TRACKINGS_URI = URI.create(AFTERSHIP_TRACKINGS_URL);
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final String apiKey;
  private final String webhookSecret;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final ParcelRepository parcelRepository;
  private final ParcelEventRepository parcelEventRepository;
  private final TrackingWebhookEventRepository trackingWebhookEventRepository;

  public AfterShipTrackingClient(
      @Value("${app.delivery.aftership-api-key:}") String apiKey,
      @Value("${app.delivery.aftership-webhook-secret:}") String webhookSecret,
      ObjectMapper objectMapper,
      ParcelRepository parcelRepository,
      ParcelEventRepository parcelEventRepository,
      TrackingWebhookEventRepository trackingWebhookEventRepository
  ) {
    this.apiKey = apiKey;
    this.webhookSecret = webhookSecret;
    this.restClient = RestClient.builder().build();
    this.objectMapper = objectMapper;
    this.parcelRepository = parcelRepository;
    this.parcelEventRepository = parcelEventRepository;
    this.trackingWebhookEventRepository = trackingWebhookEventRepository;
  }

  @Override
  @Transactional
  public void registerTracking(Parcel parcel) {
    registerTrackingInternal(parcel, false);
  }

  public boolean isConfigured() {
    return apiKey != null && !apiKey.isBlank();
  }

  @Override
  @Transactional
  public Parcel refreshTracking(Parcel parcel) {
    if (parcel == null || parcel.getId() == null) {
      return parcel;
    }

    if (apiKey == null || apiKey.isBlank()) {
      markRegisteredLocally(parcel);
      log.info("AfterShip API key absent; parcel {} cannot be refreshed remotely", parcel.getId());
      return parcelRepository.save(parcel);
    }

    if (parcel.getAggregatorTrackingId() == null || parcel.getAggregatorTrackingId().isBlank()) {
      return registerTrackingInternal(parcel, true);
    }

    try {
      Map<String, Object> response = restClient.get()
          .uri(URI.create(AFTERSHIP_TRACKINGS_URL + "/" + parcel.getAggregatorTrackingId()))
          .header("as-api-key", apiKey)
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});

      applyTrackingSnapshot(parcel, extractTrackingMap(response));
      return parcelRepository.save(parcel);
    } catch (Exception ex) {
      log.warn("AfterShip refresh failed for parcel {}", parcel.getId());
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Suivi AfterShip inaccessible", ex);
    }
  }

  private Parcel registerTrackingInternal(Parcel parcel, boolean strict) {
    if (parcel == null || parcel.getId() == null) {
      return parcel;
    }

    if (apiKey == null || apiKey.isBlank()) {
      markRegisteredLocally(parcel);
      parcelRepository.save(parcel);
      log.info("AfterShip API key absent; parcel {} kept locally without aggregator registration", parcel.getId());
      return parcel;
    }

    try {
      Map<String, Object> response = createTracking(parcel, true);
      applyTrackingSnapshot(parcel, extractTrackingMap(response));
      return parcelRepository.save(parcel);
    } catch (Exception firstFailure) {
      if (hasUsableCarrierSlug(parcel)) {
        try {
          Map<String, Object> response = createTracking(parcel, false);
          applyTrackingSnapshot(parcel, extractTrackingMap(response));
          return parcelRepository.save(parcel);
        } catch (Exception retryFailure) {
          return handleRegistrationFailure(parcel, retryFailure, strict);
        }
      }
      return handleRegistrationFailure(parcel, firstFailure, strict);
    }
  }

  private Parcel handleRegistrationFailure(Parcel parcel, Exception ex, boolean strict) {
    log.warn("AfterShip registration failed for parcel {}", parcel.getId());
    if (strict) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Enregistrement AfterShip impossible", ex);
    }
    markRegisteredLocally(parcel);
    return parcelRepository.save(parcel);
  }

  private Map<String, Object> createTracking(Parcel parcel, boolean includeCarrierSlug) {
    Map<String, Object> tracking = new HashMap<>();
    tracking.put("tracking_number", parcel.getTrackingNumber());

    if (includeCarrierSlug && hasUsableCarrierSlug(parcel)) {
      tracking.put("slug", afterShipSlug(parcel.getCarrierSlug()));
    }

    return restClient.post()
        .uri(AFTERSHIP_TRACKINGS_URI)
        .header("as-api-key", apiKey)
        .body(Map.of("tracking", tracking))
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  @Override
  @Transactional
  public TrackingWebhookResponse handleWebhook(String payload, String signatureHeader, String sharedSecretHeader) {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook AfterShip non configure");
    }

    Map<String, Object> payloadMap = parsePayload(payload);
    String externalEventId = firstNonBlank(
        stringValue(payloadMap.get("id")),
        stringValue(payloadMap.get("event_id")),
        stringValue(payloadMap.get("msg_id")),
        stringValue(payloadMap.get("webhook_id"))
    );

    boolean signatureValid = isValidSignature(payload, signatureHeader, sharedSecretHeader);
    TrackingWebhookEvent webhookEvent = saveWebhookEvent(payloadMap, externalEventId, signatureValid);
    if (!signatureValid) {
      log.warn("AfterShip webhook rejected: invalid signature");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signature webhook invalide");
    }

    if (externalEventId != null) {
      Optional<TrackingWebhookEvent> existing =
          trackingWebhookEventRepository.findByAggregatorAndExternalEventId(AGGREGATOR, externalEventId);
      if (existing.isPresent() && existing.get().getId() != null && !existing.get().getId().equals(webhookEvent.getId())
          && existing.get().getProcessedAt() != null) {
        return new TrackingWebhookResponse(true, false, "Webhook deja traite");
      }
    }

    Map<String, Object> tracking = extractTrackingMap(payloadMap);
    Optional<Parcel> parcelOpt = findParcelForWebhook(tracking);
    if (parcelOpt.isEmpty()) {
      webhookEvent.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
      trackingWebhookEventRepository.save(webhookEvent);
      return new TrackingWebhookResponse(true, false, "Aucun colis correspondant");
    }

    Parcel parcel = parcelOpt.get();
    applyTrackingSnapshot(parcel, tracking);
    parcelRepository.save(parcel);

    webhookEvent.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
    trackingWebhookEventRepository.save(webhookEvent);
    return new TrackingWebhookResponse(true, true, "Webhook traite");
  }

  private void applyTrackingSnapshot(Parcel parcel, Map<String, Object> tracking) {
    if (parcel == null || tracking == null || tracking.isEmpty()) {
      return;
    }

    parcel.setAggregator(AGGREGATOR);

    String trackingId = stringValue(tracking.get("id"));
    if (trackingId != null) {
      parcel.setAggregatorTrackingId(trackingId);
    }

    String carrierSlug = stringValue(tracking.get("slug"));
    if (carrierSlug != null && !"unrecognized".equalsIgnoreCase(carrierSlug)) {
      parcel.setCarrierSlug(carrierSlug);
    }

    String rawStatus = firstNonBlank(
        stringValue(tracking.get("tag")),
        stringValue(tracking.get("status")),
        stringValue(tracking.get("subtag"))
    );
    if (rawStatus != null) {
      parcel.setStatus(normalizeStatus(rawStatus));
    } else {
      markRegisteredLocally(parcel);
    }

    String statusLabel = firstNonBlank(
        stringValue(tracking.get("subtag_message")),
        stringValue(tracking.get("status_label")),
        stringValue(tracking.get("message"))
    );
    if (statusLabel != null) {
      parcel.setStatusLabel(statusLabel);
    }

    OffsetDateTime estimatedDeliveryAt = estimatedDeliveryAt(tracking);
    if (estimatedDeliveryAt != null) {
      parcel.setEstimatedDeliveryAt(estimatedDeliveryAt);
    }

    OffsetDateTime deliveredAt = parseNullableDateTime(firstNonBlank(
        stringValue(tracking.get("shipment_delivery_date")),
        stringValue(tracking.get("delivered_at"))
    ));
    if (deliveredAt != null) {
      parcel.setDeliveredAt(deliveredAt);
    }

    parcel.setRawCurrentPayload(new HashMap<>(tracking));

    OffsetDateTime lastEventAt = syncCheckpointEvents(parcel, tracking);
    if (lastEventAt != null) {
      parcel.setLastEventAt(lastEventAt);
    } else {
      OffsetDateTime updatedAt = parseNullableDateTime(stringValue(tracking.get("updated_at")));
      if (updatedAt != null) {
        parcel.setLastEventAt(updatedAt);
      }
    }

    if (parcel.getStatus() == ParcelStatus.DELIVERED && parcel.getDeliveredAt() == null) {
      parcel.setDeliveredAt(parcel.getLastEventAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : parcel.getLastEventAt());
    }
  }

  private OffsetDateTime syncCheckpointEvents(Parcel parcel, Map<String, Object> tracking) {
    Object checkpoints = tracking.get("checkpoints");
    if (!(checkpoints instanceof List<?> list) || list.isEmpty()) {
      return null;
    }

    OffsetDateTime latest = null;
    for (Object item : list) {
      if (!(item instanceof Map<?, ?> rawCheckpoint)) {
        continue;
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> checkpoint = (Map<String, Object>) rawCheckpoint;
      OffsetDateTime eventTime = parseDateTime(firstNonBlank(
          stringValue(checkpoint.get("checkpoint_time")),
          stringValue(checkpoint.get("event_time")),
          stringValue(checkpoint.get("created_at")),
          stringValue(tracking.get("updated_at"))
      ));

      ParcelStatus status = normalizeStatus(firstNonBlank(
          stringValue(checkpoint.get("tag")),
          stringValue(checkpoint.get("status")),
          stringValue(checkpoint.get("subtag")),
          stringValue(tracking.get("tag")),
          stringValue(tracking.get("status")),
          stringValue(tracking.get("subtag"))
      ));
      String substatus = firstNonBlank(
          stringValue(checkpoint.get("subtag")),
          stringValue(tracking.get("subtag"))
      );
      String description = firstNonBlank(
          stringValue(checkpoint.get("message")),
          stringValue(checkpoint.get("description")),
          stringValue(checkpoint.get("subtag_message")),
          stringValue(tracking.get("subtag_message")),
          stringValue(tracking.get("status_label"))
      );
      String location = firstNonBlank(
          stringValue(checkpoint.get("location")),
          compactLocation(
              stringValue(checkpoint.get("city")),
              stringValue(checkpoint.get("state")),
              stringValue(checkpoint.get("postal_code")),
              stringValue(checkpoint.get("country_region_name")),
              stringValue(checkpoint.get("country_region"))
          )
      );

      String eventHash = hashEvent(parcel.getId(), status, substatus, description, location, eventTime);
      if (!parcelEventRepository.existsByParcel_IdAndEventHash(parcel.getId(), eventHash)) {
        ParcelEvent event = new ParcelEvent();
        event.setParcel(parcel);
        event.setEventHash(eventHash);
        event.setStatus(status);
        event.setSubstatus(substatus);
        event.setDescription(description);
        event.setLocation(location);
        event.setEventTime(eventTime);
        event.setRawPayload(new HashMap<>(checkpoint));
        try {
          parcelEventRepository.save(event);
        } catch (DataIntegrityViolationException ignored) {
          // Idempotence in concurrent webhook/API refreshes.
        }
      }

      if (latest == null || eventTime.toInstant().isAfter(latest.toInstant())) {
        latest = eventTime;
      }
    }
    return latest;
  }

  boolean isValidSignature(String payload, String signatureHeader, String sharedSecretHeader) {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      return false;
    }
    if (sharedSecretHeader != null && constantTimeEquals(sharedSecretHeader.trim(), webhookSecret.trim())) {
      return true;
    }
    if (signatureHeader == null || signatureHeader.isBlank()) {
      return false;
    }

    String candidate = signatureHeader.trim();
    if (candidate.startsWith("sha256=")) {
      candidate = candidate.substring("sha256=".length());
    }

    byte[] digest = hmacSha256(payload, webhookSecret);
    String base64 = Base64.getEncoder().encodeToString(digest);
    String hex = toHex(digest);
    return constantTimeEquals(candidate, base64) || constantTimeEquals(candidate.toLowerCase(Locale.ROOT), hex);
  }

  private TrackingWebhookEvent saveWebhookEvent(
      Map<String, Object> payloadMap,
      String externalEventId,
      boolean signatureValid
  ) {
    if (externalEventId != null) {
      Optional<TrackingWebhookEvent> existing =
          trackingWebhookEventRepository.findByAggregatorAndExternalEventId(AGGREGATOR, externalEventId);
      if (existing.isPresent()) {
        return existing.get();
      }
    }

    TrackingWebhookEvent event = new TrackingWebhookEvent();
    event.setAggregator(AGGREGATOR);
    event.setExternalEventId(externalEventId);
    event.setSignatureValid(signatureValid);
    event.setPayload(payloadMap);
    return trackingWebhookEventRepository.save(event);
  }

  private Optional<Parcel> findParcelForWebhook(Map<String, Object> tracking) {
    String trackingId = stringValue(tracking.get("id"));
    if (trackingId != null) {
      Optional<Parcel> byAggregator = parcelRepository.findByAggregatorAndAggregatorTrackingId(AGGREGATOR, trackingId);
      if (byAggregator.isPresent()) {
        return byAggregator;
      }
    }

    String trackingNumber = firstNonBlank(
        stringValue(tracking.get("tracking_number")),
        stringValue(tracking.get("trackingNumber"))
    );
    if (trackingNumber == null) {
      return Optional.empty();
    }
    String normalized = trackingNumber.replaceAll("[\\s\\-_.]", "").toUpperCase(Locale.ROOT);
    return parcelRepository.findFirstByNormalizedTrackingNumberOrderByUpdatedAtDesc(normalized);
  }

  private Map<String, Object> parsePayload(String payload) {
    try {
      return objectMapper.readValue(payload, MAP_TYPE);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload webhook invalide");
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> extractTrackingMap(Map<String, Object> root) {
    if (root == null) {
      return Map.of();
    }
    Object tracking = root.get("tracking");
    if (tracking instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    Object data = root.get("data");
    if (data instanceof Map<?, ?> dataMap) {
      Object nestedTracking = dataMap.get("tracking");
      if (nestedTracking instanceof Map<?, ?> map) {
        return (Map<String, Object>) map;
      }
      Object msg = dataMap.get("msg");
      if (msg instanceof Map<?, ?> map) {
        return (Map<String, Object>) map;
      }
    }
    Object msg = root.get("msg");
    if (msg instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return root;
  }

  private ParcelStatus normalizeStatus(String raw) {
    String status = raw == null ? "" : raw.toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-]", "");
    if (status.contains("delivered")) return ParcelStatus.DELIVERED;
    if (status.contains("outfordelivery") || status.contains("courier")) return ParcelStatus.OUT_FOR_DELIVERY;
    if (status.contains("intransit") || status.contains("transit")) return ParcelStatus.IN_TRANSIT;
    if (status.contains("attemptfail") || status.contains("exception") || status.contains("failed")
        || status.contains("expired")) {
      return ParcelStatus.EXCEPTION;
    }
    if (status.contains("pending") || status.contains("inforeceived") || status.contains("registered")) {
      return ParcelStatus.REGISTERED;
    }
    return ParcelStatus.UNKNOWN;
  }

  private OffsetDateTime estimatedDeliveryAt(Map<String, Object> tracking) {
    Object estimated = tracking.get("courier_estimated_delivery_date");
    if (estimated instanceof Map<?, ?> map) {
      OffsetDateTime value = parseNullableDateTime(firstNonBlank(
          stringValue(map.get("estimated_delivery_date")),
          stringValue(map.get("estimated_delivery_date_min")),
          stringValue(map.get("estimated_delivery_date_max"))
      ));
      if (value != null) {
        return value;
      }
    }

    return parseNullableDateTime(firstNonBlank(
        stringValue(tracking.get("estimated_delivery_date")),
        stringValue(tracking.get("expected_delivery")),
        stringValue(tracking.get("expected_delivery_date"))
    ));
  }

  private OffsetDateTime parseDateTime(String value) {
    OffsetDateTime parsed = parseNullableDateTime(value);
    return parsed == null ? OffsetDateTime.now(ZoneOffset.UTC) : parsed;
  }

  private OffsetDateTime parseNullableDateTime(String value) {
    if (value == null || value.isBlank()) {
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

  private String hashEvent(
      Long parcelId,
      ParcelStatus status,
      String substatus,
      String description,
      String location,
      OffsetDateTime eventTime
  ) {
    String input = parcelId + "|" + status + "|" + safe(substatus) + "|" + safe(description)
        + "|" + safe(location) + "|" + eventTime.toInstant();
    try {
      return toHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to hash parcel event", ex);
    }
  }

  private byte[] hmacSha256(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal((payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to verify webhook signature", ex);
    }
  }

  private boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }

  private String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
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
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String compactLocation(String... parts) {
    StringBuilder sb = new StringBuilder();
    if (parts == null) {
      return null;
    }
    for (String part : parts) {
      if (part == null || part.isBlank()) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(part.trim());
    }
    return sb.length() == 0 ? null : sb.toString();
  }

  private boolean hasUsableCarrierSlug(Parcel parcel) {
    return parcel.getCarrierSlug() != null
        && !parcel.getCarrierSlug().isBlank()
        && !"unknown".equalsIgnoreCase(parcel.getCarrierSlug());
  }

  private String afterShipSlug(String carrierSlug) {
    if (carrierSlug == null) {
      return null;
    }
    return switch (carrierSlug.trim().toLowerCase(Locale.ROOT)) {
      case "chronopost" -> "chronopost-france";
      case "mondial-relay" -> "mondialrelay-fr";
      case "la-poste", "laposte" -> "colissimo";
      default -> carrierSlug.trim().toLowerCase(Locale.ROOT);
    };
  }

  private void markRegisteredLocally(Parcel parcel) {
    if (parcel.getStatus() == null || parcel.getStatus() == ParcelStatus.PENDING) {
      parcel.setStatus(ParcelStatus.REGISTERED);
    }
    if (parcel.getStatusLabel() == null || parcel.getStatusLabel().isBlank()) {
      parcel.setStatusLabel("Suivi enregistre");
    }
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }
}
