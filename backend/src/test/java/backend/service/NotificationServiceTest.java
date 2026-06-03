package backend.service;

import backend.dto.NotificationPageResponse;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class NotificationServiceTest {

  private NotificationRepository notificationRepository;
  private UserRepository userRepository;
  private SnkVenteRepository snkVenteRepository;
  private NotificationService service;
  private User user;

  @BeforeEach
  void setup() {
    notificationRepository = Mockito.mock(NotificationRepository.class);
    userRepository = Mockito.mock(UserRepository.class);
    snkVenteRepository = Mockito.mock(SnkVenteRepository.class);
    service = new NotificationService(notificationRepository, userRepository, snkVenteRepository);

    user = Mockito.mock(User.class);
    Mockito.when(user.getId()).thenReturn(1L);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
  }

  @Test
  void stockAgingNotificationsCoverGenericInventoryItems() {
    LocalDate purchaseDate = LocalDate.now(ZoneOffset.UTC).minusMonths(13);
    SnkVente watch = item(10, "Montre vintage", "MONTRES", purchaseDate);
    SnkVente console = item(11, "Console retro", "ELECTRONIQUE", purchaseDate);

    Mockito.when(snkVenteRepository.findInStockCandidatesByUserId(1L)).thenReturn(List.of(watch, console));
    Mockito.when(notificationRepository.existsByUser_IdAndTypeAndEntityTypeAndEntityIdAndMilestoneKey(
        Mockito.anyLong(),
        Mockito.any(NotificationType.class),
        Mockito.any(NotificationEntityType.class),
        Mockito.anyLong(),
        Mockito.anyString()
    )).thenReturn(false);
    Mockito.when(notificationRepository.saveAndFlush(Mockito.any(Notification.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    int created = service.generateTimedNotificationsForUser(1L);

    Assertions.assertEquals(4, created);
    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    Mockito.verify(notificationRepository, Mockito.times(4)).saveAndFlush(captor.capture());

    List<Notification> saved = captor.getAllValues();
    Assertions.assertTrue(saved.stream().allMatch(n -> n.getType() == NotificationType.STOCK_AGING));
    Assertions.assertTrue(saved.stream().allMatch(n -> n.getEntityType() == NotificationEntityType.STOCK_ITEM));
    Assertions.assertTrue(saved.stream().allMatch(n -> "/gestion".equals(n.getCtaRoute())));
    Assertions.assertTrue(saved.stream().allMatch(n -> "Voir le stock".equals(n.getCtaLabel())));
    Assertions.assertTrue(saved.stream().anyMatch(n -> n.getMessage().contains("Montre vintage")));
    Assertions.assertTrue(saved.stream().anyMatch(n -> n.getMessage().contains("Console retro")));
    Assertions.assertTrue(saved.stream().noneMatch(n -> n.getMessage().toLowerCase().contains("sneaker")));
  }

  @Test
  void listForUserIgnoresDismissedNotificationsInRepositoryCalls() {
    Notification notification = new Notification();
    notification.setId(20L);
    notification.setType(NotificationType.STOCK_AGING);
    notification.setEntityType(NotificationEntityType.STOCK_ITEM);
    notification.setSeverity(NotificationSeverity.INFO);
    notification.setMilestoneKey("STOCK_6M_ITEM_10");
    notification.setTitle("Item en stock");
    notification.setMessage("Un item demande une action.");
    notification.setRead(false);

    Mockito.when(notificationRepository.findByUserIdUnreadFirst(Mockito.eq(1L), Mockito.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(notification)));
    Mockito.when(notificationRepository.countByUser_IdAndIsReadFalseAndDismissedAtIsNull(1L))
        .thenReturn(1L);

    NotificationPageResponse response = service.listForUser(1L, 0, 20, true);

    Assertions.assertEquals(1, response.items().size());
    Assertions.assertEquals(1, response.unreadCount());
    Mockito.verify(notificationRepository).findByUserIdUnreadFirst(
        Mockito.eq(1L),
        Mockito.any(Pageable.class)
    );
    Mockito.verify(notificationRepository).countByUser_IdAndIsReadFalseAndDismissedAtIsNull(1L);
  }

  @Test
  void markReadAlsoDismissesNotification() {
    Notification notification = new Notification();
    notification.setId(30L);
    notification.setType(NotificationType.STOCK_AGING);
    notification.setEntityType(NotificationEntityType.STOCK_ITEM);
    notification.setSeverity(NotificationSeverity.INFO);
    notification.setMilestoneKey("STOCK_6M_ITEM_30");
    notification.setTitle("Item en stock");
    notification.setMessage("Un item demande une action.");
    notification.setRead(false);

    Mockito.when(notificationRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(notification));

    var response = service.markRead(1L, 30L);

    Assertions.assertTrue(response.isRead());
    Assertions.assertNotNull(response.readAt());
    Assertions.assertNotNull(response.dismissedAt());
    Assertions.assertTrue(notification.isRead());
    Assertions.assertNotNull(notification.getDismissedAt());
  }

  private SnkVente item(Integer id, String name, String type, LocalDate purchaseDate) {
    SnkVente item = new SnkVente();
    item.setId(id);
    item.setNomItem(name);
    item.setType(type);
    item.setDateAchat(purchaseDate);
    return item;
  }
}
