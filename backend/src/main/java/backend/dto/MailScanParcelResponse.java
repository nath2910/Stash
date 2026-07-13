package backend.dto;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.service.TrackingLinkResolver;
import java.time.OffsetDateTime;
import java.util.Map;

public record MailScanParcelResponse(
    Long id,
    String trackingNumber,
    String carrierSlug,
    ParcelStatus status,
    String statusLabel,
    String trackingUrl,
    OffsetDateTime lastEventAt,
    OffsetDateTime estimatedDeliveryAt,
    OffsetDateTime deliveredAt
) {
  public static MailScanParcelResponse fromEntity(Parcel parcel) {
    if (parcel == null) {
      return null;
    }
    Map<String, Object> payload = parcel.getRawCurrentPayload();
    return new MailScanParcelResponse(
        parcel.getId(),
        parcel.getTrackingNumber(),
        parcel.getCarrierSlug(),
        parcel.getStatus(),
        parcel.getStatusLabel(),
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
        parcel.getLastEventAt(),
        parcel.getEstimatedDeliveryAt(),
        parcel.getDeliveredAt()
    );
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
