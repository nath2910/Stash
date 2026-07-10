package backend.dto;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Locale;
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
        firstNonBlank(
            stringValue(payload, "courier_tracking_link"),
            stringValue(payload, "tracking_url"),
            stringValue(payload, "url"),
            stringValue(payload, "tracking_link"),
            fallbackTrackingUrl(parcel.getCarrierSlug(), parcel.getTrackingNumber())
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
