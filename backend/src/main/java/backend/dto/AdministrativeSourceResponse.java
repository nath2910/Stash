package backend.dto;

public record AdministrativeSourceResponse(
    String label,
    String url,
    String lastVerified
) {}
