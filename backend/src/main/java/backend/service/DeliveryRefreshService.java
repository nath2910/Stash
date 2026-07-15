package backend.service;

import backend.dto.ParcelResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeliveryRefreshService {

  private final DeliveryTrackingService deliveryTrackingService;

  public DeliveryRefreshService(DeliveryTrackingService deliveryTrackingService) {
    this.deliveryTrackingService = deliveryTrackingService;
  }

  public List<ParcelResponse> refreshAllForUser(Long userId) {
    // Keep manual parcel refresh focused on carrier tracking only.
    // Gmail scanning already has its own dedicated actions and should not slow this endpoint down.
    return deliveryTrackingService.refreshAllForUser(userId);
  }
}
