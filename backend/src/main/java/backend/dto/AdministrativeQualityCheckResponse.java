package backend.dto;

public record AdministrativeQualityCheckResponse(
    String id,
    String severity,
    String title,
    String message,
    long count,
    String status,
    String actionLabel,
    String target
) {}
