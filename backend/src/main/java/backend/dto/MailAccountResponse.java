package backend.dto;

import backend.entity.MailAccount;
import backend.entity.MailAccountStatus;
import backend.entity.MailProvider;
import java.time.OffsetDateTime;

public record MailAccountResponse(
    Long id,
    MailProvider provider,
    String emailAddress,
    String scopes,
    MailAccountStatus status,
    OffsetDateTime lastScanAt,
    OffsetDateTime nextScanAt,
    int errorCount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
  public static MailAccountResponse fromEntity(MailAccount account) {
    return new MailAccountResponse(
        account.getId(),
        account.getProvider(),
        account.getEmailAddress(),
        account.getScopes(),
        account.getStatus(),
        account.getLastScanAt(),
        account.getNextScanAt(),
        account.getErrorCount(),
        account.getCreatedAt(),
        account.getUpdatedAt()
    );
  }
}
