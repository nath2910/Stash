package backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class TrackingCarrierRules {

  private static final Pattern COLISSIMO_UPU_PATTERN =
      Pattern.compile("^(?!X[A-Z])[A-Z]{2}\\d{9}FR$", Pattern.CASE_INSENSITIVE);
  private static final Pattern LA_POSTE_2C_PATTERN =
      Pattern.compile("^\\d[A-Z]\\d{11}$", Pattern.CASE_INSENSITIVE);
  private static final Pattern COLISSIMO_13_DIGITS_PATTERN = Pattern.compile("^\\d{13}$");
  private static final Pattern COLISSIMO_14_DIGITS_PATTERN = Pattern.compile("^\\d{14}$");
  private static final Pattern COLISSIMO_15_DIGITS_PATTERN = Pattern.compile("^\\d{15}$");
  private static final Pattern COLISSIMO_15_CHARS_PATTERN =
      Pattern.compile("^\\d{14}[A-Z]$", Pattern.CASE_INSENSITIVE);
  private static final Set<String> TRUSTED_DOMAINS = Set.of("laposte.fr", "colissimo.fr", "suivi.laposte.fr");

  private TrackingCarrierRules() {
  }

  static String normalizeCarrierSlug(String carrierSlug) {
    if (carrierSlug == null || carrierSlug.isBlank()) {
      return null;
    }
    return switch (carrierSlug.trim().toLowerCase(Locale.ROOT)) {
      case "colissimo", "laposte", "la poste", "la-poste" -> "colissimo";
      default -> carrierSlug.trim().toLowerCase(Locale.ROOT);
    };
  }

  static boolean isSupportedCarrier(String carrierSlug) {
    return "colissimo".equals(normalizeCarrierSlug(carrierSlug));
  }

  static boolean isValidForCarrier(String trackingNumber, String carrierSlug) {
    if (!isSupportedCarrier(carrierSlug)) {
      return false;
    }
    String normalized = normalizeTrackingNumber(trackingNumber);
    return matchesColissimoFormat(normalized);
  }

  static boolean matchesSupportedCarrierFormat(String trackingNumber) {
    return matchesColissimoFormat(normalizeTrackingNumber(trackingNumber));
  }

  static List<String> matchSupportedCarriers(String trackingNumber) {
    return matchesSupportedCarrierFormat(trackingNumber) ? List.of("colissimo") : List.of();
  }

  static String inferSupportedCarrier(String trackingNumber) {
    return matchesSupportedCarrierFormat(trackingNumber) ? "colissimo" : null;
  }

  static String officialTrackingUrl(String carrierSlug, String trackingNumber) {
    if (!isSupportedCarrier(carrierSlug)) {
      return null;
    }
    String normalized = normalizeTrackingNumber(trackingNumber);
    if (!matchesColissimoFormat(normalized)) {
      return null;
    }
    return "https://www.laposte.fr/outils/suivre-vos-envois?code=" + encode(normalized);
  }

  static String detectCarrierSlugFromUrl(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      return null;
    }
    String normalizedUrl = rawUrl.trim().toLowerCase(Locale.ROOT);
    if (!normalizedUrl.startsWith("http")) {
      return null;
    }
    return TRUSTED_DOMAINS.stream().anyMatch(normalizedUrl::contains) ? "colissimo" : null;
  }

  static Set<String> trustedDomains(String carrierSlug) {
    return isSupportedCarrier(carrierSlug) ? TRUSTED_DOMAINS : Set.of();
  }

  private static boolean matchesColissimoFormat(String trackingNumber) {
    if (trackingNumber == null || trackingNumber.isBlank()) {
      return false;
    }
    return COLISSIMO_UPU_PATTERN.matcher(trackingNumber).matches()
        || LA_POSTE_2C_PATTERN.matcher(trackingNumber).matches()
        || COLISSIMO_13_DIGITS_PATTERN.matcher(trackingNumber).matches()
        || COLISSIMO_14_DIGITS_PATTERN.matcher(trackingNumber).matches()
        || COLISSIMO_15_DIGITS_PATTERN.matcher(trackingNumber).matches()
        || COLISSIMO_15_CHARS_PATTERN.matcher(trackingNumber).matches();
  }

  private static String normalizeTrackingNumber(String value) {
    return value == null
        ? ""
        : value.replaceAll("[\\s\\-_.]", "").toUpperCase(Locale.ROOT);
  }

  private static String encode(String trackingNumber) {
    return URLEncoder.encode(trackingNumber.trim(), StandardCharsets.UTF_8);
  }
}
