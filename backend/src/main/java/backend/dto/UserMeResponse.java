package backend.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record UserMeResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    LocalDateTime createdAt,
    String pictureUrl,
    String provider,
    boolean emailVerified,
    String subscriptionStatus,
    OffsetDateTime subscriptionCurrentPeriodEnd,
    boolean legalProfileCompleted,
    LegalProfileResponse legalProfile
) {}
