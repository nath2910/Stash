package backend.dto;

import java.time.LocalDate;

public record AdministrativeProfileRequest(
    String profileType,
    String siret,
    String displayName,
    String tradeName,
    String address,
    String mainActivity,
    String fiscalRegime,
    String declarationFrequency,
    String withholdingTaxOption,
    String vatFranchise,
    LocalDate activityStartDate,
    String notes,
    String verificationStatus,
    Boolean usesOnlinePlatforms,
    Boolean buysForResale
) {}
