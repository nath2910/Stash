package backend.dto;

import java.time.LocalDate;
import java.util.List;

public record MonthlyDashboardResponse(
    int year,
    int month,
    LocalDate from,
    LocalDate to,
    StatsSummaryResponse summary,
    StatsSummaryResponse previousSummary,
    List<StatsPointResponse> timeseries,
    List<StatsBreakdownResponse> brands,
    List<TopVenteProjection> topSales,
    List<StatsLabelValueResponse> categoryProfit,
    List<AnnualDashboardResponse.AnnualDashboardMonth> annualMonths
) {}
