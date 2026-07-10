package backend.dto;

public record AdministrativeObligationResponse(
    String id,
    String category,
    String title,
    String description,
    String status,
    String severity,
    String actionLabel,
    String documentType
) {}
