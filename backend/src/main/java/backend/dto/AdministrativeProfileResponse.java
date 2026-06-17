package backend.dto;

import backend.entity.DeclarationFrequency;
import backend.entity.LegalProfileType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AdministrativeProfileResponse(
    LegalProfileType profileType,
    String siret,
    String siren,
    String displayName,
    String tradeName,
    String address,
    String mainActivity,
    String fiscalRegime,
    DeclarationFrequency declarationFrequency,
    String withholdingTaxOption,
    String vatFranchise,
    LocalDate activityStartDate,
    String notes,
    String verificationStatus,
    boolean usesOnlinePlatforms,
    boolean buysForResale,
    boolean completed,
    OffsetDateTime updatedAt,
    List<String> missingFields
) {}
