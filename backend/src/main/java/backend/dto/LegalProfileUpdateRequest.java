package backend.dto;

public record LegalProfileUpdateRequest(
    String legalProfileType,
    String siret,
    String taxCategory,
    String businessRegime,
    String businessActivityType,
    String declaredRevenueThreshold,
    String vatNumber,
    String vatStatus,
    String declarationFrequency
) {}
