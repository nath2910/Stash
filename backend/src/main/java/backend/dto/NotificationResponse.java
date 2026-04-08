package backend.dto;

import backend.entity.Notification;
import java.time.OffsetDateTime;

public record NotificationResponse(
    Long id,
    String type,
    String title,
    String message,
    String severity,
    String entityType,
    Long entityId,
    String milestoneKey,
    boolean isRead,
    OffsetDateTime readAt,
    OffsetDateTime dismissedAt,
    OffsetDateTime createdAt,
    String ctaRoute,
    String ctaLabel
) {

  public static NotificationResponse fromEntity(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getType() != null ? notification.getType().name() : null,
        notification.getTitle(),
        notification.getMessage(),
        notification.getSeverity() != null ? notification.getSeverity().name() : null,
        notification.getEntityType() != null ? notification.getEntityType().name() : null,
        notification.getEntityId(),
        notification.getMilestoneKey(),
        notification.isRead(),
        notification.getReadAt(),
        notification.getDismissedAt(),
        notification.getCreatedAt(),
        notification.getCtaRoute(),
        notification.getCtaLabel()
    );
  }
}
