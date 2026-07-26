package backend.service;
public final class TrackingLinkResolver {

  private TrackingLinkResolver() {
  }

  public static String preferredTrackingUrl(String rawUrl, String carrierSlug, String trackingNumber) {
    if (isTrustedTrackingUrl(rawUrl, carrierSlug)) {
      return rawUrl == null ? null : rawUrl.trim();
    }
    if (!TrackingCarrierRules.isSupportedCarrier(carrierSlug) && detectCarrierSlug(rawUrl) != null) {
      return rawUrl == null ? null : rawUrl.trim();
    }
    return fallbackTrackingUrl(carrierSlug, trackingNumber);
  }

  public static String detectCarrierSlug(String rawUrl) {
    return TrackingCarrierRules.detectCarrierSlugFromUrl(rawUrl);
  }

  public static boolean isTrustedTrackingUrl(String rawUrl, String carrierSlug) {
    return TrackingCarrierRules.isTrustedTrackingUrl(rawUrl, carrierSlug);
  }

  public static String fallbackTrackingUrl(String carrierSlug, String trackingNumber) {
    return TrackingCarrierRules.officialTrackingUrl(carrierSlug, trackingNumber);
  }
}
