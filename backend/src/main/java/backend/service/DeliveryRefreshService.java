package backend.service;

import backend.dto.ParcelResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeliveryRefreshService {

  private final DeliveryTrackingService deliveryTrackingService;
  private final MailAccountService mailAccountService;

  public DeliveryRefreshService(
      DeliveryTrackingService deliveryTrackingService,
      MailAccountService mailAccountService
  ) {
    this.deliveryTrackingService = deliveryTrackingService;
    this.mailAccountService = mailAccountService;
  }

  public List<ParcelResponse> refreshAllForUser(Long userId) {
    // Refresh mail-derived hints first, then reconcile parcel statuses with direct carrier checks.
    mailAccountService.scanAll(userId);
    return deliveryTrackingService.refreshAllForUser(userId);
  }
}
