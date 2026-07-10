package backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdministrativeSummaryResponse(
    LocalDate periodStart,
    LocalDate periodEnd,
    int year,
    AdministrativeProfileResponse profile,
    BigDecimal periodRevenue,
    BigDecimal annualRevenue,
    long periodSaleCount,
    long annualSaleCount,
    long incompleteSaleCount,
    long missingSaleDateCount,
    long missingSaleAmountCount,
    long missingPurchaseAmountCount,
    long missingProofCount,
    long periodMismatchCount,
    long missingInvoiceCount,
    long periodPurchaseCount,
    BigDecimal periodPurchaseTotal,
    String revenueRule,
    String individualDiagnosticLevel,
    String individualDiagnosticLabel,
    String individualDiagnosticMessage,
    List<AdministrativePeriodBucketResponse> periodBreakdown,
    List<AdministrativeSaleLineResponse> sales,
    List<AdministrativeSaleLineResponse> purchases,
    List<AdministrativeObligationResponse> obligations,
    List<AdministrativeAlertResponse> alerts,
    List<AdministrativeQualityCheckResponse> qualityChecks,
    List<AdministrativeSourceResponse> sources
) {}
