package backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.dto.*;
import backend.dto.TopVenteProjection;
import backend.entity.SnkVente;
import backend.repository.SnkVenteRepository;

@Service
@Transactional(readOnly = true)
public class StatsService {

  private final SnkVenteRepository repo;

  public StatsService(SnkVenteRepository repo) {
    this.repo = repo;
  }

  public StatsSummaryResponse summary(Long userId, LocalDate from, LocalDate to) {
    return summary(userId, from, to, null, null, null);
  }

  public StatsSummaryResponse summary(Long userId, LocalDate from, LocalDate to, LocalDate asOf) {
    return summary(userId, from, to, asOf, null, null);
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).summary(#userId,#from,#to,#asOf,#categories,#types)"
  )
  public StatsSummaryResponse summary(
      Long userId,
      LocalDate from,
      LocalDate to,
      LocalDate asOf,
      List<String> categories,
      List<String> types
  ) {
    List<String> cats = normalizeCategories(categories);
    List<String> itemTypes = normalizeTypes(types);
    boolean categoriesAll = isAllCategories(cats);
    boolean typesAll = isAllTypes(itemTypes);
    String[] catArray = toArray(cats);
    String[] typeArray = toArray(itemTypes);
    LocalDateRange range = normalizeRange(from, to);
    LocalDate asOfDate = asOf != null ? asOf : range.to();

    BigDecimal ca = repo.caBetween(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll);
    BigDecimal profit = repo.profitBetween(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll);
    long sold = repo.countSoldBetween(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll);

    long stock = asOf != null
        ? repo.countInStockAt(userId, asOfDate, catArray, categoriesAll, typeArray, typesAll)
        : repo.countInStock(userId, catArray, categoriesAll, typeArray, typesAll);
    BigDecimal stockValue = asOf != null
        ? repo.stockValueAt(userId, asOfDate, catArray, categoriesAll, typeArray, typesAll)
        : repo.stockValue(userId, catArray, categoriesAll, typeArray, typesAll);

    BigDecimal margin = BigDecimal.ZERO;
    BigDecimal safeCa = nz(ca);
    BigDecimal safeProfit = nz(profit);
    if (safeCa.compareTo(BigDecimal.ZERO) > 0) {
      margin = safeProfit.divide(safeCa, 4, RoundingMode.HALF_UP);
    }

    return new StatsSummaryResponse(
        ca == null ? BigDecimal.ZERO : ca,
        profit == null ? BigDecimal.ZERO : profit,
        margin,
        sold,
        stock,
        stockValue == null ? BigDecimal.ZERO : stockValue
    );
  }

  public List<StatsPointResponse> timeseries(Long userId, LocalDate from, LocalDate to, String granularity) {
    return timeseries(userId, from, to, granularity, null, null);
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).timeseries(#userId,#from,#to,#granularity,#categories,#types)"
  )
  public List<StatsPointResponse> timeseries(
      Long userId,
      LocalDate from,
      LocalDate to,
      String granularity,
      List<String> categories,
      List<String> types
  ) {
    List<String> cats = normalizeCategories(categories);
    List<String> itemTypes = normalizeTypes(types);
    boolean categoriesAll = isAllCategories(cats);
    boolean typesAll = isAllTypes(itemTypes);
    String[] catArray = toArray(cats);
    String[] typeArray = toArray(itemTypes);
    LocalDateRange range = normalizeRange(from, to);
    var rows = "week".equalsIgnoreCase(granularity)
        ? repo.timeseriesWeek(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll)
        : "month".equalsIgnoreCase(granularity)
            ? repo.timeseriesMonth(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll)
            : repo.timeseriesDay(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll);

    return rows.stream()
        .map(r -> new StatsPointResponse(r.getBucket(), r.getCa(), r.getProfit()))
        .toList();
  }

