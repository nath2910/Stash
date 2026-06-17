package backend.dto;

import backend.entity.BusinessActivityType;
import backend.entity.BusinessRegime;
import backend.entity.DeclarationFrequency;
import backend.entity.DeclaredRevenueThreshold;
import backend.entity.LegalProfileType;
import backend.entity.TaxCategory;
import backend.entity.VatStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record LegalProfileResponse(
    LegalProfileType legalProfileType,
    String siret,
    String siren,
    TaxCategory taxCategory,
    BusinessRegime businessRegime,
    BusinessActivityType businessActivityType,
    DeclaredRevenueThreshold declaredRevenueThreshold,
    String vatNumber,
    VatStatus vatStatus,
    DeclarationFrequency declarationFrequency,
    String withholdingTaxOption,
    String vatFranchise,
    LocalDate activityStartDate,
    boolean completed,
    OffsetDateTime updatedAt
) {}
