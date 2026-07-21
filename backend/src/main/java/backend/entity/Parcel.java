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
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "parcels", schema = "public")
@Getter
@Setter
public class Parcel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mail_account_id")
  private MailAccount mailAccount;

  @Column(name = "tracking_number", nullable = false, length = 80)
  private String trackingNumber;

  @Column(name = "normalized_tracking_number", nullable = false, length = 80)
  private String normalizedTrackingNumber;

  @Column(name = "carrier_slug", length = 80)
  private String carrierSlug;

  @Column(name = "aggregator", nullable = false, length = 64)
  private String aggregator = "DIRECT_CARRIER";

  @Column(name = "aggregator_tracking_id", length = 255)
  private String aggregatorTrackingId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private ParcelStatus status = ParcelStatus.PENDING;

  @Column(name = "status_label", length = 255)
  private String statusLabel;

  @Column(name = "estimated_delivery_at")
  private OffsetDateTime estimatedDeliveryAt;

  @Column(name = "delivered_at")
  private OffsetDateTime deliveredAt;

  @Column(name = "source_provider_message_id", length = 255)
  private String sourceProviderMessageId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_current_payload", columnDefinition = "jsonb")
  private Map<String, Object> rawCurrentPayload = new HashMap<>();

  @Column(name = "first_seen_at", nullable = false)
  private OffsetDateTime firstSeenAt;

  @Column(name = "last_event_at")
  private OffsetDateTime lastEventAt;

  @Column(name = "last_tracking_refresh_at")
  private OffsetDateTime lastTrackingRefreshAt;

  @Column(name = "next_tracking_refresh_at")
  private OffsetDateTime nextTrackingRefreshAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  void prePersist() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    firstSeenAt = firstSeenAt == null ? now : firstSeenAt;
    updatedAt = now;
    if (status == null) {
      status = ParcelStatus.PENDING;
    }
    if (aggregator == null || aggregator.isBlank()) {
      aggregator = "DIRECT_CARRIER";
    }
    if (nextTrackingRefreshAt == null) {
      nextTrackingRefreshAt = now;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
