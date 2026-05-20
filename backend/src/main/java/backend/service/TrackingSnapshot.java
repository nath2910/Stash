package backend.service;

import backend.entity.ParcelStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record TrackingSnapshot(
    String provider,
    String providerTrackingId,
    String carrierSlug,
    ParcelStatus status,
    String statusLabel,
    OffsetDateTime estimatedDeliveryAt,
    OffsetDateTime deliveredAt,
    String trackingUrl,
    String originAddress,
    String destinationAddress,
    String shipmentType,
    String signedBy,
    Map<String, Object> rawPayload,
    List<TrackingEventSnapshot> events
) {
}
