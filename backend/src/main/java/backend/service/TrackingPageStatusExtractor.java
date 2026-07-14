package backend.service;

import backend.entity.ParcelStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

final class TrackingPageStatusExtractor {

  private static final int MIN_CONFIDENCE_SCORE = 74;
  private static final int DELIVERED_OVERRIDE_SCORE = 80;
  private static final String ACTIVE_STEP_SELECTOR = String.join(",",
      ".ch-suivi-colis-light-info.active",
      ".ch-suivi-colis-light-info.current",
      "[class*=step].active",
      "[class*=step].current",
      "[id*=step].active",
      "[id*=step].current",
      "[aria-current=step]",
      "[aria-current=true]",
      "[aria-selected=true]"
  );
  private static final String COMPLETED_STEP_SELECTOR = String.join(",",
      ".ch-suivi-colis-light-info.done",
      ".ch-suivi-colis-light-info.complete",
      ".ch-suivi-colis-light-info.completed",
      "[class*=step].done",
      "[class*=step].checked",
      "[class*=step].complete",
      "[class*=step].completed",
      "[id*=step].done",
      "[id*=step].checked",
      "[id*=step].complete",
      "[id*=step].completed"
  );

  private TrackingPageStatusExtractor() {
  }

  static Optional<ExtractedStatus> extractStatus(String html) {
    if (html == null || html.isBlank()) {
      return Optional.empty();
    }

    Document document = Jsoup.parse(html);
    List<Match> matches = new ArrayList<>();
    Match bestMatch = null;

    for (Match progressMatch : collectProgressMatches(document)) {
      matches.add(progressMatch);
      if (bestMatch == null || progressMatch.score() > bestMatch.score()) {
        bestMatch = progressMatch;
      }
    }

    List<Snippet> snippets = collectSnippets(document);
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
        || bestMatch.status() != ParcelStatus.IN_TRANSIT
        || deliveredMatch.score() >= bestMatch.score() - 8)) {
      return deliveredMatch;
    }
    return bestMatch;
  }

  private static List<Match> collectProgressMatches(Document document) {
    LinkedHashSet<Match> matches = new LinkedHashSet<>();
    for (Element element : document.select(ACTIVE_STEP_SELECTOR)) {
      Match match = matchProgressElement(element, 28);
      if (match != null) {
        matches.add(match);
      }
    }
    for (Element element : document.select(COMPLETED_STEP_SELECTOR)) {
      Match match = matchProgressElement(element, 20);
      if (match != null) {
        matches.add(match);
      }
    }
    return new ArrayList<>(matches);
  }

  private static Match matchProgressElement(Element element, int sourceBoost) {
    if (element == null) {
      return null;
    }
    Match best = null;
    for (String candidate : progressTextCandidates(element)) {
      Optional<CarrierStatusResolver.StatusMatch> resolved = CarrierStatusResolver.best(candidate);
      if (resolved.isEmpty()) {
        continue;
      }
      CarrierStatusResolver.StatusMatch statusMatch = resolved.get();
      Match match = new Match(
          statusMatch.status(),
          cleanLabel(candidate),
          statusMatch.score() + sourceBoost
      );
      if (best == null || match.score() > best.score()) {
        best = match;
      }
    }
    return best;
  }

  private static List<String> progressTextCandidates(Element element) {
    LinkedHashSet<String> values = new LinkedHashSet<>();
    addProgressValue(values, element.ownText());
    addProgressValue(values, textOf(element.selectFirst(".ch-suivi-colis-light-text")));
    for (Element child : element.select("[class*=text],[class*=label],[class*=title],span,p,div,strong")) {
      addProgressValue(values, textOf(child));
      if (values.size() >= 10) {
        break;
      }
    }
    addProgressValue(values, element.attr("aria-label"));
    addProgressValue(values, element.attr("title"));
    addProgressValue(values, element.text());
    return new ArrayList<>(values);
  }

  private static void addProgressValue(Set<String> values, String raw) {
    String value = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
    if (value.length() < 4 || value.length() > 120) {
      return;
    }
    values.add(value);
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
      addAttributeSnippets(snippets, element, 8);
      if (snippets.size() >= 24) {
        break;
      }
    }

    for (Element element : document.select("[class*=step],[id*=step],[class*=event],[id*=event],p,li")) {
      addSnippet(snippets, element.text(), 0);
      addAttributeSnippets(snippets, element, 2);
      if (snippets.size() >= 40) {
        break;
      }
    }

    for (Element element : document.select("script[type=application/ld+json],script[type=application/json],script:not([src])")) {
      addSnippet(snippets, compactScript(element.data()), 4);
      if (snippets.size() >= 56) {
        break;
      }
    }

    return new ArrayList<>(snippets);
  }

  private static void addAttributeSnippets(Set<Snippet> snippets, Element element, int sourceBoost) {
    if (element == null) {
      return;
    }
    for (String attributeName : List.of(
        "data-status",
        "data-state",
        "data-current-status",
        "data-delivery-status",
        "aria-label",
        "title"
    )) {
      addSnippet(snippets, element.attr(attributeName), sourceBoost);
    }
  }

  private static void addSnippet(Set<Snippet> snippets, String raw, int sourceBoost) {
    String value = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
    if (value.length() < 8 || value.length() > 360) {
      return;
    }
    snippets.add(new Snippet(value, sourceBoost));
  }

  private static String compactScript(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String compact = raw.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim();
    if (compact.length() <= 360) {
      return compact;
    }
    return compact.substring(0, 360);
  }

  private static Match matchSnippet(Snippet snippet) {
    return CarrierStatusResolver.best(snippet.text())
        .map(match -> new Match(
            match.status(),
            cleanLabel(snippet.text()),
            match.score() + Math.min(10, match.phrase().length() / 6) + snippet.sourceBoost()
        ))
        .orElse(null);
  }

  private static String cleanLabel(String snippet) {
    String compact = snippet.replaceAll("\\s+", " ").trim();
    if (compact.length() <= 140) {
      return compact;
    }
    return compact.substring(0, 137).trim() + "...";
  }

  private static String textOf(Element element) {
    if (element == null) {
      return null;
    }
    String text = element.text();
    return text == null || text.isBlank() ? null : text.trim();
  }

  record ExtractedStatus(ParcelStatus status, String label) {
  }

  private record Match(ParcelStatus status, String label, int score) {
  }

  private record Snippet(String text, int sourceBoost) {
  }
}
