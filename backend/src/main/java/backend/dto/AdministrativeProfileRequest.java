package backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdministrativeProfileRequest(
    String profileType,
    String legalStatus,
    List<String> activities,
    String vatRegime,
    List<String> declarations,
    String siret,
    String businessName,
    String ownerName,
    String displayName,
    String tradeName,
    String address,
    String legalForm,
    String mainActivity,
    String fiscalRegime,
    String declarationFrequency,
    String declarationPeriodicity,
    String withholdingTaxOption,
    String vatFranchise,
    LocalDate activityStartDate,
    Integer fiscalYearEndMonth,
    Integer fiscalYearEndDay,
    String urssafCategory,
    BigDecimal defaultVatRate,
    String notes,
    String verificationStatus,
    Boolean usesOnlinePlatforms,
    Boolean buysForResale
) {}
