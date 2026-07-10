package backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdministrativeDeclarationRecordRequest(
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal amount,
    String title,
    String profileId
) {}
