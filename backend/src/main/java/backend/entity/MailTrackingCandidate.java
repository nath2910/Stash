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
@Table(name = "mail_tracking_candidates", schema = "public")
@Getter
@Setter
public class MailTrackingCandidate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mail_account_id")
  private MailAccount mailAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parcel_id")
  private Parcel parcel;

  @Column(name = "dedupe_key", nullable = false, length = 512)
  private String dedupeKey;

  @Column(name = "provider_message_id", length = 255)
  private String providerMessageId;

  @Column(name = "source_sender", length = 500)
  private String sourceSender;

  @Column(name = "source_subject", length = 500)
  private String sourceSubject;

  @Column(name = "received_at")
  private OffsetDateTime receivedAt;

  @Column(name = "tracking_number", nullable = false, length = 80)
  private String trackingNumber;

  @Column(name = "normalized_tracking_number", nullable = false, length = 80)
  private String normalizedTrackingNumber;

  @Column(name = "carrier_slug", length = 80)
  private String carrierSlug;

  @Column(name = "tracking_url", columnDefinition = "text")
  private String trackingUrl;

  @Column(name = "merchant_name", length = 255)
  private String merchantName;

  @Column(name = "raw_status", length = 120)
  private String rawStatus;

  @Column(name = "context_snippet", length = 700)
  private String contextSnippet;

  @Column(name = "confidence_score", nullable = false)
  private int confidenceScore;

  @Column(name = "confidence_level", nullable = false, length = 20)
  private String confidenceLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "candidate_status", nullable = false, length = 40)
  private TrackingCandidateStatus status = TrackingCandidateStatus.NEEDS_REVIEW;

  @Column(name = "reason", length = 700)
  private String reason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    createdAt = now;
    updatedAt = now;
    if (status == null) {
      status = TrackingCandidateStatus.NEEDS_REVIEW;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
