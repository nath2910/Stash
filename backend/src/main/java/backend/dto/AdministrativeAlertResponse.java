package backend.dto;

public record AdministrativeAlertResponse(
    String severity,
    String title,
    String message
) {}
