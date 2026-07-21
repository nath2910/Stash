package backend.service;

import backend.entity.Parcel;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Order(1000)
public class PublicTrackingPageClient implements CarrierTrackingClient {

  private static final String PROVIDER = "PUBLIC_TRACKING_PAGE";
  private static final Set<String> SUPPORTED_CARRIERS = Set.of(
      "chronopost",
      "mondial-relay",
      "mondialrelay",
      "ups",
      "fedex",
      "dhl",
      "dpd",
      "gls",
      "landmark-global",
      "landmarkglobal"
  );

  private final RestClient restClient;

  public PublicTrackingPageClient() {
    this.restClient = RestClient.builder().build();
  }

  @Override
  public boolean supports(Parcel parcel) {
    String carrierSlug = resolvedCarrier(parcel);
    return SUPPORTED_CARRIERS.contains(carrierSlug) && resolveTrackingUrl(parcel) != null;
  }

  @Override
  public boolean isConfigured() {
    return true;
  }

  @Override
  public Optional<TrackingSnapshot> fetchTracking(Parcel parcel) {
    String carrierSlug = resolvedCarrier(parcel);
    String trackingUrl = resolveTrackingUrl(parcel);
    if (trackingUrl == null || !SUPPORTED_CARRIERS.contains(carrierSlug)) {
      return Optional.empty();
    }

    String html = restClient.get()
        .uri(URI.create(trackingUrl))
        .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 MystashTracking/1.0")
        .retrieve()
        .body(String.class);
    if (looksLikeBotChallenge(html)) {
      return Optional.empty();
    }

    return TrackingPageStatusExtractor.extractStatus(html)
        .map(status -> new TrackingSnapshot(
            PROVIDER,
            firstNonBlank(parcel.getNormalizedTrackingNumber(), parcel.getTrackingNumber()),
            carrierSlug,
            status.status(),
            status.label(),
            null,
            null,
            trackingUrl,
            null,
            null,
            null,
            null,
            rawPayload(trackingUrl, status.label()),
            java.util.List.of()
        ));
  }

  private String resolveTrackingUrl(Parcel parcel) {
    if (parcel == null) {
      return null;
    }
    String carrierSlug = resolvedCarrier(parcel);
    if (!SUPPORTED_CARRIERS.contains(carrierSlug)) {
      return null;
    }
    String rawUrl = null;
    if (parcel.getRawCurrentPayload() != null) {
      Object value = parcel.getRawCurrentPayload().get("tracking_url");
      rawUrl = value == null ? null : String.valueOf(value);
    }
    String trackingNumber = firstNonBlank(parcel.getTrackingNumber(), parcel.getNormalizedTrackingNumber());
    return TrackingLinkResolver.preferredTrackingUrl(rawUrl, carrierSlug, trackingNumber);
  }

  private String resolvedCarrier(Parcel parcel) {
    String carrierSlug = normalizeCarrier(parcel);
    if (!carrierSlug.isBlank()) {
      return carrierSlug;
    }
    String rawUrl = null;
    if (parcel != null && parcel.getRawCurrentPayload() != null) {
      Object value = parcel.getRawCurrentPayload().get("tracking_url");
      rawUrl = value == null ? null : String.valueOf(value);
    }
    String detectedCarrier = TrackingLinkResolver.detectCarrierSlug(rawUrl);
    return detectedCarrier == null ? "" : detectedCarrier;
  }

  static boolean looksLikeBotChallenge(String html) {
    if (html == null || html.isBlank()) {
      return false;
    }
    String normalized = html.toLowerCase(Locale.ROOT);
    return normalized.contains("just a moment")
        || normalized.contains("enable javascript and cookies to continue")
        || normalized.contains("access denied")
        || normalized.contains("you don't have permission to access")
        || normalized.contains("attention required! | cloudflare")
        || normalized.contains("sorry, you have been blocked")
        || normalized.contains("cf-chl")
        || normalized.contains("challenges.cloudflare.com");
  }

  private Map<String, Object> rawPayload(String trackingUrl, String statusLabel) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("mode", "public_tracking_page");
    payload.put("tracking_url", trackingUrl);
    if (statusLabel != null && !statusLabel.isBlank()) {
      payload.put("status_label", statusLabel);
    }
    return payload;
  }

  private String normalizeCarrier(Parcel parcel) {
    if (parcel == null || parcel.getCarrierSlug() == null) {
      return "";
    }
    String normalized = parcel.getCarrierSlug().trim().toLowerCase(Locale.ROOT);
    return "unknown".equals(normalized) ? "" : normalized;
  }

  private String firstNonBlank(String primary, String secondary) {
    if (primary != null && !primary.isBlank()) {
      return primary.trim();
    }
    if (secondary != null && !secondary.isBlank()) {
      return secondary.trim();
    }
    return null;
  }
}
