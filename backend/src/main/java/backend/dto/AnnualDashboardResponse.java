package backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnnualDashboardResponse(
    int year,
    LocalDate asOf,
    boolean hasData,
    boolean partialData,
    List<String> partialDataReasons,
    AnnualDashboardSummary summary,
    List<AnnualDashboardMonth> monthly,
    List<AnnualDashboardCategory> topCategories,
    List<AnnualDashboardProduct> topProducts,
    List<AnnualDashboardInventoryItem> inventoryAging
) {

  public record AnnualDashboardSummary(
      BigDecimal revenue,
      BigDecimal profit,
      BigDecimal marginRate,
      BigDecimal roi,
      long itemsSold,
      BigDecimal averageSalePrice,
      BigDecimal averageProfit,
      BigDecimal purchaseSpend,
      long itemsBought,
      long remainingStockCount,
      BigDecimal remainingStockValue,
      BigDecimal sellThroughRate,
      BigDecimal averageHoldDays
  ) {}

  public record AnnualDashboardMonth(
      int month,
      BigDecimal revenue,
      BigDecimal profit,
      BigDecimal purchaseSpend,
      long itemsSold,
      long itemsBought
  ) {}

  public record AnnualDashboardCategory(
      String name,
      BigDecimal revenue,
      BigDecimal profit,
      long count
  ) {}

  public record AnnualDashboardProduct(
      Integer id,
      String name,
      String category,
      BigDecimal purchasePrice,
      BigDecimal salePrice,
      BigDecimal profit,
      BigDecimal roi,
      LocalDate soldAt
  ) {}

  public record AnnualDashboardInventoryItem(
      Integer id,
      String name,
      String category,
      BigDecimal purchasePrice,
      LocalDate purchasedAt,
      Long ageInDays
  ) {}
}
