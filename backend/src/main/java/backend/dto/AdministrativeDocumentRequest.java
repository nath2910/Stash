package backend.dto;

import java.time.LocalDate;

public record AdministrativeDocumentRequest(
    LocalDate periodStart,
    LocalDate periodEnd,
    Integer year
) {}
