package backend.dto;

import java.time.OffsetDateTime;

public record UserMeResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String pictureUrl,
    String provider,
    boolean emailVerified,
    String subscriptionStatus,
    OffsetDateTime subscriptionCurrentPeriodEnd,
    boolean legalProfileCompleted,
    LegalProfileResponse legalProfile
) {}
