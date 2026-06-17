package backend.dto;

public record AdministrativeDocumentDescriptorResponse(
    String id,
    String title,
    String description,
    String endpoint,
    String format,
    String primaryProfile
) {}
