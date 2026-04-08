package backend.repository;

import backend.entity.Notification;
import backend.entity.NotificationEntityType;
import backend.entity.NotificationType;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  @Query("""
      select n
      from Notification n
      where n.user.id = :userId
      order by n.isRead asc, n.createdAt desc
      """)
  Page<Notification> findByUserIdUnreadFirst(@Param("userId") Long userId, Pageable pageable);

  Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  long countByUser_IdAndIsReadFalse(Long userId);

  Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

  boolean existsByUser_IdAndTypeAndEntityTypeAndEntityIdAndMilestoneKey(
      Long userId,
      NotificationType type,
      NotificationEntityType entityType,
      Long entityId,
      String milestoneKey
  );

  @Modifying
  @Query("""
      update Notification n
      set n.isRead = true,
          n.readAt = :readAt
      where n.user.id = :userId
        and n.isRead = false
      """)
  int markAllRead(@Param("userId") Long userId, @Param("readAt") OffsetDateTime readAt);
}
