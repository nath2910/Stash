package backend.service;

import java.util.Locale;
import java.util.Set;

public final class TrackingLinkResolver {

  private static final Set<String> TRUSTED_DOMAINS = Set.of("laposte.fr", "colissimo.fr", "suivi.laposte.fr");

  private TrackingLinkResolver() {
  }

  public static String preferredTrackingUrl(String rawUrl, String carrierSlug, String trackingNumber) {
    if (isTrustedTrackingUrl(rawUrl, carrierSlug) || detectCarrierSlug(rawUrl) != null) {
      return rawUrl == null ? null : rawUrl.trim();
    }
    return fallbackTrackingUrl(carrierSlug, trackingNumber);
  }

  public static String detectCarrierSlug(String rawUrl) {
    return TrackingCarrierRules.detectCarrierSlugFromUrl(rawUrl);
  }

  public static boolean isTrustedTrackingUrl(String rawUrl, String carrierSlug) {
    if (rawUrl == null || rawUrl.isBlank() || !TrackingCarrierRules.isSupportedCarrier(carrierSlug)) {
      return false;
    }
    String normalizedUrl = rawUrl.trim().toLowerCase(Locale.ROOT);
    if (!normalizedUrl.startsWith("http")) {
      return false;
    }
    return TRUSTED_DOMAINS.stream().anyMatch(normalizedUrl::contains);
  }

  public static String fallbackTrackingUrl(String carrierSlug, String trackingNumber) {
    return TrackingCarrierRules.officialTrackingUrl(carrierSlug, trackingNumber);
  }
}
