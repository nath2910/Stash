package backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class TrackingCarrierRules {

  private static final Pattern UPU_PATTERN = Pattern.compile("^[A-Z]{2}\\d{9}[A-Z]{2}$", Pattern.CASE_INSENSITIVE);
  private static final Pattern LA_POSTE_2C_PATTERN = Pattern.compile("^\\d[A-Z]\\d{11}$", Pattern.CASE_INSENSITIVE);
  private static final Pattern COLISSIMO_NUMERIC_PATTERN = Pattern.compile("^\\d{13}$");
  private static final Pattern MONDIAL_RELAY_PATTERN = Pattern.compile("^\\d{8,12}$");
  private static final Pattern CHRONOPOST_X_UPU_PATTERN = Pattern.compile("^X[A-Z]\\d{9}(?:FR|TS)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern CHRONOPOST_GENERIC_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)[A-Z0-9]{13,16}$");
  private static final Set<String> NON_CHRONOPOST_UPU_SUFFIXES = Set.of("FR", "CN", "US", "GB");

  private static final CarrierDefinition CHRONOPOST = new CarrierDefinition(
      "chronopost",
      "Chronopost",
      300,
      Set.of("chronopost.fr"),
      CHRONOPOST_X_UPU_PATTERN,
      trackingNumber -> {
        if (matches(CHRONOPOST_X_UPU_PATTERN, trackingNumber)) {
          return true;
        }
        if (matches(UPU_PATTERN, trackingNumber)) {
          return !NON_CHRONOPOST_UPU_SUFFIXES.contains(suffix(trackingNumber));
        }
        return matches(CHRONOPOST_GENERIC_PATTERN, trackingNumber)
            && !matches(UPU_PATTERN, trackingNumber)
            && !matches(LA_POSTE_2C_PATTERN, trackingNumber)
            && !matches(COLISSIMO_NUMERIC_PATTERN, trackingNumber);
      },
      trackingNumber -> "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT="
          + encode(trackingNumber)
  );

  private static final CarrierDefinition COLISSIMO = new CarrierDefinition(
      "colissimo",
      "Colissimo / La Poste",
      200,
      Set.of("laposte.fr", "colissimo.fr", "suivi.laposte.fr"),
      Pattern.compile("^(?!X[A-Z])[A-Z]{2}\\d{9}FR$", Pattern.CASE_INSENSITIVE),
      trackingNumber -> matches(LA_POSTE_2C_PATTERN, trackingNumber)
          || matches(COLISSIMO_NUMERIC_PATTERN, trackingNumber)
          || (matches(UPU_PATTERN, trackingNumber)
          && "FR".equals(suffix(trackingNumber))
          && !matches(CHRONOPOST_X_UPU_PATTERN, trackingNumber)),
      trackingNumber -> "https://www.laposte.fr/outils/suivre-vos-envois?code="
          + encode(trackingNumber)
  );

  private static final CarrierDefinition MONDIAL_RELAY = new CarrierDefinition(
      "mondial-relay",
      "Mondial Relay",
      100,
      Set.of("mondialrelay.fr", "mondialrelay.com"),
      MONDIAL_RELAY_PATTERN,
      trackingNumber -> matches(MONDIAL_RELAY_PATTERN, trackingNumber),
      trackingNumber -> "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition="
          + encode(trackingNumber)
  );

  private static final List<CarrierDefinition> SUPPORTED_CARRIERS = List.of(
      CHRONOPOST,
      COLISSIMO,
      MONDIAL_RELAY
  );

  private TrackingCarrierRules() {
  }

  static String normalizeCarrierSlug(String carrierSlug) {
    if (carrierSlug == null || carrierSlug.isBlank()) {
      return null;
    }
    return switch (carrierSlug.trim().toLowerCase(Locale.ROOT)) {
      case "la poste", "la-poste", "laposte" -> "colissimo";
      case "mondialrelay", "mondial relay" -> "mondial-relay";
      default -> carrierSlug.trim().toLowerCase(Locale.ROOT);
    };
  }

  static boolean isSupportedCarrier(String carrierSlug) {
    return definition(carrierSlug) != null;
  }

  static boolean isValidForCarrier(String trackingNumber, String carrierSlug) {
    CarrierDefinition definition = definition(carrierSlug);
    if (definition == null) {
      return false;
    }
    String normalized = normalizeTrackingNumber(trackingNumber);
    return !normalized.isBlank() && definition.validator().isValid(normalized);
  }

  static boolean matchesSupportedCarrierFormat(String trackingNumber) {
    return !matchSupportedCarriers(trackingNumber).isEmpty();
  }

  static List<String> matchSupportedCarriers(String trackingNumber) {
    String normalized = normalizeTrackingNumber(trackingNumber);
    if (normalized.isBlank()) {
      return List.of();
    }
    return SUPPORTED_CARRIERS.stream()
        .filter(definition -> definition.validator().isValid(normalized))
        .sorted((left, right) -> Integer.compare(right.priority(), left.priority()))
        .map(CarrierDefinition::slug)
        .toList();
  }

  static String inferSupportedCarrier(String trackingNumber) {
    String normalized = normalizeTrackingNumber(trackingNumber);
    if (normalized.isBlank()) {
      return null;
    }
    return SUPPORTED_CARRIERS.stream()
        .filter(definition -> definition.validator().isValid(normalized))
        .sorted((left, right) -> Integer.compare(right.priority(), left.priority()))
        .map(CarrierDefinition::slug)
        .findFirst()
        .orElse(null);
  }

  static String officialTrackingUrl(String carrierSlug, String trackingNumber) {
    CarrierDefinition definition = definition(carrierSlug);
    if (definition == null) {
      return null;
    }
    String normalized = normalizeTrackingNumber(trackingNumber);
    if (normalized.isBlank()) {
      return null;
    }
    return definition.urlBuilder().build(normalized);
  }

  static String detectCarrierSlugFromUrl(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      return null;
    }
    String normalizedUrl = rawUrl.trim().toLowerCase(Locale.ROOT);
    if (!normalizedUrl.startsWith("http")) {
      return null;
    }
    return SUPPORTED_CARRIERS.stream()
        .filter(definition -> definition.trustedDomains().stream().anyMatch(normalizedUrl::contains))
        .sorted((left, right) -> Integer.compare(right.priority(), left.priority()))
        .map(CarrierDefinition::slug)
        .findFirst()
        .orElse(null);
  }

  static Set<String> trustedDomains(String carrierSlug) {
    CarrierDefinition definition = definition(carrierSlug);
    if (definition == null) {
      return Set.of();
    }
    return new LinkedHashSet<>(definition.trustedDomains());
  }

  private static CarrierDefinition definition(String carrierSlug) {
    String normalizedCarrier = normalizeCarrierSlug(carrierSlug);
    if (normalizedCarrier == null) {
      return null;
    }
    return SUPPORTED_CARRIERS.stream()
        .filter(definition -> definition.slug().equals(normalizedCarrier))
        .findFirst()
        .orElse(null);
  }

  private static String normalizeTrackingNumber(String value) {
    return value == null
        ? ""
        : value.replaceAll("[\\s\\-_.]", "").toUpperCase(Locale.ROOT);
  }

  private static boolean matches(Pattern pattern, String value) {
    return pattern.matcher(value).matches();
  }

  private static String suffix(String value) {
    return value.length() < 2 ? "" : value.substring(value.length() - 2).toUpperCase(Locale.ROOT);
  }

  private static String encode(String trackingNumber) {
    return URLEncoder.encode(trackingNumber.trim(), StandardCharsets.UTF_8);
  }

  private record CarrierDefinition(
      String slug,
      String label,
      int priority,
      Set<String> trustedDomains,
      Pattern detector,
      TrackingValidator validator,
      TrackingUrlBuilder urlBuilder
  ) {
  }

  @FunctionalInterface
  private interface TrackingValidator {
    boolean isValid(String trackingNumber);
  }

  @FunctionalInterface
  private interface TrackingUrlBuilder {
    String build(String trackingNumber);
  }
}