  public List<StatsBreakdownResponse> brandBreakdown(Long userId, LocalDate from, LocalDate to) {
    return brandBreakdown(userId, from, to, null, null);
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).breakdown(#userId,'brands',#from,#to,#categories,#types)"
  )
  public List<StatsBreakdownResponse> brandBreakdown(
      Long userId,
      LocalDate from,
      LocalDate to,
      List<String> categories,
      List<String> types
  ) {
    List<String> cats = normalizeCategories(categories);
    List<String> itemTypes = normalizeTypes(types);
    boolean categoriesAll = isAllCategories(cats);
    boolean typesAll = isAllTypes(itemTypes);
    String[] catArray = toArray(cats);
    String[] typeArray = toArray(itemTypes);
    LocalDateRange range = normalizeRange(from, to);
    return repo.brandBreakdownSales(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll).stream()
        .map(r -> new StatsBreakdownResponse(r.getLabel(), r.getNb()))
        .toList();
  }

  public List<TopVenteProjection> topSales(Long userId, LocalDate from, LocalDate to, int limit) {
    return topSales(userId, from, to, limit, null, null);
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).topSales(#userId,#from,#to,#limit,#categories,#types)"
  )
  public List<TopVenteProjection> topSales(
      Long userId,
      LocalDate from,
      LocalDate to,
      int limit,
      List<String> categories,
      List<String> types
  ) {
    List<String> cats = normalizeCategories(categories);
    List<String> itemTypes = normalizeTypes(types);
    boolean categoriesAll = isAllCategories(cats);
    boolean typesAll = isAllTypes(itemTypes);
    String[] catArray = toArray(cats);
    String[] typeArray = toArray(itemTypes);
    LocalDateRange range = normalizeRange(from, to);
    int safe = Math.min(Math.max(limit, 1), 20);
    return repo.topVentesBetween(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll)
        .stream()
        .limit(safe)
        .toList();
  }

  public StatsKpiResponse kpi(Long userId, LocalDate from, LocalDate to, String metric) {
    return kpi(userId, from, to, metric, null, null);
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).kpi(#userId,#from,#to,#metric,#categories,#types)"
  )
  public StatsKpiResponse kpi(
      Long userId,
      LocalDate from,
      LocalDate to,
      String metric,
      List<String> categories,
      List<String> types
  ) {
    List<String> cats = normalizeCategories(categories);
    LocalDateRange range = normalizeRange(from, to);
    LocalDateRange prev = prevRange(range.from(), range.to());

    StatsSummaryResponse curr = summary(userId, range.from(), range.to(), null, cats, types);
    StatsSummaryResponse prevSummary = summary(userId, prev.from(), prev.to(), null, cats, types);

    BigDecimal currentValue = metricFromSummary(userId, curr, metric, range.from(), range.to(), cats, types);
    BigDecimal prevValue = metricFromSummary(userId, prevSummary, metric, prev.from(), prev.to(), cats, types);
    BigDecimal delta = deltaPct(currentValue, prevValue);

    return new StatsKpiResponse(currentValue, delta);
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).series(#userId,#from,#to,#metric,#granularity,#categories,#types)"
  )
  public List<StatsSeriesPointResponse> series(
      Long userId,
      LocalDate from,
      LocalDate to,
      String metric,
      String granularity
  ) {
    return series(userId, from, to, metric, granularity, null, null);
  }

  public List<StatsSeriesPointResponse> series(
      Long userId,
      LocalDate from,
      LocalDate to,
      String metric,
      String granularity,
      List<String> categories,
      List<String> types
  ) {
    List<String> cats = normalizeCategories(categories);
    List<String> itemTypes = normalizeTypes(types);
    boolean categoriesAll = isAllCategories(cats);
    boolean typesAll = isAllTypes(itemTypes);
    String[] catArray = toArray(cats);
    String[] typeArray = toArray(itemTypes);
    LocalDateRange range = normalizeRange(from, to);

    if ("avgDaysToSell".equalsIgnoreCase(metric)) {
      var rows = avgDaysRows(userId, range.from(), range.to(), granularity, catArray, categoriesAll, typeArray, typesAll);
      return rows.stream()
          .map(r -> new StatsSeriesPointResponse(r.getBucket(), toBigDecimal(r.getAvgDays())))
          .toList();
    }

    var rows = timeseriesFull(userId, range.from(), range.to(), granularity, catArray, categoriesAll, typeArray, typesAll);

    return rows.stream()
        .map(r -> {
          long stockAtBucket = historicalStockMetric(metric)
              ? repo.countInStockAt(userId, r.getBucket(), catArray, categoriesAll, typeArray, typesAll)
              : 0;
          return new StatsSeriesPointResponse(
              r.getBucket(),
              metricFromTimeseries(r.getCa(), r.getProfit(), r.getCost(), r.getNb(), stockAtBucket, metric)
          );
        })
        .toList();
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).breakdown(#userId,#metric,#from,#to,#categories,#types)"
  )
  public List<StatsLabelValueResponse> breakdown(
      Long userId,
      String metric,
      LocalDate from,
      LocalDate to
  ) {
    return breakdown(userId, metric, from, to, null, null);
  }

