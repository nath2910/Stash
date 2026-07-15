package backend.service;

import backend.dto.ParcelResponse;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeliveryRefreshServiceTest {

  @Test
  void refreshAllRefreshesParcelsWithoutTriggeringMailScan() {
    DeliveryTrackingService deliveryTrackingService = Mockito.mock(DeliveryTrackingService.class);
    List<ParcelResponse> expected = List.of();
    Mockito.when(deliveryTrackingService.refreshAllForUser(1L)).thenReturn(expected);

    DeliveryRefreshService service = new DeliveryRefreshService(deliveryTrackingService);

    List<ParcelResponse> result = service.refreshAllForUser(1L);

    Mockito.verify(deliveryTrackingService).refreshAllForUser(1L);
    Assertions.assertSame(expected, result);
  }
}
