package backend.controller;

import backend.dto.NotificationActionResponse;
import backend.dto.NotificationCountResponse;
import backend.dto.NotificationPageResponse;
import backend.dto.NotificationResponse;
import backend.dto.NotificationSyncResponse;
import backend.entity.User;
import backend.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public NotificationPageResponse list(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "true") boolean unreadFirst
  ) {
    return notificationService.listForUser(userId(currentUser), page, size, unreadFirst);
  }

  @GetMapping("/unread-count")
  public NotificationCountResponse unreadCount(@AuthenticationPrincipal User currentUser) {
    return new NotificationCountResponse(notificationService.unreadCount(userId(currentUser)));
  }

  @PostMapping("/{id}/read")
  public NotificationResponse markRead(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Long id
  ) {
    return notificationService.markRead(userId(currentUser), id);
  }

  @PostMapping("/read-all")
  public NotificationActionResponse markAllRead(@AuthenticationPrincipal User currentUser) {
    int updated = notificationService.markAllRead(userId(currentUser));
    return new NotificationActionResponse(updated);
  }

  @PostMapping("/{id}/dismiss")
  public NotificationResponse dismiss(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Long id
  ) {
    return notificationService.dismiss(userId(currentUser), id);
  }

  @PostMapping("/sync")
  public NotificationSyncResponse sync(@AuthenticationPrincipal User currentUser) {
    int created = notificationService.syncForUser(userId(currentUser));
    return new NotificationSyncResponse(created);
  }

  private Long userId(User currentUser) {
    return currentUser.getId();
  }
}
