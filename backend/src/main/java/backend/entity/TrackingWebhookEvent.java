package backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
@Table(name = "tracking_webhook_events", schema = "public")
@Getter
@Setter
public class TrackingWebhookEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "aggregator", nullable = false, length = 64)
  private String aggregator;

  @Column(name = "external_event_id", length = 255)
  private String externalEventId;

  @Column(name = "signature_valid", nullable = false)
  private boolean signatureValid;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload = new HashMap<>();

  @Column(name = "processed_at")
  private OffsetDateTime processedAt;

  @Column(name = "received_at", nullable = false)
  private OffsetDateTime receivedAt;

  @PrePersist
  void prePersist() {
    receivedAt = receivedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : receivedAt;
  }
}
