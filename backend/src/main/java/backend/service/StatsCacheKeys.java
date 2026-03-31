package backend.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class StatsCacheKeys {

  private StatsCacheKeys() {
  }

  private static String d(LocalDate value) {
    return value == null ? "_" : value.toString();
  }

  private static String s(String value) {
    if (value == null) return "_";
    String trimmed = value.trim();
    return trimmed.isEmpty() ? "_" : trimmed;
  }

  private static String n(Number value) {
    return value == null ? "_" : String.valueOf(value);
  }

  private static String list(List<String> values) {
    if (values == null || values.isEmpty()) return "*";
    return values.stream()
        .filter(v -> v != null && !v.trim().isEmpty())
        .map(String::trim)
        .distinct()
        .sorted(Comparator.naturalOrder())
        .collect(Collectors.joining(","));
  }

  public static String summary(
      Long userId,
      LocalDate from,
      LocalDate to,
      LocalDate asOf,
      List<String> categories,
      List<String> types
  ) {
    return "summary|" + n(userId) + "|" + d(from) + "|" + d(to) + "|" + d(asOf) + "|" + list(categories) + "|" + list(types);
  }

  public static String timeseries(
      Long userId,
      LocalDate from,
      LocalDate to,
      String granularity,
      List<String> categories,
      List<String> types
  ) {
    return "timeseries|" + n(userId) + "|" + d(from) + "|" + d(to) + "|" + s(granularity) + "|" + list(categories) + "|" + list(types);
  }

  public static String topSales(
      Long userId,
      LocalDate from,
      LocalDate to,
      Integer limit,
      List<String> categories,
      List<String> types
  ) {
    return "topSales|" + n(userId) + "|" + d(from) + "|" + d(to) + "|" + n(limit) + "|" + list(categories) + "|" + list(types);
  }

  public static String kpi(
      Long userId,
      LocalDate from,
      LocalDate to,
      String metric,
      List<String> categories,
      List<String> types
  ) {
    return "kpi|" + n(userId) + "|" + d(from) + "|" + d(to) + "|" + s(metric) + "|" + list(categories) + "|" + list(types);
  }

  public static String series(
      Long userId,
      LocalDate from,
      LocalDate to,
      String metric,
      String granularity,
      List<String> categories,
      List<String> types
  ) {
    return "series|" + n(userId) + "|" + d(from) + "|" + d(to) + "|" + s(metric) + "|" + s(granularity) + "|" + list(categories) + "|" + list(types);
  }

  public static String breakdown(
      Long userId,
      String metric,
      LocalDate from,
      LocalDate to,
      List<String> categories,
      List<String> types
  ) {
    return "breakdown|" + n(userId) + "|" + s(metric) + "|" + d(from) + "|" + d(to) + "|" + list(categories) + "|" + list(types);
  }

  public static String rank(
      Long userId,
      LocalDate from,
      LocalDate to,
      String metric,
      Integer limit,
      List<String> categories,
      List<String> types
  ) {
    return "rank|" + n(userId) + "|" + d(from) + "|" + d(to) + "|" + s(metric) + "|" + n(limit) + "|" + list(categories) + "|" + list(types);
  }

  public static String categories(Long userId, LocalDate from, LocalDate to) {
    return "categories|" + n(userId) + "|" + d(from) + "|" + d(to);
  }

  public static String dateBounds(Long userId) {
    return "dateBounds|" + n(userId);
  }
}
