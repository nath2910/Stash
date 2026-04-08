package backend.service;

import backend.dto.NotificationPageResponse;
import backend.dto.NotificationResponse;
import backend.entity.Notification;
import backend.entity.NotificationEntityType;
import backend.entity.NotificationSeverity;
import backend.entity.NotificationType;
import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.NotificationRepository;
import backend.repository.SnkVenteRepository;
import backend.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private static final int MAX_PAGE_SIZE = 50;
  private static final List<Integer> SUBSCRIPTION_MILESTONES_DAYS = List.of(7, 3, 1);
  private static final List<Integer> STOCK_MILESTONES_MONTHS = List.of(6, 12);
  private static final Set<String> SUBSCRIPTION_ELIGIBLE_STATUSES =
      Set.of("active", "past_due", "canceled", "trialing");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final SnkVenteRepository snkVenteRepository;

  public NotificationService(
      NotificationRepository notificationRepository,
      UserRepository userRepository,
      SnkVenteRepository snkVenteRepository
  ) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.snkVenteRepository = snkVenteRepository;
  }

  @Transactional(readOnly = true)
  public NotificationPageResponse listForUser(Long userId, int page, int size, boolean unreadFirst) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    Pageable pageable = PageRequest.of(safePage, safeSize);

    Page<Notification> result = unreadFirst
        ? notificationRepository.findByUserIdUnreadFirst(userId, pageable)
        : notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);

    List<NotificationResponse> items = result.getContent().stream()
        .map(NotificationResponse::fromEntity)
        .toList();

    long unreadCount = notificationRepository.countByUser_IdAndIsReadFalse(userId);

    return new NotificationPageResponse(
        items,
        safePage,
        safeSize,
        result.getTotalElements(),
        result.hasNext(),
        unreadCount
    );
  }

  @Transactional(readOnly = true)
  public long unreadCount(Long userId) {
    return notificationRepository.countByUser_IdAndIsReadFalse(userId);
  }

  @Transactional
  public NotificationResponse markRead(Long userId, Long notificationId) {
    Notification notification = getForUserOrThrow(userId, notificationId);
    if (!notification.isRead()) {
      notification.setRead(true);
      notification.setReadAt(OffsetDateTime.now(ZoneOffset.UTC));
    }
    return NotificationResponse.fromEntity(notification);
  }

  @Transactional
  public int markAllRead(Long userId) {
    return notificationRepository.markAllRead(userId, OffsetDateTime.now(ZoneOffset.UTC));
  }

  @Transactional
  public NotificationResponse dismiss(Long userId, Long notificationId) {
    Notification notification = getForUserOrThrow(userId, notificationId);
    notification.setDismissedAt(OffsetDateTime.now(ZoneOffset.UTC));
    if (!notification.isRead()) {
      notification.setRead(true);
      notification.setReadAt(OffsetDateTime.now(ZoneOffset.UTC));
    }
    return NotificationResponse.fromEntity(notification);
  }

  @Transactional
  public int syncForUser(Long userId) {
    return generateTimedNotificationsForUser(userId);
  }

  @Transactional
  public int generateTimedNotificationsForUser(Long userId) {
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      return 0;
    }

    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    int created = 0;
    created += generateSubscriptionExpiringNotifications(user, today);
    created += generateStockAgingNotifications(user, today);
    return created;
  }

  public int generateTimedNotificationsForAllUsers() {
    List<Long> userIds = userRepository.findAllUserIds();
    int totalCreated = 0;

    for (Long userId : userIds) {
      try {
        totalCreated += generateTimedNotificationsForUser(userId);
      } catch (Exception ex) {
        log.warn("Notification generation failed for user {}", userId, ex);
      }
    }

    return totalCreated;
  }

  private int generateSubscriptionExpiringNotifications(User user, LocalDate today) {
    String status = user.getSubscriptionStatus();
    if (status == null || !SUBSCRIPTION_ELIGIBLE_STATUSES.contains(status.toLowerCase(Locale.ROOT))) {
      return 0;
    }

    if (user.getSubscriptionCurrentPeriodEnd() == null) {
      return 0;
    }

    LocalDate endDate = user.getSubscriptionCurrentPeriodEnd()
        .withOffsetSameInstant(ZoneOffset.UTC)
        .toLocalDate();

    long daysUntilEnd = ChronoUnit.DAYS.between(today, endDate);
    if (daysUntilEnd < 0) {
      return 0;
    }

    int created = 0;
    for (int milestone : SUBSCRIPTION_MILESTONES_DAYS) {
      if (daysUntilEnd > milestone) {
        continue;
      }

      String milestoneKey = "SUB_EXP_J" + milestone + "_" + endDate;
      String title = "Abonnement: rappel J-" + milestone;
      String message = "Votre abonnement expire le " + endDate.format(DATE_FORMAT)
          + " (" + remainingLabel(daysUntilEnd) + ")."
          + " Renouvelez pour conserver l'acces premium.";

      created += createNotificationIfAbsent(
          user,
          NotificationType.SUBSCRIPTION_EXPIRING,
          NotificationSeverity.WARNING,
          NotificationEntityType.SUBSCRIPTION,
          null,
          milestoneKey,
          title,
          message,
          "/abo",
          "Renouveler"
      );
    }

    return created;
  }

  private int generateStockAgingNotifications(User user, LocalDate today) {
    List<SnkVente> inStockItems = snkVenteRepository.findInStockCandidatesByUserId(user.getId());
    if (inStockItems.isEmpty()) {
      return 0;
    }

    int created = 0;
    List<SnkVente> ordered = inStockItems.stream()
        .sorted(Comparator.comparing(SnkVente::getId))
        .toList();

    for (SnkVente item : ordered) {
      LocalDate referenceDate = resolveStockReferenceDate(item);
      if (referenceDate == null || referenceDate.isAfter(today)) {
        continue;
      }

      long monthsInStock = ChronoUnit.MONTHS.between(referenceDate, today);
      for (int milestone : STOCK_MILESTONES_MONTHS) {
        if (monthsInStock < milestone) {
          continue;
        }

        String milestoneKey = "STOCK_" + milestone + "M_ITEM_" + item.getId();
        String itemName = (item.getNomItem() == null || item.getNomItem().isBlank())
            ? "Un item de votre stock"
            : item.getNomItem();

        String title = "Item en stock depuis " + milestone + " mois";
        String message = itemName + " est en stock depuis le " + referenceDate.format(DATE_FORMAT)
            + ". Pensez a ajuster votre strategie de vente.";

        created += createNotificationIfAbsent(
            user,
            NotificationType.STOCK_AGING,
            NotificationSeverity.INFO,
            NotificationEntityType.STOCK_ITEM,
            item.getId() != null ? item.getId().longValue() : null,
            milestoneKey,
            title,
            message,
            "/gestion",
            "Voir le stock"
        );
      }
    }

    return created;
  }

  private LocalDate resolveStockReferenceDate(SnkVente item) {
    if (item.getDateAchat() != null) {
      return item.getDateAchat();
    }

    if (item.getCreatedAt() != null) {
      // Hypothese metier: si date d'achat absente, on utilise created_at comme date d'entree en stock.
      return item.getCreatedAt().atOffset(ZoneOffset.UTC).toLocalDate();
    }

    return null;
  }

  private int createNotificationIfAbsent(
      User user,
      NotificationType type,
      NotificationSeverity severity,
      NotificationEntityType entityType,
      Long entityId,
      String milestoneKey,
      String title,
      String message,
      String ctaRoute,
      String ctaLabel
  ) {
    boolean exists = notificationRepository.existsByUser_IdAndTypeAndEntityTypeAndEntityIdAndMilestoneKey(
        user.getId(),
        type,
        entityType,
        entityId,
        milestoneKey
    );

    if (exists) {
      return 0;
    }

    Notification notification = new Notification();
    notification.setUser(user);
    notification.setType(type);
    notification.setSeverity(severity);
    notification.setEntityType(entityType);
    notification.setEntityId(entityId);
    notification.setMilestoneKey(milestoneKey);
    notification.setTitle(title);
    notification.setMessage(message);
    notification.setCtaRoute(ctaRoute);
    notification.setCtaLabel(ctaLabel);
    notification.setRead(false);

    try {
      notificationRepository.saveAndFlush(notification);
      return 1;
    } catch (DataIntegrityViolationException ex) {
      // Protection anti-doublon en course concurrente grace a l'index unique.
      return 0;
    }
  }

  private Notification getForUserOrThrow(Long userId, Long notificationId) {
    return notificationRepository.findByIdAndUser_Id(notificationId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable"));
  }

  private String remainingLabel(long daysUntilEnd) {
    if (daysUntilEnd <= 0) {
      return "expiration aujourd'hui";
    }
    if (daysUntilEnd == 1) {
      return "1 jour restant";
    }
    return daysUntilEnd + " jours restants";
  }
}
