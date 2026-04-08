package backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationGenerationJob {

  private static final Logger log = LoggerFactory.getLogger(NotificationGenerationJob.class);

  private final NotificationService notificationService;

  public NotificationGenerationJob(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Scheduled(cron = "${app.notifications.generation-cron:0 15 3 * * *}", zone = "UTC")
  public void generateDaily() {
    int created = notificationService.generateTimedNotificationsForAllUsers();
    log.info("Notification generation done: {} notifications created", created);
  }
}
