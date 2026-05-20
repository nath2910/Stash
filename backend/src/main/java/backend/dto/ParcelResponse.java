package backend.dto;

import backend.entity.Parcel;
import backend.entity.ParcelEvent;
import backend.entity.ParcelStatus;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record ParcelResponse(
    Long id,
    String trackingNumber,
    String normalizedTrackingNumber,
    String carrierSlug,
    String aggregator,
    String aggregatorTrackingId,
    ParcelStatus status,
    String statusLabel,
    OffsetDateTime estimatedDeliveryAt,
    OffsetDateTime deliveredAt,
    OffsetDateTime firstSeenAt,
    OffsetDateTime lastEventAt,
    OffsetDateTime updatedAt,
    String trackingUrl,
    String originAddress,
    String destinationAddress,
    String shipmentType,
    String signedBy,
    List<ParcelEventResponse> events
) {
  public static ParcelResponse fromEntity(Parcel parcel, List<ParcelEvent> events) {
    Map<String, Object> payload = parcel.getRawCurrentPayload();
    return new ParcelResponse(
        parcel.getId(),
        parcel.getTrackingNumber(),
        parcel.getNormalizedTrackingNumber(),
        parcel.getCarrierSlug(),
        parcel.getAggregator(),
        parcel.getAggregatorTrackingId(),
        parcel.getStatus(),
        parcel.getStatusLabel(),
        parcel.getEstimatedDeliveryAt(),
        parcel.getDeliveredAt(),
        parcel.getFirstSeenAt(),
        parcel.getLastEventAt(),
        parcel.getUpdatedAt(),
        firstNonBlank(
            stringValue(payload, "courier_tracking_link"),
            stringValue(payload, "tracking_url"),
            stringValue(payload, "url"),
            stringValue(payload, "tracking_link"),
            fallbackTrackingUrl(parcel.getCarrierSlug(), parcel.getTrackingNumber())
        ),
        firstNonBlank(stringValue(payload, "origin_raw_location"), address(payload, "origin")),
        firstNonBlank(stringValue(payload, "destination_raw_location"), address(payload, "destination")),
        stringValue(payload, "shipment_type"),
        stringValue(payload, "signed_by"),
        events == null ? List.of() : events.stream().map(ParcelEventResponse::fromEntity).toList()
    );
  }

  private static String address(Map<String, Object> payload, String prefix) {
    List<String> parts = new ArrayList<>();
    addIfPresent(parts, stringValue(payload, prefix + "_city"));
    addIfPresent(parts, stringValue(payload, prefix + "_state"));
    addIfPresent(parts, stringValue(payload, prefix + "_postal_code"));
    addIfPresent(parts, stringValue(payload, prefix + "_country_region"));
    return parts.isEmpty() ? null : String.join(", ", parts);
  }

  private static void addIfPresent(List<String> parts, String value) {
    if (value != null && !value.isBlank()) {
      parts.add(value);
    }
  }

  private static String stringValue(Map<String, Object> payload, String key) {
    if (payload == null || key == null) {
      return null;
    }
    Object value = payload.get(key);
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private static String firstNonBlank(String... values) {
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

  private static String fallbackTrackingUrl(String carrierSlug, String trackingNumber) {
    if (carrierSlug == null || carrierSlug.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
      return null;
    }

    String encodedTracking = URLEncoder.encode(trackingNumber.trim(), StandardCharsets.UTF_8);
    return switch (carrierSlug.trim().toLowerCase(Locale.ROOT)) {
      case "colissimo", "laposte", "la-poste" ->
          "https://www.laposte.fr/outils/suivre-vos-envois?code=" + encodedTracking;
      case "chronopost" ->
          "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=" + encodedTracking;
      case "mondial-relay", "mondialrelay" ->
          "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=" + encodedTracking;
      case "ups" ->
          "https://www.ups.com/track?tracknum=" + encodedTracking;
      case "fedex" ->
          "https://www.fedex.com/fedextrack/?trknbr=" + encodedTracking;
      case "dhl" ->
          "https://www.dhl.com/global-en/home/tracking.html?tracking-id=" + encodedTracking;
      case "dpd" ->
          "https://www.dpd.fr/trace/" + encodedTracking;
      case "gls" ->
          "https://gls-group.com/FR/fr/suivi-colis?match=" + encodedTracking;
      case "amazon-logistics" ->
          "https://track.amazon.com/tracking/" + encodedTracking;
      case "usps" ->
          "https://tools.usps.com/go/TrackConfirmAction?tLabels=" + encodedTracking;
      case "royal-mail" ->
          "https://www.royalmail.com/track-your-item#/tracking-results/" + encodedTracking;
      default -> null;
    };
  }
}
