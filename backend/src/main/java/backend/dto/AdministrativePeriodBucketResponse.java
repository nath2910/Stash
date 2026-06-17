package backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdministrativePeriodBucketResponse(
    String label,
    LocalDate start,
    LocalDate end,
    BigDecimal revenue,
    long saleCount
) {}