  public List<StatsLabelValueResponse> breakdown(
      Long userId,
      String metric,
      LocalDate from,
      LocalDate to,
      List<String> categories,
      List<String> types
  ) {
    List<String> cats = normalizeCategories(categories);
    List<String> itemTypes = normalizeTypes(types);
    boolean categoriesAll = isAllCategories(cats);
    boolean typesAll = isAllTypes(itemTypes);
    String[] catArray = toArray(cats);
    String[] typeArray = toArray(itemTypes);
    LocalDateRange range = normalizeRange(from, to);

    if ("deathPileAge".equalsIgnoreCase(metric)) {
      return repo.deathPileAge(userId, catArray, categoriesAll, typeArray, typesAll).stream()
          .map(r -> new StatsLabelValueResponse(r.getLabel(), r.getValue()))
          .toList();
    }

    if ("brands".equalsIgnoreCase(metric)) {
      return repo.brandBreakdownSales(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll).stream()
          .map(r -> new StatsLabelValueResponse(r.getLabel(), BigDecimal.valueOf(r.getNb())))
          .toList();
    }

    if ("typeSoldCount".equalsIgnoreCase(metric)) {
      return repo.soldCountByType(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll).stream()
          .map(r -> new StatsLabelValueResponse(r.getLabel(), BigDecimal.valueOf(r.getValue())))
          .toList();
    }

    if ("typeStockCount".equalsIgnoreCase(metric)) {
      return repo.stockCountByTypeAt(userId, range.to(), catArray, categoriesAll, typeArray, typesAll).stream()
          .map(r -> new StatsLabelValueResponse(r.getLabel(), BigDecimal.valueOf(r.getValue())))
          .toList();
    }

    if ("typeProfit".equalsIgnoreCase(metric)) {
      return repo.profitByType(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll).stream()
          .map(r -> new StatsLabelValueResponse(r.getLabel(), r.getValue()))
          .toList();
    }

    return List.of();
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).rank(#userId,#from,#to,#metric,#limit,#categories,#types)"
  )
  public List<StatsLabelValueResponse> rank(
      Long userId,
      LocalDate from,
      LocalDate to,
      String metric,
      int limit
  ) {
    return rank(userId, from, to, metric, limit, null, null);
  }

  public List<StatsLabelValueResponse> rank(
      Long userId,
      LocalDate from,
      LocalDate to,
      String metric,
      int limit,
      List<String> categories,
      List<String> types
  ) {
    List<String> cats = normalizeCategories(categories);
    List<String> itemTypes = normalizeTypes(types);
    boolean categoriesAll = isAllCategories(cats);
    boolean typesAll = isAllTypes(itemTypes);
    String[] catArray = toArray(cats);
    String[] typeArray = toArray(itemTypes);
    LocalDateRange range = normalizeRange(from, to);
    int safe = Math.min(Math.max(limit, 1), 50);

    if ("topBrandsProfit".equalsIgnoreCase(metric)) {
      return repo.topBrandsProfit(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll).stream()
          .limit(safe)
          .map(r -> new StatsLabelValueResponse(r.getLabel(), r.getValue()))
          .toList();
    }

    if ("topCategoriesProfit".equalsIgnoreCase(metric)) {
      return repo.topCategoriesProfit(userId, range.from(), range.to(), catArray, categoriesAll, typeArray, typesAll).stream()
          .limit(safe)
          .map(r -> new StatsLabelValueResponse(r.getLabel(), r.getValue()))
          .toList();
    }

    return List.of();
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).dateBounds(#userId)"
  )
  public StatsDateBoundsResponse dateBounds(Long userId) {
    LocalDate minAchat = repo.minAchatDate(userId);
    LocalDate minVente = repo.minVenteDate(userId);
    LocalDate min = null;
    if (minAchat != null && minVente != null) {
      min = minAchat.isBefore(minVente) ? minAchat : minVente;
    } else if (minAchat != null) {
      min = minAchat;
    } else if (minVente != null) {
      min = minVente;
    }
    LocalDate max = LocalDate.now();
    return new StatsDateBoundsResponse(min, max);
  }

