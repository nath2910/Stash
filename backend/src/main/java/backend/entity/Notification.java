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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notifications", schema = "public")
@Getter
@Setter
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 64)
  private NotificationType type;

  @Column(name = "title", nullable = false, length = 180)
  private String title;

  @Column(name = "message", nullable = false, length = 500)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(name = "severity", nullable = false, length = 16)
  private NotificationSeverity severity = NotificationSeverity.INFO;

  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type", nullable = false, length = 32)
  private NotificationEntityType entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "milestone_key", nullable = false, length = 120)
  private String milestoneKey;

  @Column(name = "cta_route", length = 160)
  private String ctaRoute;

  @Column(name = "cta_label", length = 80)
  private String ctaLabel;

  @Column(name = "is_read", nullable = false)
  private boolean isRead;

  @Column(name = "read_at")
  private OffsetDateTime readAt;

  @Column(name = "dismissed_at")
  private OffsetDateTime dismissedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
