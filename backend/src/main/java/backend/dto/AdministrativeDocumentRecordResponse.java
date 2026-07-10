package backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AdministrativeDocumentRecordResponse(
    String id,
    Long userId,
    String profileId,
    String type,
    String title,
    LocalDate periodStart,
    LocalDate periodEnd,
    String status,
    String filePath,
    Long fileSize,
    String mimeType,
    String checksum,
    BigDecimal amount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