  private record LocalDateRange(LocalDate from, LocalDate to) {}

  private LocalDateRange normalizeRange(LocalDate from, LocalDate to) {
    LocalDate end = to != null ? to : LocalDate.now();
    LocalDate start = from != null ? from : end.minusDays(30);
    if (start.isAfter(end)) {
      LocalDate tmp = start;
      start = end;
      end = tmp;
    }
    return new LocalDateRange(start, end);
  }

  private LocalDateRange prevRange(LocalDate from, LocalDate to) {
    long days = ChronoUnit.DAYS.between(from, to) + 1;
    LocalDate prevTo = from.minusDays(1);
    LocalDate prevFrom = prevTo.minusDays(days - 1);
    return new LocalDateRange(prevFrom, prevTo);
  }

  private BigDecimal metricFromSummary(
      Long userId,
      StatsSummaryResponse summary,
      String metric,
      LocalDate from,
      LocalDate to,
      List<String> categories,
      List<String> types
  ) {
    BigDecimal ca = nz(summary.ca());
    BigDecimal profit = nz(summary.profit());
    BigDecimal margin = nz(summary.profitMargin());
    long sold = summary.itemsVendues();
    long stock = summary.itemsEnStock();
    List<String> itemTypes = normalizeTypes(types);
    BigDecimal invested = repo.costBetween(
        userId,
        from,
        to,
        toArray(categories),
        isAllCategories(categories),
        toArray(itemTypes),
        isAllTypes(itemTypes)
    );

    if ("roi".equalsIgnoreCase(metric)) {
      if (invested == null || invested.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
      return profit.divide(invested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
    if ("avgMargin".equalsIgnoreCase(metric)) {
      return sold > 0 ? profit.divide(BigDecimal.valueOf(sold), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
    if ("asp".equalsIgnoreCase(metric)) {
      return sold > 0 ? ca.divide(BigDecimal.valueOf(sold), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
    if ("activeListings".equalsIgnoreCase(metric)) {
      return BigDecimal.valueOf(stock);
    }
    if ("sellThrough".equalsIgnoreCase(metric)) {
      long total = sold + stock;
      if (total == 0) return BigDecimal.ZERO;
      return BigDecimal.valueOf(sold).multiply(BigDecimal.valueOf(100))
          .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }
    if ("cashAvailable".equalsIgnoreCase(metric)) {
      return profit;
    }
    if ("avgDaysToSell".equalsIgnoreCase(metric)) {
      Double avg = repo.avgDaysToSellBetween(
          userId,
          from,
          to,
          toArray(categories),
          isAllCategories(categories),
          toArray(itemTypes),
          isAllTypes(itemTypes)
      );
      return toBigDecimal(avg);
    }
    if ("grossRevenue".equalsIgnoreCase(metric) || "ca".equalsIgnoreCase(metric)) {
      return ca;
    }
    if ("netProfit".equalsIgnoreCase(metric) || "profit".equalsIgnoreCase(metric)) {
      return profit;
    }

    return BigDecimal.ZERO;
  }

  private BigDecimal metricFromTimeseries(
      BigDecimal ca,
      BigDecimal profit,
      BigDecimal cost,
      long sold,
      long stockAtBucket,
      String metric
  ) {
    BigDecimal safeCa = nz(ca);
    BigDecimal safeProfit = nz(profit);
    BigDecimal safeCost = nz(cost);

    if ("avgMargin".equalsIgnoreCase(metric)) {
      return sold > 0 ? safeProfit.divide(BigDecimal.valueOf(sold), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
    if ("asp".equalsIgnoreCase(metric)) {
      return sold > 0 ? safeCa.divide(BigDecimal.valueOf(sold), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
    if ("roi".equalsIgnoreCase(metric)) {
      if (safeCost.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
      return safeProfit.divide(safeCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
    if ("sellThrough".equalsIgnoreCase(metric)) {
      long total = sold + stockAtBucket;
      if (total == 0) return BigDecimal.ZERO;
      return BigDecimal.valueOf(sold).multiply(BigDecimal.valueOf(100))
          .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }
    if ("activeListings".equalsIgnoreCase(metric)) {
      return BigDecimal.valueOf(stockAtBucket);
    }
    if ("grossRevenue".equalsIgnoreCase(metric) || "ca".equalsIgnoreCase(metric)) {
      return safeCa;
    }
    if ("netProfit".equalsIgnoreCase(metric) || "profit".equalsIgnoreCase(metric) || "cashAvailable".equalsIgnoreCase(metric)) {
      return safeProfit;
    }
    return BigDecimal.ZERO;
  }

  private boolean historicalStockMetric(String metric) {
    return "sellThrough".equalsIgnoreCase(metric) || "activeListings".equalsIgnoreCase(metric);
  }

  private List<SnkVenteRepository.AvgDaysRow> avgDaysRows(
      Long userId,
      LocalDate from,
      LocalDate to,
      String granularity,
      String[] categories,
      boolean categoriesAll,
      String[] types,
      boolean typesAll
  ) {
    if ("week".equalsIgnoreCase(granularity)) {
      return repo.avgDaysToSellWeek(userId, from, to, categories, categoriesAll, types, typesAll);
    }
    if ("month".equalsIgnoreCase(granularity)) {
      return repo.avgDaysToSellMonth(userId, from, to, categories, categoriesAll, types, typesAll);
    }
    return repo.avgDaysToSellDay(userId, from, to, categories, categoriesAll, types, typesAll);
  }

  private List<SnkVenteRepository.TimePointFullRow> timeseriesFull(
      Long userId,
      LocalDate from,
      LocalDate to,
      String granularity,
      String[] categories,
      boolean categoriesAll,
      String[] types,
      boolean typesAll
  ) {
    if ("week".equalsIgnoreCase(granularity)) {
      return repo.timeseriesWeekFull(userId, from, to, categories, categoriesAll, types, typesAll);
    }
    if ("month".equalsIgnoreCase(granularity)) {
      return repo.timeseriesMonthFull(userId, from, to, categories, categoriesAll, types, typesAll);
    }
    return repo.timeseriesDayFull(userId, from, to, categories, categoriesAll, types, typesAll);
  }

  @Cacheable(
      cacheNames = "statsQueries",
      key = "T(backend.service.StatsCacheKeys).categories(#userId,#from,#to)"
  )
  public List<String> categories(Long userId, LocalDate from, LocalDate to) {
    List<String> scoped = null;
    if (from != null && to != null) {
      scoped = repo.distinctCategoriesBetween(userId, from, to);
    }
    if (scoped != null && !scoped.isEmpty()) {
      return scoped;
    }
    return repo.distinctCategories(userId);
  }

  public AnnualDashboardResponse annualDashboard(Long userId, int year) {
    LocalDate today = LocalDate.now();
    int safeYear = Math.min(Math.max(year, 2000), 2100);
    LocalDate start = LocalDate.of(safeYear, 1, 1);
    LocalDate end = LocalDate.of(safeYear, 12, 31);
    LocalDate asOf = safeYear == today.getYear() ? today : end;

    List<SnkVente> sold = repo.findSoldBetweenForAnnualDashboard(userId, start, end);
    List<SnkVente> purchased = repo.findPurchasedBetweenForAnnualDashboard(userId, start, end);
    List<SnkVente> inventory = repo.findInventoryAtForAnnualDashboard(userId, asOf);

    AnnualAccumulator[] monthly = new AnnualAccumulator[12];
    for (int i = 0; i < monthly.length; i++) {
      monthly[i] = new AnnualAccumulator();
    }

    AnnualAccumulator soldTotals = new AnnualAccumulator();
    Map<String, AnnualAccumulator> categoryTotals = new LinkedHashMap<>();
    List<String> partialReasons = new ArrayList<>();
    long holdDaysTotal = 0;
    long holdDaysCount = 0;

    for (SnkVente item : sold) {
      if (item.getDateVente() == null) continue;
      int monthIndex = item.getDateVente().getMonthValue() - 1;
      BigDecimal revenue = nz(item.getPrixResell());
      BigDecimal purchasePrice = nz(item.getPrixRetail());
      BigDecimal profit = revenue.subtract(purchasePrice);

      soldTotals.revenue = soldTotals.revenue.add(revenue);
      soldTotals.profit = soldTotals.profit.add(profit);
      soldTotals.cost = soldTotals.cost.add(purchasePrice);
      soldTotals.count += 1;

      monthly[monthIndex].revenue = monthly[monthIndex].revenue.add(revenue);
      monthly[monthIndex].profit = monthly[monthIndex].profit.add(profit);
      monthly[monthIndex].count += 1;

      AnnualAccumulator category = categoryTotals.computeIfAbsent(categoryName(item), ignored -> new AnnualAccumulator());
      category.revenue = category.revenue.add(revenue);
      category.profit = category.profit.add(profit);
      category.count += 1;

      if (item.getDateAchat() != null && !item.getDateAchat().isAfter(item.getDateVente())) {
        holdDaysTotal += ChronoUnit.DAYS.between(item.getDateAchat(), item.getDateVente());
        holdDaysCount += 1;
      }

      if (item.getPrixResell() == null) {
        addReason(partialReasons, "Certaines ventes n'ont pas de prix de vente.");
      }
      if (item.getPrixRetail() == null) {
        addReason(partialReasons, "Certains couts d'achat sont manquants.");
      }
      if (item.getDateAchat() == null) {
        addReason(partialReasons, "Certaines ventes n'ont pas de date d'achat.");
      }
    }

    BigDecimal purchaseSpend = BigDecimal.ZERO;
    long itemsBought = 0;
    for (SnkVente item : purchased) {
      if (item.getDateAchat() == null) continue;
      int monthIndex = item.getDateAchat().getMonthValue() - 1;
      BigDecimal cost = nz(item.getPrixRetail());
      purchaseSpend = purchaseSpend.add(cost);
      itemsBought += 1;
      monthly[monthIndex].purchaseSpend = monthly[monthIndex].purchaseSpend.add(cost);
      monthly[monthIndex].itemsBought += 1;
      if (item.getPrixRetail() == null) {
        addReason(partialReasons, "Certains achats n'ont pas de cout renseigne.");
      }
    }

    BigDecimal remainingStockValue = BigDecimal.ZERO;
    for (SnkVente item : inventory) {
      remainingStockValue = remainingStockValue.add(nz(item.getPrixRetail()));
      if (item.getDateAchat() == null) {
        addReason(partialReasons, "Certains articles en stock n'ont pas de date d'achat.");
      }
      if (item.getPrixRetail() == null) {
        addReason(partialReasons, "Certains articles en stock n'ont pas de cout d'achat.");
      }
    }

    BigDecimal revenue = soldTotals.revenue;
    BigDecimal profit = soldTotals.profit;
    long itemsSold = soldTotals.count;
    BigDecimal soldCost = soldTotals.cost;
    BigDecimal marginRate = divide(profit, revenue, 4);
    BigDecimal roi = divide(profit, soldCost, 4);
    BigDecimal averageSalePrice = itemsSold > 0
        ? revenue.divide(BigDecimal.valueOf(itemsSold), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;
    BigDecimal averageProfit = itemsSold > 0
        ? profit.divide(BigDecimal.valueOf(itemsSold), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;
    BigDecimal sellThroughRate = divide(
        BigDecimal.valueOf(itemsSold),
        BigDecimal.valueOf(itemsSold + inventory.size()),
        4
    );
    BigDecimal averageHoldDays = holdDaysCount > 0
        ? BigDecimal.valueOf(holdDaysTotal).divide(BigDecimal.valueOf(holdDaysCount), 1, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    AnnualDashboardResponse.AnnualDashboardSummary summary =
        new AnnualDashboardResponse.AnnualDashboardSummary(
            money(revenue),
            money(profit),
            marginRate,
            roi,
            itemsSold,
            averageSalePrice,
            averageProfit,
            money(purchaseSpend),
            itemsBought,
            inventory.size(),
            money(remainingStockValue),
            sellThroughRate,
            averageHoldDays
        );

    boolean hasData = itemsSold > 0 || itemsBought > 0 || !inventory.isEmpty();

    return new AnnualDashboardResponse(
        safeYear,
        asOf,
        hasData,
        !partialReasons.isEmpty(),
        List.copyOf(partialReasons),
        summary,
        monthlyRows(monthly),
        categoryRows(categoryTotals),
        topProductRows(sold),
        inventoryRows(inventory, asOf)
    );
  }

  private List<String> normalizeCategories(List<String> categories) {
    if (categories == null) return null;
    var cleaned = new ArrayList<String>();
    for (String category : categories) {
      if (category == null || category.trim().isEmpty()) continue;
      String trimmed = category.trim();
      cleaned.add(trimmed);
      if ("Autre".equalsIgnoreCase(trimmed)) {
        cleaned.add("Sans sous-categorie");
      }
    }
    cleaned = new ArrayList<>(cleaned.stream().distinct().toList());
    return cleaned.isEmpty() ? null : cleaned;
  }

  private boolean isAllCategories(List<String> categories) {
    return categories == null || categories.isEmpty();
  }

  private List<String> normalizeTypes(List<String> types) {
    if (types == null) return null;
    var cleaned = types.stream()
        .filter(t -> t != null && !t.trim().isEmpty())
        .map(snkVenteService::normalizeItemType)
        .distinct()
        .toList();
    return cleaned.isEmpty() ? null : cleaned;
  }

  private boolean isAllTypes(List<String> types) {
    return types == null || types.isEmpty();
  }

  private String[] toArray(List<String> categories) {
    if (categories == null || categories.isEmpty()) return new String[0];
    return categories.toArray(new String[0]);
  }

  private BigDecimal deltaPct(BigDecimal current, BigDecimal previous) {
    if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
    return current.subtract(previous)
        .divide(previous.abs(), 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
  }

  private BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal toBigDecimal(Double value) {
    if (value == null) return BigDecimal.ZERO;
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal money(BigDecimal value) {
    return nz(value).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal divide(BigDecimal numerator, BigDecimal denominator, int scale) {
    BigDecimal safeDenominator = nz(denominator);
    if (safeDenominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
    return nz(numerator).divide(safeDenominator, scale, RoundingMode.HALF_UP);
  }

  private String categoryName(SnkVente item) {
    String category = item.getCategorie();
    if (category != null && !category.trim().isEmpty()) {
      return category.trim();
    }
    return itemTypeDisplayName(item.getType());
  }

  private String itemTypeDisplayName(String type) {
    String normalized = snkVenteService.normalizeItemType(type);
    if ("POKEMON_CARD".equals(normalized)) return "Pokemon";
    if ("TICKET".equals(normalized)) return "Tickets";
    if ("OTHER".equals(normalized)) return "Autres";
    if ("SNEAKER".equals(normalized)) return "Sneakers";
    if ("CLOTHING".equals(normalized)) return "Vetements";
    if ("ACCESSORY".equals(normalized)) return "Accessoires";
    if ("WATCH".equals(normalized)) return "Montres";
    if ("ELECTRONICS".equals(normalized)) return "Electronique";
    if ("COLLECTIBLE".equals(normalized)) return "Collection";
    if ("HOME".equals(normalized)) return "Maison";
    String human = normalized.replace('_', ' ').toLowerCase(Locale.ROOT);
    if (human.isBlank()) return "Sneakers";
    return Normalizer.normalize(human, Normalizer.Form.NFC)
        .substring(0, 1)
        .toUpperCase(Locale.ROOT)
        + human.substring(1);
  }

  private String itemName(SnkVente item) {
    String name = item.getNomItem();
    if (name != null && !name.trim().isEmpty()) return name.trim();
    return "Article sans nom";
  }

  private void addReason(List<String> reasons, String reason) {
    if (reasons.size() >= 4 || reasons.contains(reason)) return;
    reasons.add(reason);
  }

  private List<AnnualDashboardResponse.AnnualDashboardMonth> monthlyRows(AnnualAccumulator[] monthly) {
    List<AnnualDashboardResponse.AnnualDashboardMonth> rows = new ArrayList<>();
    for (int i = 0; i < monthly.length; i++) {
      AnnualAccumulator row = monthly[i];
      rows.add(new AnnualDashboardResponse.AnnualDashboardMonth(
          i + 1,
          money(row.revenue),
          money(row.profit),
          money(row.purchaseSpend),
          row.count,
          row.itemsBought
      ));
    }
    return rows;
  }

  private List<AnnualDashboardResponse.AnnualDashboardCategory> categoryRows(
      Map<String, AnnualAccumulator> categoryTotals
  ) {
    List<Map.Entry<String, AnnualAccumulator>> sorted = categoryTotals.entrySet().stream()
        .sorted((a, b) -> b.getValue().profit.compareTo(a.getValue().profit))
        .toList();

    List<AnnualDashboardResponse.AnnualDashboardCategory> rows = new ArrayList<>();
    AnnualAccumulator others = new AnnualAccumulator();
    for (int i = 0; i < sorted.size(); i++) {
      Map.Entry<String, AnnualAccumulator> entry = sorted.get(i);
      if (i < 5) {
        AnnualAccumulator value = entry.getValue();
        rows.add(new AnnualDashboardResponse.AnnualDashboardCategory(
            entry.getKey(),
            money(value.revenue),
            money(value.profit),
            value.count
        ));
      } else {
        others.revenue = others.revenue.add(entry.getValue().revenue);
        others.profit = others.profit.add(entry.getValue().profit);
        others.count += entry.getValue().count;
      }
    }
    if (others.count > 0) {
      rows.add(new AnnualDashboardResponse.AnnualDashboardCategory(
          "Autres",
          money(others.revenue),
          money(others.profit),
          others.count
      ));
    }
    return rows;
  }

  private List<AnnualDashboardResponse.AnnualDashboardProduct> topProductRows(List<SnkVente> sold) {
    return sold.stream()
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing((SnkVente item) -> nz(item.getPrixResell()).subtract(nz(item.getPrixRetail()))).reversed())
        .limit(8)
        .map(item -> {
          BigDecimal purchasePrice = nz(item.getPrixRetail());
          BigDecimal salePrice = nz(item.getPrixResell());
          BigDecimal profit = salePrice.subtract(purchasePrice);
          return new AnnualDashboardResponse.AnnualDashboardProduct(
              item.getId(),
              itemName(item),
              categoryName(item),
              money(purchasePrice),
              money(salePrice),
              money(profit),
              divide(profit, purchasePrice, 4),
              item.getDateVente()
          );
        })
        .toList();
  }

  private List<AnnualDashboardResponse.AnnualDashboardInventoryItem> inventoryRows(
      List<SnkVente> inventory,
      LocalDate asOf
  ) {
    return inventory.stream()
        .filter(Objects::nonNull)
        .sorted(Comparator
            .comparing((SnkVente item) -> item.getDateAchat() == null ? Long.MIN_VALUE : ChronoUnit.DAYS.between(item.getDateAchat(), asOf))
            .reversed())
        .limit(8)
        .map(item -> new AnnualDashboardResponse.AnnualDashboardInventoryItem(
            item.getId(),
            itemName(item),
            categoryName(item),
            money(item.getPrixRetail()),
            item.getDateAchat(),
            item.getDateAchat() == null ? null : ChronoUnit.DAYS.between(item.getDateAchat(), asOf)
        ))
        .toList();
  }

  private static final class AnnualAccumulator {
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal profit = BigDecimal.ZERO;
    private BigDecimal cost = BigDecimal.ZERO;
    private BigDecimal purchaseSpend = BigDecimal.ZERO;
    private long count = 0;
    private long itemsBought = 0;
  }
}
