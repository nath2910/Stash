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

  @Modifying
  @Query(value = """
      delete from public.notifications n
      where n.user_id = :userId
        and n.type = 'STOCK_AGING'
        and n.entity_type = 'STOCK_ITEM'
        and (
          n.entity_id is null
          or not exists (
            select 1
            from public.tableauventes v
            where v.id = n.entity_id
              and v.user_id = :userId
              and v.date_achat is not null
          )
        )
      """, nativeQuery = true)
  int deleteInvalidStockAgingNotificationsForUser(@Param("userId") Long userId);

  @Modifying
  @Query(value = """
      delete from public.notifications n
      where n.user_id = :userId
        and n.type = 'STOCK_AGING'
        and n.entity_type = 'STOCK_ITEM'
        and n.entity_id = :entityId
        and n.milestone_key like concat(:milestonePrefix, '%')
        and n.milestone_key <> :keepMilestoneKey
      """, nativeQuery = true)
  int deleteStockAgingMilestoneVariants(
      @Param("userId") Long userId,
      @Param("entityId") Long entityId,
      @Param("milestonePrefix") String milestonePrefix,
      @Param("keepMilestoneKey") String keepMilestoneKey
  );
}
