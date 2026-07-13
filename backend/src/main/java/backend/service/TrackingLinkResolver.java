package backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TrackingLinkResolver {

  private static final Map<String, Set<String>> TRUSTED_DOMAINS = Map.ofEntries(
      Map.entry("colissimo", Set.of("laposte.fr", "colissimo.fr", "suivi.laposte.fr")),
      Map.entry("laposte", Set.of("laposte.fr", "colissimo.fr", "suivi.laposte.fr")),
      Map.entry("la-poste", Set.of("laposte.fr", "colissimo.fr", "suivi.laposte.fr")),
      Map.entry("chronopost", Set.of("chronopost.fr")),
      Map.entry("mondial-relay", Set.of("mondialrelay.fr", "mondialrelay.com")),
      Map.entry("mondialrelay", Set.of("mondialrelay.fr", "mondialrelay.com")),
      Map.entry("ups", Set.of("ups.com", "upsemail.com")),
      Map.entry("fedex", Set.of("fedex.com")),
      Map.entry("dhl", Set.of("dhl.com", "dhlparcel.fr", "dhlparcel.nl", "dhl.de")),
      Map.entry("dpd", Set.of("dpd.fr", "dpd.com", "dpdgroup.com")),
      Map.entry("gls", Set.of("gls-group.com", "gls-group.eu", "gls-france.com")),
      Map.entry("amazon-logistics", Set.of("amazon.com", "amazon.fr", "track.amazon.com")),
      Map.entry("usps", Set.of("usps.com")),
      Map.entry("royal-mail", Set.of("royalmail.com")),
      Map.entry("china-post", Set.of("chinapost.com.cn", "ems.com.cn")),
      Map.entry("yunexpress", Set.of("yunexpress.com", "yuntrack.com"))
  );

  private TrackingLinkResolver() {
  }

  public static String preferredTrackingUrl(String rawUrl, String carrierSlug, String trackingNumber) {
    if (isTrustedTrackingUrl(rawUrl, carrierSlug) || (isBlank(carrierSlug) && detectCarrierSlug(rawUrl) != null)) {
      return rawUrl == null ? null : rawUrl.trim();
    }
    return fallbackTrackingUrl(carrierSlug, trackingNumber);
  }

  public static String detectCarrierSlug(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      return null;
    }
    String normalizedUrl = rawUrl.trim().toLowerCase(Locale.ROOT);
    if (!normalizedUrl.startsWith("http")) {
      return null;
    }
    return TRUSTED_DOMAINS.entrySet().stream()
        .filter(entry -> entry.getValue().stream().anyMatch(normalizedUrl::contains))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  public static boolean isTrustedTrackingUrl(String rawUrl, String carrierSlug) {
    if (rawUrl == null || rawUrl.isBlank() || carrierSlug == null || carrierSlug.isBlank()) {
      return false;
    }
    String normalizedUrl = rawUrl.trim().toLowerCase(Locale.ROOT);
    if (!normalizedUrl.startsWith("http")) {
      return false;
    }
    Set<String> domains = TRUSTED_DOMAINS.get(normalizeCarrier(carrierSlug));
    if (domains == null || domains.isEmpty()) {
      return false;
    }
    return domains.stream().anyMatch(normalizedUrl::contains);
  }

  public static String fallbackTrackingUrl(String carrierSlug, String trackingNumber) {
    if (carrierSlug == null || carrierSlug.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
      return null;
    }

    String encodedTracking = URLEncoder.encode(trackingNumber.trim(), StandardCharsets.UTF_8);
    return switch (normalizeCarrier(carrierSlug)) {
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

  private static String normalizeCarrier(String carrierSlug) {
    return carrierSlug.trim().toLowerCase(Locale.ROOT);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
