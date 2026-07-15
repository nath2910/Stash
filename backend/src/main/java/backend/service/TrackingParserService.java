package backend.service;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class TrackingParserService {

  public static final int AUTO_IMPORT_THRESHOLD = 75;
  public static final int REVIEW_THRESHOLD = 55;

  private static final int CONTEXT_RADIUS = 180;
  private static final Pattern BASIC_TRACKING = Pattern.compile("^[A-Z0-9]{8,40}$");
  private static final Pattern UPS_PATTERN = Pattern.compile("^1Z[0-9A-Z]{16}$");
  private static final Pattern AMAZON_PATTERN = Pattern.compile("^TBA[0-9A-Z]{9,20}$");
  private static final Pattern UPU_PATTERN = Pattern.compile("^[A-Z]{2}\\d{9}[A-Z]{2}$");
  private static final Pattern LA_POSTE_2C_PATTERN = Pattern.compile("^\\d[A-Z]\\d{11}$");
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");
  private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
  private static final Pattern LETTER_PATTERN = Pattern.compile("[A-Z]");
  private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>()\"']+", Pattern.CASE_INSENSITIVE);
  private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("\\b(\\d{5})\\b");
  private static final Pattern POSTAL_CODE_LABEL_PATTERN = Pattern.compile(
      "\\b(?:code\\s+postal|cp|postal\\s+code|zip(?:\\s+code)?)\\b[^\\d]{0,12}(\\d{5})\\b",
      Pattern.CASE_INSENSITIVE
  );
  private static final Pattern MONDIAL_RELAY_POSTAL_QUERY_PATTERN = Pattern.compile(
      "[?&]codepostal=(\\d{5})(?:[&#]|$)",
      Pattern.CASE_INSENSITIVE
  );
  private static final Set<String> MONDIAL_RELAY_POSTAL_LINE_HINTS = Set.of(
      "mondial relay",
      "point relais",
      "relais",
      "adresse",
      "livraison",
      "destinataire",
      "pickup"
  );
  private static final Set<String> MONDIAL_RELAY_POSTAL_LINE_NEGATIVE_HINTS = Set.of(
      "commande",
      "order",
      "facture",
      "invoice",
      "montant",
      "prix",
      "total",
      "telephone",
      "mobile"
  );

  private static final List<Pattern> CANDIDATE_PATTERNS = List.of(
      Pattern.compile("\\b1Z[0-9A-Z][0-9A-Z\\s\\-]{14,22}[0-9A-Z]\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bTBA[0-9A-Z]{9,20}\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b[A-Z]{2}[\\s\\-]?\\d{9}[\\s\\-]?[A-Z]{2}\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b\\d[A-Z][\\s\\-]?\\d{11}\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b(?:JD|JJD)[0-9A-Z]{10,24}\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b(?:YT|LP|SY|CN)[0-9A-Z]{9,22}\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\b\\d[\\d\\s\\-]{6,30}\\d\\b")
  );

  private static final Set<String> PREFILTER_KEYWORDS = Set.of(
      "suivi",
      "colis",
      "livraison",
      "expedition",
      "expedie",
      "commande expediee",
      "votre colis",
      "numero de suivi",
      "numero d expedition",
      "suivre votre colis",
      "transporteur",
      "en transit",
      "livraison prevue",
      "livre",
      "relay",
      "point relais",
      "tracking",
      "shipment",
      "shipped",
      "delivery",
      "package",
      "parcel",
      "tracking number",
      "track your order",
      "your order has shipped",
      "in transit",
      "out for delivery",
      "delivered"
  );

  private static final Set<String> STRONG_POSITIVE_KEYWORDS = Set.of(
      "numero de suivi",
      "no de suivi",
      "n de suivi",
      "suivi colis",
      "tracking number",
      "tracking code",
      "shipment number",
      "tracking id",
      "track your package",
      "track your parcel",
      "numero d expedition",
      "suivre votre colis",
      "suivre mon colis",
      "votre numero de suivi",
      "numero de colis"
  );

  private static final Set<String> GENERIC_POSITIVE_KEYWORDS = Set.of(
      "votre colis",
      "colis",
      "livraison",
      "expedition",
      "expedie",
      "en transit",
      "transporteur",
      "point relais",
      "package",
      "parcel",
      "shipment",
      "delivery",
      "shipped",
      "carrier",
      "out for delivery",
      "delivered"
  );

  private static final Set<String> STRONG_NEGATIVE_KEYWORDS = Set.of(
      "numero de commande",
      "n de commande",
      "commande n",
      "commande no",
      "order number",
      "order no",
      "order id",
      "invoice number",
      "numero de facture",
      "facture n",
      "customer number",
      "numero client",
      "client n",
      "transaction id",
      "payment id",
      "reference paiement",
      "reference produit",
      "sku",
      "ticket support"
  );

  private static final Set<String> NEGATIVE_KEYWORDS = Set.of(
      "commande",
      "order",
      "invoice",
      "facture",
      "customer",
      "client",
      "transaction",
      "payment",
      "paiement",
      "telephone",
      "phone",
      "mobile",
      "zip",
      "postal",
      "montant",
      "total",
      "tva",
      "vat",
      "sku",
      "promo",
      "coupon",
      "paypal",
      "stripe",
      "iban",
      "ticket",
      "support"
  );

  private static final List<CarrierRule> RULES = List.of(
      new CarrierRule(
          "ups",
          "UPS",
          Set.of("ups.com", "upsemail.com"),
          Set.of("ups"),
          List.of(UPS_PATTERN),
          45,
          false
      ),
      new CarrierRule(
          "amazon-logistics",
          "Amazon Logistics",
          Set.of("amazon.", "amazon.com", "amazon.fr"),
          Set.of("amazon logistics", "amazon shipping"),
          List.of(AMAZON_PATTERN),
          45,
          false
      ),
      new CarrierRule(
          "colissimo",
          "Colissimo",
          Set.of("colissimo.fr", "laposte.fr", "suivi.laposte.fr", "notif-colissimo-laposte.info"),
          Set.of("colissimo", "la poste", "laposte"),
          List.of(UPU_PATTERN, LA_POSTE_2C_PATTERN, Pattern.compile("^\\d{13}$")),
          35,
          true
      ),
      new CarrierRule(
          "chronopost",
          "Chronopost",
          Set.of("chronopost.fr", "notifications.chronopost.fr"),
          Set.of("chronopost", "chrono post"),
          List.of(UPU_PATTERN, Pattern.compile("^[A-Z0-9]{13,16}$")),
          35,
          true
      ),
      new CarrierRule(
          "mondial-relay",
          "Mondial Relay",
          Set.of("mondialrelay.fr", "mondialrelay.com"),
          Set.of("mondial relay", "mondialrelay", "point relais"),
          List.of(Pattern.compile("^\\d{8,12}$")),
          30,
          true
      ),
      new CarrierRule(
          "dpd",
          "DPD",
          Set.of("dpd.fr", "dpd.com", "dpdgroup.com"),
          Set.of("dpd"),
          List.of(Pattern.compile("^\\d{12,14}$")),
          30,
          true
      ),
      new CarrierRule(
          "dhl",
          "DHL",
          Set.of("dhl.com", "dhlparcel.fr", "dhlparcel.nl", "dhl.de"),
          Set.of("dhl", "dhl parcel", "dhl express"),
          List.of(Pattern.compile("^\\d{10,11}$"), UPU_PATTERN, Pattern.compile("^(JD|JJD)[0-9A-Z]{10,24}$")),
          32,
          true
      ),
      new CarrierRule(
          "fedex",
          "FedEx",
          Set.of("fedex.com", "email.fedex.com"),
          Set.of("fedex", "fed ex"),
          List.of(Pattern.compile("^\\d{12}$"), Pattern.compile("^\\d{15}$"), Pattern.compile("^\\d{20}$"),
              Pattern.compile("^\\d{22}$")),
          30,
          true
      ),
      new CarrierRule(
          "gls",
          "GLS",
          Set.of("gls-group.eu", "gls-france.com", "gls-one.de"),
          Set.of("gls"),
          List.of(Pattern.compile("^\\d{11,14}$"), Pattern.compile("^[A-Z0-9]{8,12}$")),
          28,
          true
      ),
      new CarrierRule(
          "colis-prive",
          "Colis Prive",
          Set.of("colisprive.com", "colis-prive.com"),
          Set.of("colis prive"),
          List.of(Pattern.compile("^\\d{10,16}$")),
          28,
          true
      ),
      new CarrierRule(
          "relais-colis",
          "Relais Colis",
          Set.of("relaiscolis.com", "relais-colis.com"),
          Set.of("relais colis"),
          List.of(Pattern.compile("^\\d{10,16}$")),
          28,
          true
      ),
      new CarrierRule(
          "usps",
          "USPS",
          Set.of("usps.com"),
          Set.of("usps", "postal service"),
          List.of(UPU_PATTERN, Pattern.compile("^9\\d{21,25}$")),
          35,
          true
      ),
      new CarrierRule(
          "royal-mail",
          "Royal Mail",
          Set.of("royalmail.com"),
          Set.of("royal mail", "parcelforce"),
          List.of(UPU_PATTERN),
          32,
          true
      ),
      new CarrierRule(
          "china-post",
          "China Post",
          Set.of("chinapost.com.cn", "ems.com.cn"),
          Set.of("china post", "ems china"),
          List.of(UPU_PATTERN),
          32,
          true
      ),
      new CarrierRule(
          "cainiao",
          "Cainiao",
          Set.of("cainiao.com", "global.cainiao.com"),
          Set.of("cainiao", "aliexpress"),
          List.of(Pattern.compile("^(LP|SY|CN)[0-9A-Z]{9,22}$"), UPU_PATTERN),
          30,
          true
      ),
      new CarrierRule(
          "yunexpress",
          "YunExpress",
          Set.of("yunexpress.com", "yuntrack.com"),
          Set.of("yunexpress", "yun express", "yuntrack"),
          List.of(Pattern.compile("^YT\\d{12,18}$"), Pattern.compile("^YT[0-9A-Z]{12,20}$")),
          35,
          false
      )
  );

  public boolean looksLikeCarrierMessage(String from, String subject) {
    String haystack = normalizeText(String.join(" ", safe(from), safe(subject)));
    return RULES.stream().anyMatch(rule -> rule.hasSignal(haystack));
  }

  public boolean shouldInspectMessage(String from, String subject) {
    return shouldInspectMessage(from, subject, "");
  }

  public boolean shouldInspectMessage(String from, String subject, String body) {
    String haystack = normalizeText(String.join(" ", safe(from), safe(subject), safe(body)));
    if (haystack.isBlank()) {
      return false;
    }
    if (RULES.stream().anyMatch(rule -> rule.hasSignal(haystack))) {
      return true;
    }
    return PREFILTER_KEYWORDS.stream().anyMatch(haystack::contains);
  }

  public List<TrackingCandidate> parse(String from, String subject, String body) {
    return detect(from, subject, body, extractLinks(body)).autoImportCandidates();
  }

  public TrackingDetectionResult detect(String from, String subject, String body) {
    return detect(from, subject, body, extractLinks(body));
  }

  public TrackingDetectionResult detect(String from, String subject, String body, List<String> links) {
    if (!shouldInspectMessage(from, subject, body)) {
      return new TrackingDetectionResult(List.of(), List.of(), 0, 0);
    }

    String visibleText = safe(body);
    List<String> safeLinks = links == null ? List.of() : links.stream()
        .filter(link -> link != null && !link.isBlank())
        .map(String::trim)
        .distinct()
        .toList();
    String emailContext = normalizeText(String.join(" ", safe(from), safe(subject), visibleText,
        String.join(" ", safeLinks)));

    Map<String, TrackingCandidate> bestByTrackingNumber = new LinkedHashMap<>();
    int rejectedCount = 0;
    int duplicateCount = 0;

    for (Occurrence occurrence : extractOccurrences(visibleText, safeLinks)) {
      String normalized = normalizeTrackingNumber(occurrence.raw());
      CandidateScore score = scoreCandidate(occurrence, normalized, visibleText, safeLinks, from, subject, emailContext);
      if (score.rejected()) {
        rejectedCount++;
        continue;
      }

      TrackingConfidence confidence = confidence(score.value());
      if (confidence == TrackingConfidence.LOW) {
        rejectedCount++;
        continue;
      }

      String contextSnippet = contextSnippet(visibleText, occurrence.start(), occurrence.end(), CONTEXT_RADIUS);

      TrackingCandidate candidate = new TrackingCandidate(
          score.carrierSlug(),
          occurrence.raw().trim(),
          normalized,
          score.value(),
          confidence,
          bestTrackingUrl(normalized, score.carrierSlug(), occurrence, safeLinks, visibleText, contextSnippet),
          contextSnippet,
          displayName(from),
          rawStatus(visibleText),
          String.join("; ", score.reasons())
      );

      TrackingCandidate existing = bestByTrackingNumber.get(normalized);
      if (existing == null || candidate.confidenceScore() > existing.confidenceScore()) {
        if (existing != null) {
          duplicateCount++;
        }
        bestByTrackingNumber.put(normalized, candidate);
      } else {
        duplicateCount++;
      }
    }

    List<TrackingCandidate> autoImport = new ArrayList<>();
    List<TrackingCandidate> review = new ArrayList<>();
    bestByTrackingNumber.values().stream()
        .sorted(Comparator.comparing(TrackingCandidate::confidenceScore).reversed())
        .forEach(candidate -> {
          if (candidate.confidenceLevel() == TrackingConfidence.HIGH) {
            autoImport.add(candidate);
          } else if (candidate.confidenceLevel() == TrackingConfidence.MEDIUM) {
            review.add(candidate);
          }
        });

    return new TrackingDetectionResult(List.copyOf(autoImport), List.copyOf(review), rejectedCount, duplicateCount);
  }

  public String normalizeTrackingNumber(String value) {
    return safe(value)
        .replaceAll("[\\s\\-_.]", "")
        .toUpperCase(Locale.ROOT);
  }

  public boolean isValidTrackingNumber(String value) {
    String normalized = normalizeTrackingNumber(value);
    if (!BASIC_TRACKING.matcher(normalized).matches()) {
      return false;
    }
    if (looksLikeDate(normalized) || looksLikeAmountContext("", normalized)) {
      return false;
    }
    if (NUMERIC_PATTERN.matcher(normalized).matches()
        && normalized.length() < 10
        && !TrackingCarrierRules.matchesSupportedCarrierFormat(normalized)) {
      return false;
    }
    if (!hasBalancedTrackingSignal(normalized)) {
      return false;
    }
    return true;
  }

  public String inferCarrierSlug(String trackingNumber) {
    String normalized = normalizeTrackingNumber(trackingNumber);
    if (UPS_PATTERN.matcher(normalized).matches()) {
      return "ups";
    }
    if (AMAZON_PATTERN.matcher(normalized).matches()) {
      return "amazon-logistics";
    }
    if (Pattern.compile("^YT\\d{12,18}$").matcher(normalized).matches()) {
      return "yunexpress";
    }
    String supportedCarrier = TrackingCarrierRules.inferSupportedCarrier(normalized);
    if (supportedCarrier != null && !"mondial-relay".equals(supportedCarrier)) {
      return supportedCarrier;
    }
    if (UPU_PATTERN.matcher(normalized).matches()) {
      String suffix = normalized.substring(normalized.length() - 2);
      return switch (suffix) {
        case "CN" -> "china-post";
        case "US" -> "usps";
        case "GB" -> "royal-mail";
        default -> null;
      };
    }
    return null;
  }

  private List<Occurrence> extractOccurrences(String visibleText, List<String> links) {
    List<Occurrence> occurrences = new ArrayList<>();
    String text = safe(visibleText);
    for (Pattern pattern : CANDIDATE_PATTERNS) {
      Matcher matcher = pattern.matcher(text);
      while (matcher.find()) {
        occurrences.add(new Occurrence(matcher.group(), matcher.start(), matcher.end(), CandidateSource.VISIBLE, null));
      }
    }

    for (String link : links) {
      if (!isTrackingRelatedLink(link)) {
        continue;
      }
      String decoded = decodeUrl(link);
      for (Pattern pattern : CANDIDATE_PATTERNS) {
        Matcher matcher = pattern.matcher(decoded);
        while (matcher.find()) {
          occurrences.add(new Occurrence(matcher.group(), -1, -1, CandidateSource.LINK, link));
        }
      }
    }
    return occurrences;
  }

  private CandidateScore scoreCandidate(
      Occurrence occurrence,
      String normalized,
      String visibleText,
      List<String> links,
      String from,
      String subject,
      String emailContext
  ) {
    List<String> reasons = new ArrayList<>();
    if (!BASIC_TRACKING.matcher(normalized).matches()) {
      return CandidateScore.rejected("format invalide");
    }
    if (looksLikeDate(normalized)) {
      return CandidateScore.rejected("ressemble a une date");
    }
    if (!hasBalancedTrackingSignal(normalized)) {
      return CandidateScore.rejected("format trop faible");
    }

    String context = occurrence.source() == CandidateSource.VISIBLE
        ? contextSnippet(visibleText, occurrence.start(), occurrence.end(), CONTEXT_RADIUS)
        : decodeUrl(occurrence.link());
    String normalizedContext = normalizeText(context);
    String normalizedPrefix = occurrence.source() == CandidateSource.VISIBLE
        ? normalizeText(contextBefore(visibleText, occurrence.start(), 30))
        : normalizedContext;
    String normalizedSubject = normalizeText(subject);
    String carrierSlug = detectCarrier(normalized, emailContext, normalizedContext).orElse(null);
    int score = 0;

    FormatScore formatScore = formatScore(normalized, carrierSlug, emailContext);
    score += formatScore.value();
    reasons.add(formatScore.reason());

    if (carrierSlug != null) {
      score += 20;
      reasons.add("transporteur probable: " + carrierSlug);
    }

    boolean strongPositivePrefix = containsAny(normalizedPrefix, STRONG_POSITIVE_KEYWORDS);
    boolean strongPositiveContext = strongPositivePrefix || containsAny(normalizedContext, STRONG_POSITIVE_KEYWORDS);
    if (strongPositiveContext) {
      score += 25;
      reasons.add("contexte proche de suivi");
    } else if (containsAny(normalizedContext, GENERIC_POSITIVE_KEYWORDS)) {
      score += 12;
      reasons.add("contexte livraison");
    }

    if (containsAny(normalizedSubject, STRONG_POSITIVE_KEYWORDS)) {
      score += 10;
    } else if (containsAny(normalizedSubject, GENERIC_POSITIVE_KEYWORDS)) {
      score += 5;
    }

    if (linkContainsTracking(links, normalized)) {
      score += 15;
      reasons.add("present dans un lien de suivi");
    }

    if (containsAny(normalizedPrefix, STRONG_NEGATIVE_KEYWORDS)) {
      score -= 40;
      reasons.add("contexte proche de commande/facture");
    } else if (containsAny(normalizedContext, STRONG_NEGATIVE_KEYWORDS) && !strongPositivePrefix) {
      score -= 30;
      reasons.add("contexte mixte commande/facture");
    } else if (containsAny(normalizedContext, NEGATIVE_KEYWORDS) && !strongPositiveContext) {
      score -= 25;
      reasons.add("contexte administratif");
    }

    if (looksLikePhone(normalized, normalizedContext)) {
      score -= 25;
      reasons.add("ressemble a un telephone");
    }
    if (looksLikeAmountContext(normalizedContext, normalized)) {
      score -= 25;
      reasons.add("ressemble a un montant");
    }
    if (NUMERIC_PATTERN.matcher(normalized).matches() && carrierSlug == null) {
      score -= 20;
      reasons.add("numero purement numerique sans transporteur");
    }
    if (occurrence.source() == CandidateSource.LINK && !linkContainsTracking(links, normalized)) {
      score -= 30;
      reasons.add("present seulement dans une URL technique");
    }

    return new CandidateScore(Math.max(0, Math.min(100, score)), carrierSlug, false, reasons);
  }

  private Optional<String> detectCarrier(String normalized, String emailContext, String candidateContext) {
    if (UPS_PATTERN.matcher(normalized).matches()) {
      return Optional.of("ups");
    }
    if (AMAZON_PATTERN.matcher(normalized).matches()) {
      return Optional.of("amazon-logistics");
    }
    if (Pattern.compile("^YT\\d{12,18}$").matcher(normalized).matches()) {
      return Optional.of("yunexpress");
    }

    String combined = emailContext + " " + candidateContext;
    for (CarrierRule rule : RULES) {
      if (rule.hasSignal(combined) && rule.matchesFormat(normalized)) {
        return Optional.of(rule.slug());
      }
    }

    String inferred = inferCarrierSlug(normalized);
    return inferred == null ? Optional.empty() : Optional.of(inferred);
  }

  private FormatScore formatScore(String normalized, String carrierSlug, String emailContext) {
    if (UPS_PATTERN.matcher(normalized).matches()) {
      return new FormatScore(45, "format UPS reconnu");
    }
    if (AMAZON_PATTERN.matcher(normalized).matches()) {
      return new FormatScore(45, "format Amazon Logistics reconnu");
    }
    if (UPU_PATTERN.matcher(normalized).matches()) {
      return new FormatScore(40, "format postal UPU reconnu");
    }
    if (Pattern.compile("^YT\\d{12,18}$").matcher(normalized).matches()) {
      return new FormatScore(40, "format YunExpress reconnu");
    }
    if (LA_POSTE_2C_PATTERN.matcher(normalized).matches()) {
      return new FormatScore(35, "format Colissimo probable");
    }

    int best = 0;
    String reason = "format generique plausible";
    for (CarrierRule rule : RULES) {
      if (!rule.matchesFormat(normalized)) {
        continue;
      }
      boolean carrierSignal = carrierSlug != null && carrierSlug.equals(rule.slug()) || rule.hasSignal(emailContext);
      int value = rule.requiresCarrierContext() && !carrierSignal
          ? Math.min(18, rule.formatBoost())
          : rule.formatBoost();
      if (value > best) {
        best = value;
        reason = "format " + rule.name() + (carrierSignal ? " reconnu" : " plausible");
      }
    }

    if (best > 0) {
      return new FormatScore(best, reason);
    }

    if (NUMERIC_PATTERN.matcher(normalized).matches()) {
      return new FormatScore(normalized.length() >= 12 ? 12 : 5, "format numerique faible");
    }
    return new FormatScore(10, "format alphanumerique faible");
  }

  private TrackingConfidence confidence(int score) {
    if (score >= AUTO_IMPORT_THRESHOLD) {
      return TrackingConfidence.HIGH;
    }
    if (score >= REVIEW_THRESHOLD) {
      return TrackingConfidence.MEDIUM;
    }
    return TrackingConfidence.LOW;
  }

  private List<String> extractLinks(String body) {
    String text = safe(body);
    List<String> links = new ArrayList<>();
    Matcher matcher = URL_PATTERN.matcher(text);
    while (matcher.find()) {
      links.add(matcher.group());
    }
    return links;
  }

  private boolean isTrackingRelatedLink(String link) {
    String normalized = normalizeText(decodeUrl(link));
    if (normalized.length() > 2000) {
      return false;
    }
    boolean carrierDomain = RULES.stream().anyMatch(rule -> rule.domains().stream().anyMatch(normalized::contains));
    boolean trackingKeyword = containsAny(normalized, Set.of(
        "track",
        "tracking",
        "suivi",
        "colis",
        "parcel",
        "package",
        "shipment",
        "livraison",
        "expedition"
    ));
    return carrierDomain || trackingKeyword;
  }

  private boolean linkContainsTracking(List<String> links, String normalizedTracking) {
    for (String link : links) {
      if (!isTrackingRelatedLink(link)) {
        continue;
      }
      String decoded = normalizeTrackingNumber(decodeUrl(link));
      if (decoded.contains(normalizedTracking)) {
        return true;
      }
    }
    return false;
  }

  private String bestTrackingUrl(
      String normalizedTracking,
      String carrierSlug,
      Occurrence occurrence,
      List<String> links,
      String visibleText,
      String candidateContext
  ) {
    String trustedUrl = trustedTrackingUrl(normalizedTracking, carrierSlug, occurrence, links);
    String normalizedCarrier = TrackingCarrierRules.normalizeCarrierSlug(carrierSlug);
    if (!"mondial-relay".equals(normalizedCarrier)) {
      return trustedUrl;
    }

    String postalCode = mondialRelayPostalCodeFromUrl(trustedUrl);
    if (postalCode == null) {
      postalCode = extractMondialRelayPostalCode(visibleText, candidateContext, links);
    }
    if (postalCode != null) {
      return buildMondialRelayTrackingUrl(normalizedTracking, postalCode);
    }
    return trustedUrl;
  }

  private String trustedTrackingUrl(
      String normalizedTracking,
      String carrierSlug,
      Occurrence occurrence,
      List<String> links
  ) {
    if (
        occurrence.source() == CandidateSource.LINK &&
        isTrackingRelatedLink(occurrence.link()) &&
        TrackingLinkResolver.isTrustedTrackingUrl(occurrence.link(), carrierSlug)
    ) {
      return sanitizeUrl(occurrence.link());
    }
    for (String link : links) {
      if (
          isTrackingRelatedLink(link) &&
          normalizeTrackingNumber(decodeUrl(link)).contains(normalizedTracking) &&
          TrackingLinkResolver.isTrustedTrackingUrl(link, carrierSlug)
      ) {
        return sanitizeUrl(link);
      }
    }
    return null;
  }

  private String extractMondialRelayPostalCode(String visibleText, String candidateContext, List<String> links) {
    for (String link : links) {
      if (!TrackingLinkResolver.isTrustedTrackingUrl(link, "mondial-relay")) {
        continue;
      }
      String postalCode = mondialRelayPostalCodeFromUrl(link);
      if (postalCode != null) {
        return postalCode;
      }
    }

    String postalCode = bestPostalCodeFromText(candidateContext, true);
    if (postalCode != null) {
      return postalCode;
    }
    return bestPostalCodeFromText(visibleText, false);
  }

  private String bestPostalCodeFromText(String text, boolean contextualScan) {
    String safeText = safe(text);
    if (safeText.isBlank()) {
      return null;
    }

    Matcher labeledMatcher = POSTAL_CODE_LABEL_PATTERN.matcher(safeText);
    if (labeledMatcher.find()) {
      return labeledMatcher.group(1);
    }

    List<PostalCodeScore> candidates = new ArrayList<>();
    String[] lines = safeText.split("\\R+");
    for (String line : lines) {
      String normalizedLine = normalizeText(line);
      if (normalizedLine.isBlank()) {
        continue;
      }

      int lineScore = 0;
      if (containsAny(normalizedLine, MONDIAL_RELAY_POSTAL_LINE_HINTS)) {
        lineScore += 25;
      }
      if (containsAny(normalizedLine, MONDIAL_RELAY_POSTAL_LINE_NEGATIVE_HINTS)) {
        lineScore -= 20;
      }
      if (normalizedLine.contains("code postal") || normalizedLine.matches(".*\\bcp\\b.*")) {
        lineScore += 35;
      }
      if (normalizedLine.matches(".*\\b\\d{5}\\s+[a-z].*")) {
        lineScore += 10;
      }

      Matcher matcher = POSTAL_CODE_PATTERN.matcher(line);
      while (matcher.find()) {
        candidates.add(new PostalCodeScore(matcher.group(1), lineScore));
      }
    }

    if (candidates.isEmpty()) {
      return null;
    }

    candidates.sort((left, right) -> Integer.compare(right.score(), left.score()));
    PostalCodeScore best = candidates.get(0);
    int minimumScore = contextualScan ? 15 : 25;
    if (best.score() < minimumScore) {
      return null;
    }

    boolean conflictingBest = candidates.stream()
        .filter(candidate -> candidate.score() == best.score())
        .map(PostalCodeScore::postalCode)
        .distinct()
        .count() > 1;
    if (conflictingBest) {
      return null;
    }

    return best.postalCode();
  }

  private String mondialRelayPostalCodeFromUrl(String link) {
    String decoded = decodeUrl(link).toLowerCase(Locale.ROOT);
    Matcher matcher = MONDIAL_RELAY_POSTAL_QUERY_PATTERN.matcher(decoded);
    if (!matcher.find()) {
      return null;
    }
    return matcher.group(1);
  }

  private String buildMondialRelayTrackingUrl(String normalizedTracking, String postalCode) {
    return "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition="
        + URLEncoder.encode(normalizedTracking, StandardCharsets.UTF_8)
        + "&codePostal="
        + URLEncoder.encode(postalCode, StandardCharsets.UTF_8);
  }

  private String sanitizeUrl(String link) {
    if (link == null) {
      return null;
    }
    String trimmed = link.trim();
    if (!trimmed.toLowerCase(Locale.ROOT).startsWith("http") || trimmed.length() > 2000) {
      return null;
    }
    return trimmed;
  }

  private String contextSnippet(String body, int start, int end, int radius) {
    String text = safe(body).replaceAll("\\s+", " ").trim();
    if (text.isBlank()) {
      return "";
    }
    if (start < 0 || end < 0 || start >= text.length()) {
      return text.substring(0, Math.min(text.length(), 320));
    }
    int safeStart = Math.max(0, start - radius);
    int safeEnd = Math.min(text.length(), end + radius);
    return text.substring(safeStart, safeEnd).trim();
  }

  private String contextBefore(String body, int start, int radius) {
    String text = safe(body);
    if (start <= 0 || text.isBlank()) {
      return "";
    }
    int safeStart = Math.max(0, start - radius);
    return text.substring(safeStart, Math.min(start, text.length()));
  }

  private String displayName(String from) {
    String value = safe(from).trim();
    if (value.isBlank()) {
      return null;
    }
    int emailStart = value.indexOf('<');
    if (emailStart > 0) {
      value = value.substring(0, emailStart).trim();
    }
    value = value.replace("\"", "").trim();
    return value.isBlank() ? null : value;
  }

  private String rawStatus(String body) {
    String normalized = normalizeText(body);
    if (normalized.isBlank()) {
      return null;
    }

    int deliveredScore = scoreStatus(normalized, Set.of(
        "delivered",
        "successfully delivered",
        "parcel delivered",
        "colis livre",
        "votre colis est livre",
        "votre colis a ete livre",
        "remis au destinataire",
        "remis en boite aux lettres",
        "livraison effectuee",
        "livraison terminee",
        "distribue"
    ));
    int outForDeliveryScore = scoreStatus(normalized, Set.of(
        "out for delivery",
        "en cours de livraison",
        "en livraison",
        "livraison ce jour",
        "votre colis est en cours de livraison"
    ));
    int inTransitScore = scoreStatus(normalized, Set.of(
        "in transit",
        "shipment in transit",
        "en transit",
        "pris en charge",
        "achemine",
        "acheminement"
    ));
    int shippedScore = scoreStatus(normalized, Set.of(
        "shipped",
        "shipment information received",
        "expedie",
        "etiquette creee",
        "label created"
    ));

    if (deliveredScore > 0
        && (deliveredScore >= outForDeliveryScore
        || deliveredScore >= outForDeliveryScore - 1)) {
      return "DELIVERED";
    }
    if (outForDeliveryScore > 0 && outForDeliveryScore >= inTransitScore) {
      return "OUT_FOR_DELIVERY";
    }
    if (inTransitScore > 0 && inTransitScore >= shippedScore) {
      return "IN_TRANSIT";
    }
    if (shippedScore > 0) {
      return "SHIPPED";
    }
    return null;
  }

  private int scoreStatus(String normalized, Set<String> phrases) {
    int score = 0;
    for (String phrase : phrases) {
      if (normalized.contains(phrase)) {
        score += Math.max(1, phrase.length() / 8);
      }
    }
    return score;
  }

  private boolean looksLikeDate(String normalized) {
    if (!NUMERIC_PATTERN.matcher(normalized).matches()) {
      return false;
    }
    if (normalized.length() == 8) {
      int firstFour = parseInt(normalized.substring(0, 4));
      int monthA = parseInt(normalized.substring(4, 6));
      int dayA = parseInt(normalized.substring(6, 8));
      int dayB = parseInt(normalized.substring(0, 2));
      int monthB = parseInt(normalized.substring(2, 4));
      int yearB = parseInt(normalized.substring(4, 8));
      return isDateParts(firstFour, monthA, dayA) || isDateParts(yearB, monthB, dayB);
    }
    return false;
  }

  private boolean looksLikePhone(String normalized, String context) {
    if (!NUMERIC_PATTERN.matcher(normalized).matches()) {
      return false;
    }
    if (containsAny(context, Set.of("telephone", "tel", "phone", "mobile", "whatsapp"))) {
      return normalized.length() >= 9 && normalized.length() <= 15;
    }
    return normalized.length() == 10 && normalized.startsWith("0");
  }

  private boolean looksLikeAmountContext(String context, String normalized) {
    if (!NUMERIC_PATTERN.matcher(normalized).matches()) {
      return false;
    }
    return containsAny(context, Set.of("eur", "euro", "usd", "total", "montant", "prix", "amount", "paid", "ttc", "ht"));
  }

  private boolean hasBalancedTrackingSignal(String normalized) {
    int digitCount = countMatches(DIGIT_PATTERN, normalized);
    if (digitCount < 4) {
      return false;
    }
    if (NUMERIC_PATTERN.matcher(normalized).matches()) {
      return normalized.length() >= 10 || TrackingCarrierRules.matchesSupportedCarrierFormat(normalized);
    }
    boolean knownCarrierFormat = matchesKnownCarrierFormat(normalized);
    int letterCount = countMatches(LETTER_PATTERN, normalized);
    if (!knownCarrierFormat && digitCount < 8) {
      return false;
    }
    if (!knownCarrierFormat && letterCount > digitCount) {
      return false;
    }
    return letterCount <= Math.max(8, digitCount + 2);
  }

  private boolean matchesKnownCarrierFormat(String normalized) {
    if (TrackingCarrierRules.matchesSupportedCarrierFormat(normalized)
        || UPS_PATTERN.matcher(normalized).matches()
        || AMAZON_PATTERN.matcher(normalized).matches()
        || UPU_PATTERN.matcher(normalized).matches()
        || LA_POSTE_2C_PATTERN.matcher(normalized).matches()
        || Pattern.compile("^YT\\d{12,18}$").matcher(normalized).matches()) {
      return true;
    }
    return RULES.stream().anyMatch(rule -> rule.matchesFormat(normalized));
  }

  private boolean isDateParts(int year, int month, int day) {
    return year >= 2000 && year <= 2100 && month >= 1 && month <= 12 && day >= 1 && day <= 31;
  }

  private int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

  private String decodeUrl(String value) {
    if (value == null) {
      return "";
    }
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return value;
    }
  }

  private boolean containsAny(String value, Set<String> keywords) {
    String text = value == null ? "" : value;
    return keywords.stream().anyMatch(text::contains);
  }

  private int countMatches(Pattern pattern, String value) {
    Matcher matcher = pattern.matcher(safe(value));
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  private String normalizeText(String value) {
    String lowercase = safe(value).toLowerCase(Locale.ROOT);
    return Normalizer.normalize(lowercase, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replace('`', '\'');
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }

  public record TrackingDetectionResult(
      List<TrackingCandidate> autoImportCandidates,
      List<TrackingCandidate> reviewCandidates,
      int rejectedCount,
      int duplicateCount
  ) {
  }

  public record TrackingCandidate(
      String carrierSlug,
      String trackingNumber,
      String normalizedTrackingNumber,
      int confidenceScore,
      TrackingConfidence confidenceLevel,
      String trackingUrl,
      String contextSnippet,
      String merchantName,
      String rawStatus,
      String reason
  ) {
    public TrackingCandidate(String carrierSlug, String trackingNumber, String normalizedTrackingNumber) {
      this(carrierSlug, trackingNumber, normalizedTrackingNumber, 100, TrackingConfidence.HIGH,
          null, null, null, null, "manual or legacy candidate");
    }
  }

  public enum TrackingConfidence {
    HIGH,
    MEDIUM,
    LOW
  }

  private enum CandidateSource {
    VISIBLE,
    LINK
  }

  private record Occurrence(String raw, int start, int end, CandidateSource source, String link) {
  }

  private record CandidateScore(int value, String carrierSlug, boolean rejected, List<String> reasons) {
    static CandidateScore rejected(String reason) {
      return new CandidateScore(0, null, true, List.of(reason));
    }
  }

  private record FormatScore(int value, String reason) {
  }

  private record PostalCodeScore(String postalCode, int score) {
  }

  private record CarrierRule(
      String slug,
      String name,
      Set<String> domains,
      Set<String> aliases,
      List<Pattern> normalizedPatterns,
      int formatBoost,
      boolean requiresCarrierContext
  ) {
    boolean hasSignal(String normalizedText) {
      String text = normalizedText == null ? "" : normalizedText;
      return domains.stream().anyMatch(text::contains) || aliases.stream().anyMatch(text::contains);
    }

    boolean matchesFormat(String normalizedTrackingNumber) {
      return normalizedPatterns.stream().anyMatch(pattern -> pattern.matcher(normalizedTrackingNumber).matches());
    }
  }
}
