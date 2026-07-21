package backend.dto;

import backend.entity.Parcel;
import backend.entity.ParcelEvent;
import backend.entity.ParcelStatus;
import backend.service.TrackingLinkResolver;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
    boolean completionRequired,
    String completionHint,
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
        TrackingLinkResolver.preferredTrackingUrl(
            firstNonBlank(
                stringValue(payload, "courier_tracking_link"),
                stringValue(payload, "tracking_url"),
                stringValue(payload, "url"),
                stringValue(payload, "tracking_link")
            ),
            parcel.getCarrierSlug(),
            parcel.getTrackingNumber()
        ),
        false,
        null,
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

}
