package backend.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class TrackingParserService {

  public static final int AUTO_IMPORT_THRESHOLD = 75;
  public static final int REVIEW_THRESHOLD = 55;

  private static final int CONTEXT_RADIUS = 120;
  private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>()\"']+", Pattern.CASE_INSENSITIVE);
  private static final Pattern COLISSIMO_UPU_INLINE =
      Pattern.compile("\\b(?!X[A-Z])[A-Z]{2}[\\s\\-]?\\d{9}[\\s\\-]?FR\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern LA_POSTE_2C_INLINE =
      Pattern.compile("\\b\\d[A-Z][\\s\\-]?\\d{11}\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern COLISSIMO_15_CHARS_INLINE =
      Pattern.compile("\\b\\d{14}[A-Z]\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern COLISSIMO_NUMERIC_INLINE =
      Pattern.compile("\\b\\d(?:[\\d\\s\\-]{11,16})\\d\\b");
  private static final Pattern CHRONOPOST_UPU_INLINE =
      Pattern.compile("\\b[A-Z]{2}[\\s\\-]?\\d{9}[\\s\\-]?[A-Z]{2}\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern CHRONOPOST_15_CHARS_INLINE =
      Pattern.compile("\\b\\d{14}[A-Z]\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");

  private static final Set<String> COLISSIMO_SIGNAL_KEYWORDS = Set.of(
      "colissimo",
      "la poste",
      "laposte",
      "notif-colissimo-laposte",
      "suivi.laposte.fr"
  );

  private static final Set<String> COLISSIMO_DELIVERY_KEYWORDS = Set.of(
      "suivi",
      "suivi de livraison",
      "livraison suivi",
      "colis",
      "livraison",
      "numero de suivi",
      "votre colis",
      "courrier suivi",
      "suivre votre colis"
  );

  private static final Set<String> CHRONOPOST_SIGNAL_KEYWORDS = Set.of(
      "chronopost",
      "mychronopost",
      "predict",
      "chronopost.fr"
  );

  private static final Set<String> CHRONOPOST_DELIVERY_KEYWORDS = Set.of(
      "suivi",
      "suivi colis",
      "suivi de livraison",
      "colis",
      "livraison",
      "numero de colis",
      "numero de suivi",
      "votre colis",
      "point de proximite"
  );

  private static final Set<String> STRONG_TRACKING_CONTEXT = Set.of(
      "numero de suivi",
      "n de suivi",
      "no de suivi",
      "suivi colissimo",
      "suivi de livraison",
      "suivre votre colis",
      "votre colis",
      "code de suivi",
      "tracking number"
  );

  private static final Set<String> NEGATIVE_CONTEXT = Set.of(
      "numero de commande",
      "commande",
      "order number",
      "order id",
      "invoice",
      "facture",
      "paiement",
      "payment",
      "telephone",
      "mobile"
  );

  private static final Set<String> PICKUP_READY_KEYWORDS = Set.of(
      "disponible en relais",
      "disponible au relais",
      "disponible en point relais",
      "disponible au point relais",
      "arrive en relais",
      "point relais",
      "point de retrait",
      "relais pickup",
      "pickup pass",
      "code de retrait",
      "a retirer",
      "retrait en relais",
      "vous attend dans votre point de retrait"
  );

  private static final Set<String> DELIVERED_STATUS_KEYWORDS = Set.of(
      "a ete livre",
      "colis a ete livre",
      "est livre dans votre boite",
      "livre dans votre boite",
      "livraison effectuee",
      "livre",
      "boite aux lettres",
      "retire par le destinataire",
      "colis retire",
      "a ete distribue",
      "remis au destinataire"
  );

  private static final Set<String> OUT_FOR_DELIVERY_KEYWORDS = Set.of(
      "en cours de livraison",
      "envoi en cours de livraison",
      "livraison ce jour",
      "livraison aujourd hui",
      "sera livre aujourd hui",
      "depart en livraison"
  );

  private static final Set<String> IN_TRANSIT_STATUS_KEYWORDS = Set.of(
      "en transit",
      "pris en charge",
      "pris en charge par chronopost",
      "en preparation chez l expediteur",
      "en cours d acheminement",
      "acheminement",
      "expedie",
      "site de distribution",
      "site de tri",
      "plateformes logistiques",
      "arrive"
  );

  public boolean looksLikeCarrierMessage(String from, String subject) {
    return shouldInspectMessage(from, subject, "");
  }

  public boolean shouldInspectMessage(String from, String subject) {
    return shouldInspectMessage(from, subject, "");
  }

  public boolean shouldInspectMessage(String from, String subject, String body) {
    return shouldInspectColissimoMessage(from, subject, body)
        || shouldInspectChronopostMessage(from, subject, body);
  }

  public boolean shouldInspectColissimoMessage(String from, String subject, String body) {
    String haystack = normalizeText(String.join(" ", safe(from), safe(subject), safe(body)));
    if (haystack.isBlank()) {
      return false;
    }
    return containsAny(haystack, COLISSIMO_SIGNAL_KEYWORDS) && containsAny(haystack, COLISSIMO_DELIVERY_KEYWORDS);
  }

  public boolean shouldInspectChronopostMessage(String from, String subject, String body) {
    String haystack = normalizeText(String.join(" ", safe(from), safe(subject), safe(body)));
    if (haystack.isBlank()) {
      return false;
    }
    return containsAny(haystack, CHRONOPOST_SIGNAL_KEYWORDS) && containsAny(haystack, CHRONOPOST_DELIVERY_KEYWORDS);
  }

  public List<TrackingCandidate> parse(String from, String subject, String body) {
    TrackingDetectionResult detection = detect(from, subject, body, extractLinks(body));
    return detection.autoImportCandidates();
  }

  public TrackingDetectionResult detect(String from, String subject, String body) {
    return detect(from, subject, body, extractLinks(body));
  }

  public TrackingDetectionResult detect(String from, String subject, String body, List<String> links) {
    TrackingDetectionResult colissimo = detectColissimo(from, subject, body, links);
    TrackingDetectionResult chronopost = detectChronopost(from, subject, body, links);
    return mergeDetectionResults(colissimo, chronopost);
  }

  public TrackingDetectionResult detectColissimo(String from, String subject, String body) {
    return detectColissimo(from, subject, body, extractLinks(body));
  }

  public TrackingDetectionResult detectColissimo(String from, String subject, String body, List<String> links) {
    if (!shouldInspectColissimoMessage(from, subject, body)) {
      return new TrackingDetectionResult(List.of(), List.of(), 0, 0);
    }

    String visibleText = safe(body);
    List<String> safeLinks = links == null ? List.of() : links.stream()
        .filter(link -> link != null && !link.isBlank())
        .map(String::trim)
        .distinct()
        .toList();

    Map<String, TrackingCandidate> bestByTrackingNumber = new LinkedHashMap<>();
    int rejectedCount = 0;
    int duplicateCount = 0;

    for (Occurrence occurrence : extractOccurrences(visibleText, safeLinks)) {
      String normalized = normalizeTrackingNumber(occurrence.raw());
      if (!TrackingCarrierRules.isValidForCarrier(normalized, "colissimo")) {
        rejectedCount++;
        continue;
      }
      if (looksLikeDate(normalized)) {
        rejectedCount++;
        continue;
      }

      String context = occurrence.source() == CandidateSource.VISIBLE
          ? contextSnippet(visibleText, occurrence.start(), occurrence.end(), CONTEXT_RADIUS)
          : decodeUrl(occurrence.link());
      String normalizedContext = normalizeText(context);
      int score = scoreCandidate("colissimo", normalized, normalizedContext, normalizeText(subject), safeLinks, occurrence);
      if (score < REVIEW_THRESHOLD) {
        rejectedCount++;
        continue;
      }

      TrackingCandidate candidate = new TrackingCandidate(
          "colissimo",
          occurrence.raw().trim(),
          normalized,
          score,
          confidence(score),
          bestTrackingUrl("colissimo", normalized, safeLinks),
          context,
          displayName(from),
          rawStatus(visibleText),
          "colissimo"
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

    List<TrackingCandidate> autoImport = bestByTrackingNumber.values().stream()
        .filter(candidate -> candidate.confidenceLevel() == TrackingConfidence.HIGH)
        .sorted(Comparator.comparing(TrackingCandidate::confidenceScore).reversed())
        .toList();
    List<TrackingCandidate> review = bestByTrackingNumber.values().stream()
        .filter(candidate -> candidate.confidenceLevel() == TrackingConfidence.MEDIUM)
        .sorted(Comparator.comparing(TrackingCandidate::confidenceScore).reversed())
        .toList();

    return new TrackingDetectionResult(autoImport, review, rejectedCount, duplicateCount);
  }

  public TrackingDetectionResult detectChronopost(String from, String subject, String body) {
    return detectChronopost(from, subject, body, extractLinks(body));
  }

  public TrackingDetectionResult detectChronopost(String from, String subject, String body, List<String> links) {
    if (!shouldInspectChronopostMessage(from, subject, body)) {
      return new TrackingDetectionResult(List.of(), List.of(), 0, 0);
    }

    String visibleText = safe(body);
    List<String> safeLinks = links == null ? List.of() : links.stream()
        .filter(link -> link != null && !link.isBlank())
        .map(String::trim)
        .distinct()
        .toList();

    Map<String, TrackingCandidate> bestByTrackingNumber = new LinkedHashMap<>();
    int rejectedCount = 0;
    int duplicateCount = 0;

    for (Occurrence occurrence : extractChronopostOccurrences(visibleText, safeLinks)) {
      String normalized = normalizeTrackingNumber(occurrence.raw());
      if (!TrackingCarrierRules.isValidForCarrier(normalized, "chronopost")) {
        rejectedCount++;
        continue;
      }
      if (looksLikeDate(normalized)) {
        rejectedCount++;
        continue;
      }

      String context = occurrence.source() == CandidateSource.VISIBLE
          ? contextSnippet(visibleText, occurrence.start(), occurrence.end(), CONTEXT_RADIUS)
          : decodeUrl(occurrence.link());
      String normalizedContext = normalizeText(context);
      int score = scoreCandidate("chronopost", normalized, normalizedContext, normalizeText(subject), safeLinks, occurrence);
      if (score < REVIEW_THRESHOLD) {
        rejectedCount++;
        continue;
      }

      TrackingCandidate candidate = new TrackingCandidate(
          "chronopost",
          occurrence.raw().trim(),
          normalized,
          score,
          confidence(score),
          bestTrackingUrl("chronopost", normalized, safeLinks),
          context,
          displayName(from),
          rawStatus(visibleText),
          "chronopost"
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

    List<TrackingCandidate> autoImport = bestByTrackingNumber.values().stream()
        .filter(candidate -> candidate.confidenceLevel() == TrackingConfidence.HIGH)
        .sorted(Comparator.comparing(TrackingCandidate::confidenceScore).reversed())
        .toList();
    List<TrackingCandidate> review = bestByTrackingNumber.values().stream()
        .filter(candidate -> candidate.confidenceLevel() == TrackingConfidence.MEDIUM)
        .sorted(Comparator.comparing(TrackingCandidate::confidenceScore).reversed())
        .toList();

    return new TrackingDetectionResult(autoImport, review, rejectedCount, duplicateCount);
  }

  public String normalizeTrackingNumber(String value) {
    return safe(value)
        .replaceAll("[\\s\\-_.]", "")
        .toUpperCase(Locale.ROOT);
  }

  public boolean isValidTrackingNumber(String value) {
    String normalized = normalizeTrackingNumber(value);
    return TrackingCarrierRules.matchesSupportedCarrierFormat(normalized) && !looksLikeDate(normalized);
  }

  public String inferCarrierSlug(String trackingNumber) {
    String normalized = normalizeTrackingNumber(trackingNumber);
    return looksLikeDate(normalized) ? null : TrackingCarrierRules.inferSupportedCarrier(normalized);
  }

  private int scoreCandidate(
      String carrierSlug,
      String normalizedTracking,
      String normalizedContext,
      String normalizedSubject,
      List<String> links,
      Occurrence occurrence
  ) {
    int score = 45;
    if (!NUMERIC_PATTERN.matcher(normalizedTracking).matches()) {
      score += 15;
    }
    if (containsAny(normalizedContext, STRONG_TRACKING_CONTEXT)) {
      score += 25;
    } else if (containsAny(normalizedSubject, STRONG_TRACKING_CONTEXT)) {
      score += 15;
    }
    if (linkContainsTracking(carrierSlug, links, normalizedTracking)) {
      score += 20;
    }
    if (occurrence.source() == CandidateSource.LINK) {
      score += 10;
    }
    if (NUMERIC_PATTERN.matcher(normalizedTracking).matches() && !containsAny(normalizedContext, STRONG_TRACKING_CONTEXT)) {
      score -= 20;
    }
    if (containsAny(normalizedContext, NEGATIVE_CONTEXT) && !containsAny(normalizedContext, STRONG_TRACKING_CONTEXT)) {
      score -= 25;
    }
    return Math.max(0, Math.min(100, score));
  }

  private List<Occurrence> extractOccurrences(String visibleText, List<String> links) {
    List<Occurrence> occurrences = new ArrayList<>();
    addOccurrences(occurrences, visibleText, COLISSIMO_UPU_INLINE);
    addOccurrences(occurrences, visibleText, LA_POSTE_2C_INLINE);
    addOccurrences(occurrences, visibleText, COLISSIMO_15_CHARS_INLINE);
    addOccurrences(occurrences, visibleText, COLISSIMO_NUMERIC_INLINE);

    for (String link : links) {
      if (!TrackingLinkResolver.isTrustedTrackingUrl(link, "colissimo")) {
        continue;
      }
      String decoded = decodeUrl(link);
      addLinkOccurrences(occurrences, decoded, link, COLISSIMO_UPU_INLINE);
      addLinkOccurrences(occurrences, decoded, link, LA_POSTE_2C_INLINE);
      addLinkOccurrences(occurrences, decoded, link, COLISSIMO_15_CHARS_INLINE);
      addLinkOccurrences(occurrences, decoded, link, COLISSIMO_NUMERIC_INLINE);
    }
    return occurrences;
  }

  private List<Occurrence> extractChronopostOccurrences(String visibleText, List<String> links) {
    List<Occurrence> occurrences = new ArrayList<>();
    addOccurrences(occurrences, visibleText, CHRONOPOST_UPU_INLINE);
    addOccurrences(occurrences, visibleText, CHRONOPOST_15_CHARS_INLINE);

    for (String link : links) {
      if (!TrackingLinkResolver.isTrustedTrackingUrl(link, "chronopost")) {
        continue;
      }
      String decoded = decodeUrl(link);
      addLinkOccurrences(occurrences, decoded, link, CHRONOPOST_UPU_INLINE);
      addLinkOccurrences(occurrences, decoded, link, CHRONOPOST_15_CHARS_INLINE);
    }
    return occurrences;
  }

  private void addOccurrences(List<Occurrence> occurrences, String text, Pattern pattern) {
    Matcher matcher = pattern.matcher(safe(text));
    while (matcher.find()) {
      occurrences.add(new Occurrence(matcher.group(), matcher.start(), matcher.end(), CandidateSource.VISIBLE, null));
    }
  }

  private void addLinkOccurrences(List<Occurrence> occurrences, String decodedLink, String rawLink, Pattern pattern) {
    Matcher matcher = pattern.matcher(decodedLink);
    while (matcher.find()) {
      occurrences.add(new Occurrence(matcher.group(), -1, -1, CandidateSource.LINK, rawLink));
    }
  }

  private List<String> extractLinks(String body) {
    Matcher matcher = URL_PATTERN.matcher(safe(body));
    List<String> links = new ArrayList<>();
    while (matcher.find()) {
      links.add(matcher.group());
    }
    return links;
  }

  private boolean linkContainsTracking(List<String> links, String normalizedTracking) {
    return linkContainsTracking("colissimo", links, normalizedTracking);
  }

  private boolean linkContainsTracking(String carrierSlug, List<String> links, String normalizedTracking) {
    for (String link : links) {
      if (!TrackingLinkResolver.isTrustedTrackingUrl(link, carrierSlug)) {
        continue;
      }
      if (normalizeTrackingNumber(decodeUrl(link)).contains(normalizedTracking)) {
        return true;
      }
    }
    return false;
  }

  private String bestTrackingUrl(String normalizedTracking, List<String> links) {
    return bestTrackingUrl("colissimo", normalizedTracking, links);
  }

  private String bestTrackingUrl(String carrierSlug, String normalizedTracking, List<String> links) {
    for (String link : links) {
      if (TrackingLinkResolver.isTrustedTrackingUrl(link, carrierSlug)
          && normalizeTrackingNumber(decodeUrl(link)).contains(normalizedTracking)) {
        return link.trim();
      }
    }
    return TrackingLinkResolver.fallbackTrackingUrl(carrierSlug, normalizedTracking);
  }

  private String contextSnippet(String visibleText, int start, int end, int radius) {
    if (start < 0 || end < 0) {
      return null;
    }
    int from = Math.max(0, start - radius);
    int to = Math.min(safe(visibleText).length(), end + radius);
    String snippet = safe(visibleText).substring(from, to).replaceAll("\\s+", " ").trim();
    return snippet.isEmpty() ? null : snippet;
  }

  private String displayName(String from) {
    String value = safe(from).trim();
    int emailStart = value.indexOf('<');
    if (emailStart > 0) {
      value = value.substring(0, emailStart).trim();
    }
    value = value.replace("\"", "").trim();
    return value.isBlank() ? null : value;
  }

  private String rawStatus(String body) {
    String normalized = normalizeText(body);
    if (containsAny(normalized, PICKUP_READY_KEYWORDS)) {
      return "AVAILABLE_FOR_PICKUP";
    }
    if (containsAny(normalized, DELIVERED_STATUS_KEYWORDS)) {
      return "DELIVERED";
    }
    if (containsAny(normalized, OUT_FOR_DELIVERY_KEYWORDS)) {
      return "OUT_FOR_DELIVERY";
    }
    if (containsAny(normalized, IN_TRANSIT_STATUS_KEYWORDS)) {
      return "IN_TRANSIT";
    }
    return null;
  }

  private TrackingDetectionResult mergeDetectionResults(TrackingDetectionResult first, TrackingDetectionResult second) {
    List<TrackingCandidate> autoImport = new ArrayList<>();
    List<TrackingCandidate> review = new ArrayList<>();
    if (first != null) {
      autoImport.addAll(first.autoImportCandidates());
      review.addAll(first.reviewCandidates());
    }
    if (second != null) {
      autoImport.addAll(second.autoImportCandidates());
      review.addAll(second.reviewCandidates());
    }
    autoImport.sort(Comparator.comparing(TrackingCandidate::confidenceScore).reversed());
    review.sort(Comparator.comparing(TrackingCandidate::confidenceScore).reversed());
    int rejectedCount = (first == null ? 0 : first.rejectedCount()) + (second == null ? 0 : second.rejectedCount());
    int duplicateCount = (first == null ? 0 : first.duplicateCount()) + (second == null ? 0 : second.duplicateCount());
    return new TrackingDetectionResult(autoImport, review, rejectedCount, duplicateCount);
  }

  private boolean looksLikeDate(String normalized) {
    if (!NUMERIC_PATTERN.matcher(normalized).matches() || normalized.length() != 8) {
      return false;
    }
    int yyyy = parseInt(normalized.substring(0, 4));
    int mm = parseInt(normalized.substring(4, 6));
    int dd = parseInt(normalized.substring(6, 8));
    int ddAlt = parseInt(normalized.substring(0, 2));
    int mmAlt = parseInt(normalized.substring(2, 4));
    int yyyyAlt = parseInt(normalized.substring(4, 8));
    return isDateParts(yyyy, mm, dd) || isDateParts(yyyyAlt, mmAlt, ddAlt);
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

  private TrackingConfidence confidence(int score) {
    if (score >= AUTO_IMPORT_THRESHOLD) {
      return TrackingConfidence.HIGH;
    }
    if (score >= REVIEW_THRESHOLD) {
      return TrackingConfidence.MEDIUM;
    }
    return TrackingConfidence.LOW;
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
}
