package backend.service;

import backend.entity.ParcelStatus;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

final class TrackingPageStatusExtractor {

  private static final int MIN_CONFIDENCE_SCORE = 74;
  private static final int DELIVERED_OVERRIDE_SCORE = 80;

  private static final List<StatusRule> STATUS_RULES = List.of(
      new StatusRule(
          ParcelStatus.DELIVERED,
          List.of(
              new Phrase("votre colis a ete livre", 98),
              new Phrase("colis a ete livre", 96),
              new Phrase("remis au destinataire", 95),
              new Phrase("livraison effectuee", 95),
              new Phrase("livraison terminee", 94),
              new Phrase("delivery completed", 92),
              new Phrase("successfully delivered", 92),
              new Phrase("parcel delivered", 90),
              new Phrase("a ete distribue", 90),
              new Phrase("remis dans votre boite aux lettres", 89),
              new Phrase("colis livre", 88),
              new Phrase("delivered", 80),
              new Phrase("livre", 74),
              new Phrase("distribue", 72)
          )
      ),
      new StatusRule(
          ParcelStatus.OUT_FOR_DELIVERY,
          List.of(
              new Phrase("en cours de livraison", 90),
              new Phrase("out for delivery", 90),
              new Phrase("livraison ce jour", 86),
              new Phrase("available for pickup", 82),
              new Phrase("disponible au point relais", 82),
              new Phrase("disponible en point relais", 82),
              new Phrase("en livraison", 74)
          )
      ),
      new StatusRule(
          ParcelStatus.EXCEPTION,
          List.of(
              new Phrase("delivery exception", 92),
              new Phrase("echec de livraison", 90),
              new Phrase("delivery failed", 90),
              new Phrase("retour expediteur", 88),
              new Phrase("returned to sender", 88),
              new Phrase("adresse incomplete", 86),
              new Phrase("incident de livraison", 84),
              new Phrase("incident", 74),
              new Phrase("retard", 70)
          )
      ),
      new StatusRule(
          ParcelStatus.IN_TRANSIT,
          List.of(
              new Phrase("shipment in transit", 84),
              new Phrase("colis en transit", 82),
              new Phrase("in transit", 80),
              new Phrase("on the way", 76),
              new Phrase("pris en charge", 76),
              new Phrase("arrive en agence", 74),
              new Phrase("arrived at sorting center", 74),
              new Phrase("achemine", 72),
              new Phrase("en transit", 70)
          )
      ),
      new StatusRule(
          ParcelStatus.REGISTERED,
          List.of(
              new Phrase("shipment information received", 82),
              new Phrase("etiquette creee", 80),
              new Phrase("label created", 80),
              new Phrase("pre-advice", 78),
              new Phrase("annonce au transporteur", 76),
              new Phrase("preparation de votre colis", 76)
          )
      )
  );

  private TrackingPageStatusExtractor() {
  }

  static Optional<ExtractedStatus> extractStatus(String html) {
    if (html == null || html.isBlank()) {
      return Optional.empty();
    }

    Document document = Jsoup.parse(html);
    List<Snippet> snippets = collectSnippets(document);
    List<Match> matches = new ArrayList<>();
    Match bestMatch = null;
    for (Snippet snippet : snippets) {
      Match match = matchSnippet(snippet);
      if (match == null) {
        continue;
      }
      matches.add(match);
      if (bestMatch == null || match.score() > bestMatch.score()) {
        bestMatch = match;
      }
    }

    Match selectedMatch = selectBestMatch(matches, bestMatch);
    if (selectedMatch == null || selectedMatch.score() < MIN_CONFIDENCE_SCORE) {
      return Optional.empty();
    }
    return Optional.of(new ExtractedStatus(selectedMatch.status(), selectedMatch.label()));
  }

  private static Match selectBestMatch(List<Match> matches, Match bestMatch) {
    if (matches.isEmpty()) {
      return null;
    }
    Match deliveredMatch = matches.stream()
        .filter(match -> match.status() == ParcelStatus.DELIVERED)
        .max(java.util.Comparator.comparingInt(Match::score))
        .orElse(null);
    if (deliveredMatch != null
        && (deliveredMatch.score() >= DELIVERED_OVERRIDE_SCORE
        || bestMatch == null
        || bestMatch.status() != ParcelStatus.OUT_FOR_DELIVERY
        || deliveredMatch.score() >= bestMatch.score() - 8)) {
      return deliveredMatch;
    }
    return bestMatch;
  }

  private static List<Snippet> collectSnippets(Document document) {
    LinkedHashSet<Snippet> snippets = new LinkedHashSet<>();
    addSnippet(snippets, document.title(), 18);

    for (Element meta : document.select("meta[name=description],meta[property=og:description],meta[name=twitter:description]")) {
      addSnippet(snippets, meta.attr("content"), 14);
    }

    for (Element element : document.select("h1,h2,h3,h4,[class*=status],[id*=status],[class*=etat],[id*=etat],"
        + "[class*=state],[id*=state],[class*=current],[id*=current],[class*=active],[id*=active]")) {
      addSnippet(snippets, element.text(), 10);
      if (snippets.size() >= 24) {
        break;
      }
    }

    for (Element element : document.select("[class*=step],[id*=step],[class*=event],[id*=event],p,li")) {
      addSnippet(snippets, element.text(), 0);
      if (snippets.size() >= 40) {
        break;
      }
    }

    return new ArrayList<>(snippets);
  }

  private static void addSnippet(Set<Snippet> snippets, String raw, int sourceBoost) {
    String value = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
    if (value.length() < 8 || value.length() > 220) {
      return;
    }
    snippets.add(new Snippet(value, sourceBoost));
  }

  private static Match matchSnippet(Snippet snippet) {
    String normalized = normalize(snippet.text());
    Match best = null;
    for (StatusRule rule : STATUS_RULES) {
      for (Phrase phrase : rule.phrases()) {
        if (!normalized.contains(phrase.value())) {
          continue;
        }
        int score = phrase.score() + Math.min(10, phrase.value().length() / 6) + snippet.sourceBoost();
        Match candidate = new Match(rule.status(), cleanLabel(snippet.text()), score);
        if (best == null || candidate.score() > best.score()) {
          best = candidate;
        }
      }
    }
    return best;
  }

  private static String cleanLabel(String snippet) {
    String compact = snippet.replaceAll("\\s+", " ").trim();
    if (compact.length() <= 140) {
      return compact;
    }
    return compact.substring(0, 137).trim() + "...";
  }

  private static String normalize(String value) {
    String lowercase = value == null ? "" : value.toLowerCase(Locale.ROOT);
    return Normalizer.normalize(lowercase, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
  }

  record ExtractedStatus(ParcelStatus status, String label) {
  }

  private record StatusRule(ParcelStatus status, List<Phrase> phrases) {
  }

  private record Phrase(String value, int score) {
  }

  private record Match(ParcelStatus status, String label, int score) {
  }

  private record Snippet(String text, int sourceBoost) {
  }
}
