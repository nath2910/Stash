package backend.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class TrackingCarrierRules {

  private static final String COLISSIMO = "colissimo";
  private static final String CHRONOPOST = "chronopost";
  private static final Pattern COLISSIMO_UPU_PATTERN =
      Pattern.compile("^(?!X[A-Z])[A-Z]{2}\\d{9}FR$", Pattern.CASE_INSENSITIVE);
  private static final Pattern LA_POSTE_2C_PATTERN =
      Pattern.compile("^\\d[A-Z]\\d{11}$", Pattern.CASE_INSENSITIVE);
  private static final Pattern COLISSIMO_13_DIGITS_PATTERN = Pattern.compile("^\\d{13}$");
  private static final Pattern COLISSIMO_14_DIGITS_PATTERN = Pattern.compile("^\\d{14}$");
  private static final Pattern COLISSIMO_15_DIGITS_PATTERN = Pattern.compile("^\\d{15}$");
  private static final Pattern COLISSIMO_15_CHARS_PATTERN =
      Pattern.compile("^\\d{14}[A-Z]$", Pattern.CASE_INSENSITIVE);
  private static final Pattern CHRONOPOST_UPU_PATTERN =
      Pattern.compile("^[A-Z]{2}\\d{9}[A-Z]{2}$", Pattern.CASE_INSENSITIVE);
  private static final Pattern CHRONOPOST_15_CHARS_PATTERN =
      Pattern.compile("^\\d{14}[A-Z]$", Pattern.CASE_INSENSITIVE);
  private static final Set<String> COLISSIMO_TRUSTED_DOMAINS = Set.of("colissimo.fr", "suivi.laposte.fr");
  private static final Set<String> CHRONOPOST_TRUSTED_DOMAINS = Set.of("chronopost.fr");
  private static final String SHARED_LAPOSTE_DOMAIN = "laposte.fr";
  private static final String SHARED_LAPOSTE_TRACKING_PATH = "/outils/suivre-vos-envois";

  private TrackingCarrierRules() {
  }

  static String normalizeCarrierSlug(String carrierSlug) {
    if (carrierSlug == null || carrierSlug.isBlank()) {
      return null;
    }
    return switch (carrierSlug.trim().toLowerCase(Locale.ROOT)) {
      case "colissimo", "laposte", "la poste", "la-poste" -> COLISSIMO;
      case "chronopost", "chrono post", "chrono-post" -> CHRONOPOST;
      default -> carrierSlug.trim().toLowerCase(Locale.ROOT);
    };
  }

  static boolean isSupportedCarrier(String carrierSlug) {
    String normalized = normalizeCarrierSlug(carrierSlug);
    return COLISSIMO.equals(normalized) || CHRONOPOST.equals(normalized);
  }

  static boolean isValidForCarrier(String trackingNumber, String carrierSlug) {
    String normalizedCarrier = normalizeCarrierSlug(carrierSlug);
    if (!isSupportedCarrier(normalizedCarrier)) {
      return false;
    }
    String normalized = normalizeTrackingNumber(trackingNumber);
    return switch (normalizedCarrier) {
      case COLISSIMO -> matchesColissimoFormat(normalized);
      case CHRONOPOST -> matchesChronopostFormat(normalized);
      default -> false;
    };
  }

  static boolean matchesSupportedCarrierFormat(String trackingNumber) {
    return !matchSupportedCarriers(trackingNumber).isEmpty();
  }

  static List<String> matchSupportedCarriers(String trackingNumber) {
    String normalized = normalizeTrackingNumber(trackingNumber);
    List<String> carriers = new java.util.ArrayList<>();
    if (matchesColissimoFormat(normalized)) {
      carriers.add(COLISSIMO);
    }
    if (matchesChronopostFormat(normalized)) {
      carriers.add(CHRONOPOST);
    }
    return carriers;
  }

  static String inferSupportedCarrier(String trackingNumber) {
    List<String> carriers = matchSupportedCarriers(trackingNumber);
    return carriers.size() == 1 ? carriers.get(0) : null;
  }

  static String officialTrackingUrl(String carrierSlug, String trackingNumber) {
    String normalizedCarrier = normalizeCarrierSlug(carrierSlug);
    if (!isSupportedCarrier(normalizedCarrier)) {
      return null;
    }
    String normalized = normalizeTrackingNumber(trackingNumber);
    return switch (normalizedCarrier) {
      case COLISSIMO -> matchesColissimoFormat(normalized)
          ? "https://www.laposte.fr/outils/suivre-vos-envois?code=" + encode(normalized)
          : null;
      case CHRONOPOST -> matchesChronopostFormat(normalized)
          ? "https://www.laposte.fr/outils/suivre-vos-envois?code=" + encode(normalized)
          : null;
      default -> null;
    };
  }

  static String detectCarrierSlugFromUrl(String rawUrl) {
    ParsedTrackingUrl parsedUrl = parseTrackingUrl(rawUrl);
    if (parsedUrl == null) {
      return null;
    }
    if (matchesAnyDomain(parsedUrl.host(), CHRONOPOST_TRUSTED_DOMAINS)) {
      return CHRONOPOST;
    }
    if (matchesAnyDomain(parsedUrl.host(), COLISSIMO_TRUSTED_DOMAINS)) {
      return COLISSIMO;
    }
    if (isSharedLaPosteTrackingUrl(parsedUrl)) {
      return inferSupportedCarrier(parsedUrl.queryParam("code"));
    }
    return null;
  }

  static Set<String> trustedDomains(String carrierSlug) {
    return switch (normalizeCarrierSlug(carrierSlug)) {
      case COLISSIMO -> COLISSIMO_TRUSTED_DOMAINS;
      case CHRONOPOST -> CHRONOPOST_TRUSTED_DOMAINS;
      default -> Set.of();
    };
  }

  static boolean isTrustedTrackingUrl(String rawUrl, String carrierSlug) {
    String normalizedCarrier = normalizeCarrierSlug(carrierSlug);
    if (!isSupportedCarrier(normalizedCarrier)) {
      return false;
    }

    ParsedTrackingUrl parsedUrl = parseTrackingUrl(rawUrl);
    if (parsedUrl == null) {
      return false;
    }

    return switch (normalizedCarrier) {
      case COLISSIMO -> matchesAnyDomain(parsedUrl.host(), COLISSIMO_TRUSTED_DOMAINS)
          || (isSharedLaPosteTrackingUrl(parsedUrl) && matchesColissimoFormat(parsedUrl.queryParam("code")));
      case CHRONOPOST -> matchesAnyDomain(parsedUrl.host(), CHRONOPOST_TRUSTED_DOMAINS)
          || (isSharedLaPosteTrackingUrl(parsedUrl) && matchesChronopostFormat(parsedUrl.queryParam("code")));
      default -> false;
    };
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

  private static boolean matchesChronopostFormat(String trackingNumber) {
    if (trackingNumber == null || trackingNumber.isBlank()) {
      return false;
    }
    return CHRONOPOST_UPU_PATTERN.matcher(trackingNumber).matches()
        || CHRONOPOST_15_CHARS_PATTERN.matcher(trackingNumber).matches();
  }

  private static String normalizeTrackingNumber(String value) {
    return value == null
        ? ""
        : value.replaceAll("[\\s\\-_.]", "").toUpperCase(Locale.ROOT);
  }

  private static ParsedTrackingUrl parseTrackingUrl(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      return null;
    }
    try {
      URI uri = URI.create(rawUrl.trim());
      String scheme = uri.getScheme();
      String host = uri.getHost();
      if (scheme == null || host == null || !scheme.toLowerCase(Locale.ROOT).startsWith("http")) {
        return null;
      }
      return new ParsedTrackingUrl(uri, host.toLowerCase(Locale.ROOT), normalizePath(uri.getPath()));
    } catch (Exception ignored) {
      return null;
    }
  }

  private static boolean isSharedLaPosteTrackingUrl(ParsedTrackingUrl parsedUrl) {
    return parsedUrl != null
        && matchesDomain(parsedUrl.host(), SHARED_LAPOSTE_DOMAIN)
        && SHARED_LAPOSTE_TRACKING_PATH.equals(parsedUrl.path());
  }

  private static boolean matchesAnyDomain(String host, Set<String> domains) {
    if (host == null || domains == null || domains.isEmpty()) {
      return false;
    }
    for (String domain : domains) {
      if (matchesDomain(host, domain)) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesDomain(String host, String domain) {
    if (host == null || domain == null || domain.isBlank()) {
      return false;
    }
    return host.equals(domain) || host.endsWith("." + domain);
  }

  private static String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }
    return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
  }

  private static String encode(String trackingNumber) {
    return URLEncoder.encode(trackingNumber.trim(), StandardCharsets.UTF_8);
  }

  private record ParsedTrackingUrl(URI uri, String host, String path) {

    String queryParam(String name) {
      if (name == null || name.isBlank() || uri.getQuery() == null || uri.getQuery().isBlank()) {
        return "";
      }
      for (String segment : uri.getQuery().split("&")) {
        String[] parts = segment.split("=", 2);
        if (parts.length == 2 && name.equalsIgnoreCase(parts[0])) {
          return normalizeTrackingNumber(parts[1]);
        }
      }
      return "";
    }
  }
}
