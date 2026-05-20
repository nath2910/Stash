package backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mail_accounts", schema = "public")
@Getter
@Setter
public class MailAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 32)
  private MailProvider provider;

  @Column(name = "provider_account_id", nullable = false, length = 255)
  private String providerAccountId;

  @Column(name = "email_address", nullable = false, length = 320)
  private String emailAddress;

  @Column(name = "scopes", nullable = false, columnDefinition = "text")
  private String scopes;

  @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "text")
  private String encryptedRefreshToken;

  @Column(name = "encrypted_access_token", columnDefinition = "text")
  private String encryptedAccessToken;

  @Column(name = "access_token_expires_at")
  private OffsetDateTime accessTokenExpiresAt;

  @Column(name = "scan_cursor", columnDefinition = "text")
  private String scanCursor;

  @Column(name = "last_scan_at")
  private OffsetDateTime lastScanAt;

  @Column(name = "next_scan_at", nullable = false)
  private OffsetDateTime nextScanAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private MailAccountStatus status = MailAccountStatus.ACTIVE;

  @Column(name = "error_count", nullable = false)
  private int errorCount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    createdAt = now;
    updatedAt = now;
    if (nextScanAt == null) {
      nextScanAt = now;
    }
    if (status == null) {
      status = MailAccountStatus.ACTIVE;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
