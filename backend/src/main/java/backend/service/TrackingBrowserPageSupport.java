package backend.service;

import backend.entity.ParcelStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TrackingBrowserPageSupport {

  private static final Pattern EUROPEAN_DATE_PATTERN = Pattern.compile(
      "\\b\\d{1,2}/\\d{1,2}/\\d{4}(?:\\s+(?:a\\s+)?\\d{1,2}[:hH]\\d{2})?\\b",
      Pattern.CASE_INSENSITIVE
  );
  private static final Pattern UPS_DATE_PATTERN = Pattern.compile(
      "(?i)\\b(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|"
          + "aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+\\d{1,2},\\s+\\d{4}"
          + "(?:,?\\s+\\d{1,2}:\\d{2}\\s*(?:a\\.m\\.|p\\.m\\.|am|pm))?\\b"
  );
  private static final DateTimeFormatter EUROPEAN_DATE_TIME = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern("d/M/uuuu")
      .optionalStart()
      .appendLiteral(' ')
      .appendValue(ChronoField.HOUR_OF_DAY)
      .appendLiteral(':')
      .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
      .optionalEnd()
      .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
      .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
      .toFormatter(Locale.FRANCE);
  private static final List<DateTimeFormatter> UPS_DATE_TIME_FORMATTERS = List.of(
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("MMMM d, uuuu h:mm a")
          .toFormatter(Locale.ENGLISH),
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("MMM d, uuuu h:mm a")
          .toFormatter(Locale.ENGLISH),
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("MMMM d, uuuu")
          .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
          .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
          .toFormatter(Locale.ENGLISH),
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("MMM d, uuuu")
          .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
          .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
          .toFormatter(Locale.ENGLISH)
  );

  private TrackingBrowserPageSupport() {
  }

  static List<String> meaningfulLines(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    LinkedHashSet<String> lines = new LinkedHashSet<>();
    for (String rawLine : text.split("\\R+")) {
      String line = compact(rawLine);
      if (line.length() < 4 || line.length() > 220) {
        continue;
      }
      lines.add(line);
      if (lines.size() >= 160) {
        break;
      }
    }
    return new ArrayList<>(lines);
  }

  static String bestStatusLabel(String text) {
    StatusLine best = null;
    for (String line : meaningfulLines(text)) {
      Optional<CarrierStatusResolver.StatusMatch> match = CarrierStatusResolver.best(line);
      if (match.isEmpty()) {
        continue;
      }
      StatusLine candidate = new StatusLine(line, match.get().status(), match.get().score());
      if (best == null
          || candidate.score() > best.score()
          || (candidate.score() == best.score() && candidate.line().length() < best.line().length())) {
        best = candidate;
      }
    }
    return best == null ? null : best.line();
  }

  static String bestStatusLabel(String text, String title) {
    String label = bestStatusLabel(text);
    if (label != null) {
      return label;
    }

    String candidate = compact(title);
    if (candidate.isBlank()) {
      return null;
    }

    if (CarrierStatusResolver.best(candidate).isPresent()) {
      return candidate;
    }

    String normalized = candidate.toLowerCase(Locale.ROOT);
    if (containsAny(normalized, "aucune information", "aucun resultat", "aucun envoi", "introuvable",
        "information not available", "not found", "we could not locate")) {
      return candidate;
    }
    return null;
  }

  static List<TrackingEventSnapshot> extractEuropeanEvents(String text) {
    return extractEvents(meaningfulLines(text), EUROPEAN_DATE_PATTERN, TrackingBrowserPageSupport::parseEuropeanDateTime);
  }

  static List<TrackingEventSnapshot> extractUpsEvents(String text) {
    return extractEvents(meaningfulLines(text), UPS_DATE_PATTERN, TrackingBrowserPageSupport::parseUpsDateTime);
  }

  static OffsetDateTime latestEventTime(List<TrackingEventSnapshot> events) {
    if (events == null || events.isEmpty()) {
      return null;
    }
    return events.stream()
        .map(TrackingEventSnapshot::eventTime)
        .filter(java.util.Objects::nonNull)
        .max(OffsetDateTime::compareTo)
        .orElse(null);
  }

  static OffsetDateTime latestDeliveredAt(List<TrackingEventSnapshot> events) {
    if (events == null || events.isEmpty()) {
      return null;
    }
    return events.stream()
        .filter(event -> event != null && event.status() == ParcelStatus.DELIVERED && event.eventTime() != null)
        .map(TrackingEventSnapshot::eventTime)
        .max(OffsetDateTime::compareTo)
        .orElse(null);
  }

  static ParcelStatus resolveBestStatus(String html, String text, List<TrackingEventSnapshot> events) {
    ParcelStatus best = TrackingPageStatusExtractor.extractStatus(html == null ? "" : html)
        .map(TrackingPageStatusExtractor.ExtractedStatus::status)
        .orElse(ParcelStatus.UNKNOWN);

    ParcelStatus fromLabel = CarrierStatusResolver.resolve(bestStatusLabel(text));
    if (statusPriority(fromLabel) > statusPriority(best)) {
      best = fromLabel;
    }

    if (events != null) {
      for (TrackingEventSnapshot event : events) {
        if (event != null && statusPriority(event.status()) > statusPriority(best)) {
          best = event.status();
        }
      }
    }
    return best == null ? ParcelStatus.UNKNOWN : best;
  }

  static String compact(String value) {
    return value == null ? "" : value.replaceAll("\\s+", " ").trim();
  }

  static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private static boolean containsAny(String value, String... needles) {
    if (value == null || needles == null) {
      return false;
    }
    for (String needle : needles) {
      if (needle != null && value.contains(needle)) {
        return true;
      }
    }
    return false;
  }

  private static List<TrackingEventSnapshot> extractEvents(
      List<String> lines,
      Pattern datePattern,
      java.util.function.Function<String, OffsetDateTime> dateParser
  ) {
    if (lines == null || lines.isEmpty()) {
      return List.of();
    }

    List<TrackingEventSnapshot> events = new ArrayList<>();
    for (int index = 0; index < lines.size(); index++) {
      String line = lines.get(index);
      Matcher matcher = datePattern.matcher(line);
      if (!matcher.find()) {
        continue;
      }

      OffsetDateTime eventTime = dateParser.apply(matcher.group());
      if (eventTime == null) {
        continue;
      }

      List<String> detailParts = new ArrayList<>();
      String sameLineRemainder = compact(line.substring(matcher.end()));
      if (!sameLineRemainder.isBlank()) {
        detailParts.add(sameLineRemainder.replaceFirst("^[\\-:|]+\\s*", ""));
      }

      for (int cursor = index + 1; cursor < lines.size() && detailParts.size() < 3; cursor++) {
        String nextLine = lines.get(cursor);
        if (datePattern.matcher(nextLine).find()) {
          break;
        }
        detailParts.add(nextLine);
      }

      String description = detailParts.isEmpty() ? line : String.join(" | ", detailParts);
      ParcelStatus status = CarrierStatusResolver.resolve(description, line);
      if (status == ParcelStatus.UNKNOWN && detailParts.isEmpty()) {
        continue;
      }

      events.add(new TrackingEventSnapshot(
          status,
          null,
          description,
          null,
          eventTime,
          java.util.Map.of("source_line", line)
      ));
    }

    events.sort(Comparator.comparing(
        TrackingEventSnapshot::eventTime,
        Comparator.nullsLast(Comparator.reverseOrder())
    ));
    return events;
  }

  private static OffsetDateTime parseEuropeanDateTime(String rawValue) {
    String normalized = compact(rawValue)
        .replace(" à ", " ")
        .replace(" a ", " ")
        .replace('h', ':')
        .replace('H', ':');
    try {
      LocalDateTime value = LocalDateTime.parse(normalized, EUROPEAN_DATE_TIME);
      return value.atOffset(ZoneOffset.UTC);
    } catch (DateTimeParseException ignored) {
      try {
        LocalDate value = LocalDate.parse(normalized, DateTimeFormatter.ofPattern("d/M/uuuu", Locale.FRANCE));
        return value.atStartOfDay().atOffset(ZoneOffset.UTC);
      } catch (DateTimeParseException ignoredAgain) {
        return null;
      }
    }
  }

  private static OffsetDateTime parseUpsDateTime(String rawValue) {
    String normalized = compact(rawValue)
        .replace("A.M.", "AM")
        .replace("P.M.", "PM")
        .replace("a.m.", "AM")
        .replace("p.m.", "PM");
    for (DateTimeFormatter formatter : UPS_DATE_TIME_FORMATTERS) {
      try {
        if (normalized.contains(":")) {
          return LocalDateTime.parse(normalized, formatter).atOffset(ZoneOffset.UTC);
        }
        return LocalDate.parse(normalized, formatter).atStartOfDay().atOffset(ZoneOffset.UTC);
      } catch (DateTimeParseException ignored) {
        // try next formatter
      }
    }
    return null;
  }

  private static int statusPriority(ParcelStatus status) {
    if (status == null) {
      return 0;
    }
    return switch (status) {
      case UNKNOWN, PENDING -> 0;
      case REGISTERED -> 1;
      case IN_TRANSIT -> 2;
      case OUT_FOR_DELIVERY -> 3;
      case EXCEPTION -> 4;
      case DELIVERED -> 5;
    };
  }

  private record StatusLine(String line, ParcelStatus status, int score) {
  }
}
