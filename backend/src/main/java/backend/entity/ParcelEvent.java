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
@Table(name = "parcel_events", schema = "public")
@Getter
@Setter
public class ParcelEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parcel_id", nullable = false)
  private Parcel parcel;

  @Column(name = "event_hash", nullable = false, length = 128)
  private String eventHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private ParcelStatus status;

  @Column(name = "substatus", length = 120)
  private String substatus;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "location", length = 255)
  private String location;

  @Column(name = "event_time", nullable = false)
  private OffsetDateTime eventTime;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_payload", columnDefinition = "jsonb")
  private Map<String, Object> rawPayload = new HashMap<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void prePersist() {
    createdAt = createdAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : createdAt;
  }
}
