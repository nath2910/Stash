package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Order(105)
public class ChronopostTrackingClient implements CarrierTrackingClient {

  private static final Logger log = LoggerFactory.getLogger(ChronopostTrackingClient.class);
  private static final String PROVIDER = "CHRONOPOST_DIRECT";
  private static final String BASE_URL = "https://www.chronopost.fr/tracking-no-cms";
  private static final long LOCAL_BROWSER_TIMEOUT_SECONDS = 20;
  private static final String BROWSER_USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
          + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";
  private static final DateTimeFormatter EVENT_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final Pattern UPU_TRACKING = Pattern.compile("^[A-Z]{2}\\d{9}[A-Z]{2}$", Pattern.CASE_INSENSITIVE);
  private static final Set<String> NON_CHRONOPOST_UPU_SUFFIXES = Set.of("FR", "CN", "US", "GB");
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public ChronopostTrackingClient() {
    this(new ObjectMapper());
  }

  public ChronopostTrackingClient(ObjectMapper objectMapper) {
    this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(Parcel parcel) {
    if (parcel == null) {
      return false;
    }
    String carrier = normalizedCarrier(parcel);
    if ("chronopost".equals(carrier)) {
      return true;
    }
    String trackingUrl = rawTrackingUrl(parcel);
    if ("chronopost".equals(TrackingLinkResolver.detectCarrierSlug(trackingUrl))) {
      return true;
    }
    String trackingNumber = normalizedTracking(parcel);
    if (!UPU_TRACKING.matcher(trackingNumber).matches()) {
      return false;
    }
    String suffix = trackingNumber.substring(trackingNumber.length() - 2).toUpperCase(Locale.ROOT);
    return !NON_CHRONOPOST_UPU_SUFFIXES.contains(suffix);
  }

  @Override
  public boolean isConfigured() {
    return true;
  }

  @Override
  public Optional<TrackingSnapshot> fetchTracking(Parcel parcel) {
    Map<String, Object> response = null;
    try {
      String ajaxBody = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/suivi-colis")
              .queryParam("listeNumerosLT", parcel.getTrackingNumber().trim())
              .queryParam("langue", "fr")
              .build())
          .header(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
          .header(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01")
          .header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
          .header(HttpHeaders.CACHE_CONTROL, "no-cache")
          .header("Pragma", "no-cache")
          .header("Origin", "https://www.chronopost.fr")
          .header("X-Requested-With", "XMLHttpRequest")
          .header(HttpHeaders.REFERER, trackingPageUrl(parcel))
          .retrieve()
          .body(String.class);
      response = parseAjaxPayload(ajaxBody);
    } catch (Exception ex) {
      log.warn("Chronopost AJAX tracking failed for parcel {} ({})",
          parcel.getId(), parcel.getTrackingNumber(), ex);
    }

    if (response == null || response.isEmpty() || response.get("error") != null) {
      log.debug("Chronopost direct AJAX payload empty for parcel {} ({}), trying cookie session fallback",
          parcel.getId(), parcel.getTrackingNumber());
      response = fetchWithCookieSession(parcel).orElse(null);
    }

    if (response == null || response.isEmpty() || response.get("error") != null) {
      log.debug("Chronopost cookie fallback empty for parcel {} ({}), trying local browser fallback",
          parcel.getId(), parcel.getTrackingNumber());
      response = fetchWithLocalBrowser(parcel).orElse(null);
    }

    if (response == null || response.isEmpty() || response.get("error") != null) {
      log.debug("Chronopost returned empty or errored payload for parcel {} ({}) after fallback",
          parcel.getId(), parcel.getTrackingNumber());
      return Optional.empty();
    }

    return Optional.of(toSnapshot(parcel, response));
  }

  static TrackingSnapshot toSnapshot(Parcel parcel, Map<String, Object> response) {
    String topHtml = stringValue(response.get("top"));
    String tabHtml = stringValue(response.get("tab"));
    String mergedHtml = String.join("\n", safe(topHtml), safe(tabHtml));
    String activeStepLabel = extractActiveStepLabel(topHtml);
    String summaryLabel = extractSummaryLabel(topHtml);

    List<TrackingEventSnapshot> events = extractEvents(tabHtml);
    TrackingEventSnapshot latest = latestEvent(events);
    ParcelStatus summaryStatus = statusFromDescription(firstNonBlank(activeStepLabel, summaryLabel, mergedHtml));
    ParcelStatus latestStatus = latest == null ? ParcelStatus.UNKNOWN : latest.status();
    ParcelStatus extractedStatus = TrackingPageStatusExtractor.extractStatus(mergedHtml)
        .map(TrackingPageStatusExtractor.ExtractedStatus::status)
        .orElse(ParcelStatus.UNKNOWN);
    ParcelStatus status = bestStatus(summaryStatus, extractedStatus, latestStatus);
    String statusLabel = firstNonBlank(
        preferredStatusLabel(status, summaryLabel, activeStepLabel),
        latest == null ? null : latest.description()
    );
    OffsetDateTime deliveredAt = status == ParcelStatus.DELIVERED ? firstDeliveredAt(events) : null;

    Map<String, Object> rawPayload = new HashMap<>();
    putIfPresent(rawPayload, "top", topHtml);
    putIfPresent(rawPayload, "tab", tabHtml);
    rawPayload.put("tracking_url", trackingPageUrl(parcel));

    return new TrackingSnapshot(
        PROVIDER,
        firstNonBlank(parcel.getNormalizedTrackingNumber(), parcel.getTrackingNumber()),
        "chronopost",
        status,
        statusLabel,
        null,
        deliveredAt,
        trackingPageUrl(parcel),
        null,
        null,
        "Chronopost",
        null,
        rawPayload,
        events
    );
  }

  static List<TrackingEventSnapshot> extractEvents(String tabHtml) {
    if (tabHtml == null || tabHtml.isBlank()) {
      return List.of();
    }
    Document document = Jsoup.parse(tabHtml);
    List<TrackingEventSnapshot> events = new ArrayList<>();
    for (Element row : document.select("tr.toggleElmt")) {
      List<Element> cells = row.select("td");
      if (cells.size() < 2) {
        continue;
      }
      String dateCell = cells.get(0).text();
      Element detailsCell = cells.get(1);
      List<String> detailLines = extractLines(detailsCell.html());
      String location = detailLines.isEmpty() ? null : detailLines.get(0);
      String description = detailLines.size() > 1 ? detailLines.get(1) : null;
      String complement = cells.size() > 2 ? String.join(" ", extractLines(cells.get(2).html())) : null;
      String fullEventText = firstNonBlank(
          joinNonBlank(" ", location, description, complement),
          detailsCell.text().trim()
      );
      OffsetDateTime eventTime = parseChronopostDateTime(dateCell);
      ParcelStatus status = statusFromDescription(fullEventText);
      Map<String, Object> rawEvent = new HashMap<>();
      putIfPresent(rawEvent, "date_cell", dateCell);
      putIfPresent(rawEvent, "location", location);
      putIfPresent(rawEvent, "description", description);
      putIfPresent(rawEvent, "complement", complement);
      events.add(new TrackingEventSnapshot(status, null, description, location, eventTime, rawEvent));
    }
    return events;
  }

  static TrackingEventSnapshot latestEvent(List<TrackingEventSnapshot> events) {
    if (events == null || events.isEmpty()) {
      return null;
    }
    return events.stream()
        .filter(event -> event != null && event.eventTime() != null)
        .max(Comparator.comparing(TrackingEventSnapshot::eventTime))
        .orElse(events.get(0));
  }

  private static OffsetDateTime firstDeliveredAt(List<TrackingEventSnapshot> events) {
    if (events == null || events.isEmpty()) {
      return null;
    }
    return events.stream()
        .filter(event -> event != null && event.status() == ParcelStatus.DELIVERED && event.eventTime() != null)
        .map(TrackingEventSnapshot::eventTime)
        .max(OffsetDateTime::compareTo)
        .orElseGet(() -> latestEvent(events) == null ? null : latestEvent(events).eventTime());
  }

  private static String extractSummaryLabel(String topHtml) {
    if (topHtml == null || topHtml.isBlank()) {
      return null;
    }
    Document document = Jsoup.parse(topHtml);
    Element info = document.selectFirst(".ch-colis-information");
    return info == null ? null : info.text().replaceAll("\\s+", " ").trim();
  }

  private static String extractActiveStepLabel(String topHtml) {
    if (topHtml == null || topHtml.isBlank()) {
      return null;
    }
    Document document = Jsoup.parse(topHtml);
    for (Element step : document.select(
        ".ch-suivi-colis-light-info.active,"
            + ".ch-suivi-colis-light-info.current,"
            + "[id^=step].active,"
            + "[id^=step].current,"
            + "[class*=step].active,"
            + "[class*=step].current")) {
      String value = firstNonBlank(
          textOf(step.selectFirst(".ch-suivi-colis-light-text")),
          textOf(step.selectFirst("[class*=text]")),
          step.text()
      );
      if (value != null && !value.isBlank()) {
        return value.replaceAll("\\s+", " ").trim();
      }
    }
    return null;
  }

  private static ParcelStatus statusFromDescription(String description) {
    ParcelStatus resolved = CarrierStatusResolver.resolve(description);
    if (resolved != ParcelStatus.UNKNOWN) {
      return resolved;
    }
    return TrackingPageStatusExtractor.extractStatus("<div>" + safe(description) + "</div>")
        .map(TrackingPageStatusExtractor.ExtractedStatus::status)
        .orElse(ParcelStatus.UNKNOWN);
  }

  private static String preferredStatusLabel(ParcelStatus resolvedStatus, String summaryLabel, String activeStepLabel) {
    if (resolvedStatus == null) {
      return firstNonBlank(summaryLabel, activeStepLabel);
    }
    if (summaryLabel != null && statusFromDescription(summaryLabel) == resolvedStatus) {
      return summaryLabel;
    }
    if (activeStepLabel != null && statusFromDescription(activeStepLabel) == resolvedStatus) {
      return activeStepLabel;
    }
    return firstNonBlank(summaryLabel, activeStepLabel);
  }

  private static OffsetDateTime parseChronopostDateTime(String rawValue) {
    if (rawValue == null || rawValue.isBlank()) {
      return null;
    }
    String normalized = rawValue
        .replaceAll("(?i)lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche", "")
        .replaceAll("\\s+", " ")
        .trim()
        .replace("<br>", " ");
    try {
      return LocalDateTime.parse(normalized, EVENT_DATE_TIME).atOffset(ZoneOffset.UTC);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static String trackingPageUrl(Parcel parcel) {
    return BASE_URL + "/suivi-page?listeNumerosLT="
        + URLEncoder.encode(parcel.getTrackingNumber().trim(), StandardCharsets.UTF_8)
        + "&langue=fr_FR";
  }

  private Optional<Map<String, Object>> fetchWithCookieSession(Parcel parcel) {
    try {
      Connection.Response pageResponse = Jsoup.connect(trackingPageUrl(parcel))
          .userAgent(BROWSER_USER_AGENT)
          .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
          .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
          .header("Cache-Control", "no-cache")
          .header("Pragma", "no-cache")
          .timeout(15000)
          .followRedirects(true)
          .ignoreContentType(true)
          .execute();
      if (PublicTrackingPageClient.looksLikeBotChallenge(pageResponse.body())) {
        throw new IllegalStateException("Chronopost bloque la page de suivi par Cloudflare");
      }

      String ajaxBody = Jsoup.connect(BASE_URL + "/suivi-colis?listeNumerosLT="
              + URLEncoder.encode(parcel.getTrackingNumber().trim(), StandardCharsets.UTF_8)
              + "&langue=fr")
          .userAgent(BROWSER_USER_AGENT)
          .header("Accept", "application/json, text/javascript, */*; q=0.01")
          .header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
          .header("Cache-Control", "no-cache")
          .header("Pragma", "no-cache")
          .header("Origin", "https://www.chronopost.fr")
          .header("Referer", trackingPageUrl(parcel))
          .header("X-Requested-With", "XMLHttpRequest")
          .cookies(pageResponse.cookies())
          .timeout(15000)
          .ignoreContentType(true)
          .execute()
          .body();

      if (ajaxBody == null || ajaxBody.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(parseAjaxPayload(ajaxBody));
    } catch (Exception ex) {
      log.warn("Chronopost cookie-session fallback failed for parcel {} ({})",
          parcel.getId(), parcel.getTrackingNumber(), ex);
      return Optional.empty();
    }
  }

  private Optional<Map<String, Object>> fetchWithLocalBrowser(Parcel parcel) {
    Path scriptPath = resolveBrowserScriptPath();
    if (scriptPath == null) {
      return Optional.empty();
    }

    Process process = null;
    try {
      process = new ProcessBuilder(
          "node",
          scriptPath.toString(),
          trackingPageUrl(parcel)
      ).redirectErrorStream(true).start();

      boolean finished = process.waitFor(LOCAL_BROWSER_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IllegalStateException("Chronopost local browser fallback timed out");
      }

      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (process.exitValue() != 0) {
        throw new IllegalStateException(output.isBlank() ? "Chronopost local browser fallback failed" : output);
      }
      return Optional.of(parseAjaxPayload(output));
    } catch (Exception ex) {
      log.warn("Chronopost local browser fallback failed for parcel {} ({})",
          parcel.getId(), parcel.getTrackingNumber(), ex);
      return Optional.empty();
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
  }

  private Path resolveBrowserScriptPath() {
    List<Path> candidates = List.of(
        Path.of("").toAbsolutePath().resolve("../frontend/scripts/chronopost-browser-scrape.mjs").normalize(),
        Path.of("").toAbsolutePath().resolve("frontend/scripts/chronopost-browser-scrape.mjs").normalize()
    );
    for (Path candidate : candidates) {
      if (Files.exists(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private Map<String, Object> parseAjaxPayload(String body) throws Exception {
    if (body == null || body.isBlank()) {
      return Map.of();
    }
    if (PublicTrackingPageClient.looksLikeBotChallenge(body)) {
      throw new IllegalStateException("Chronopost bloque la requete de suivi par Cloudflare");
    }
    return objectMapper.readValue(body, MAP_TYPE);
  }

  private static String normalizedCarrier(Parcel parcel) {
    if (parcel == null || parcel.getCarrierSlug() == null) {
      return "";
    }
    String normalized = parcel.getCarrierSlug().trim().toLowerCase(Locale.ROOT);
    return "unknown".equals(normalized) ? "" : normalized;
  }

  private static String normalizedTracking(Parcel parcel) {
    if (parcel == null) {
      return "";
    }
    String tracking = firstNonBlank(parcel.getNormalizedTrackingNumber(), parcel.getTrackingNumber());
    return tracking == null ? "" : tracking.trim().toUpperCase(Locale.ROOT);
  }

  private static String rawTrackingUrl(Parcel parcel) {
    if (parcel == null || parcel.getRawCurrentPayload() == null) {
      return null;
    }
    Object rawUrl = parcel.getRawCurrentPayload().get("tracking_url");
    return rawUrl == null ? null : String.valueOf(rawUrl);
  }

  private static String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }

  private static String textOf(Element element) {
    if (element == null) {
      return null;
    }
    String text = element.text();
    return text == null || text.isBlank() ? null : text.trim();
  }

  private static List<String> extractLines(String html) {
    if (html == null || html.isBlank()) {
      return List.of();
    }
    String normalized = html
        .replace("<br>", "\n")
        .replace("<br/>", "\n")
        .replace("<br />", "\n");
    List<String> lines = new ArrayList<>();
    for (String part : normalized.split("\n")) {
      String text = Jsoup.parse(part).text().replaceAll("\\s+", " ").trim();
      if (!text.isBlank()) {
        lines.add(text);
      }
    }
    return lines;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private static String joinNonBlank(String separator, String... values) {
    List<String> parts = new ArrayList<>();
    if (values != null) {
      for (String value : values) {
        if (value != null && !value.isBlank()) {
          parts.add(value.trim());
        }
      }
    }
    return parts.isEmpty() ? null : String.join(separator, parts);
  }

  private static void putIfPresent(Map<String, Object> target, String key, String value) {
    if (value != null && !value.isBlank()) {
      target.put(key, value);
    }
  }

  private static ParcelStatus bestStatus(ParcelStatus... statuses) {
    ParcelStatus best = ParcelStatus.UNKNOWN;
    for (ParcelStatus status : statuses) {
      if (statusPriority(status) > statusPriority(best)) {
        best = status;
      }
    }
    return best;
  }

  private static int statusPriority(ParcelStatus status) {
    if (status == null) {
      return 0;
    }
    return switch (status) {
      case PENDING, UNKNOWN -> 0;
      case REGISTERED -> 1;
      case IN_TRANSIT -> 2;
      case OUT_FOR_DELIVERY -> 3;
      case EXCEPTION -> 4;
      case DELIVERED -> 5;
    };
  }

}
