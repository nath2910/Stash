package backend.service;

import backend.entity.ParcelStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CarrierStatusResolverTest {

  @Test
  void resolvesOutForDeliveryAsDedicatedStatus() {
    Assertions.assertEquals(
        ParcelStatus.OUT_FOR_DELIVERY,
        CarrierStatusResolver.resolve("Your package is out for delivery")
    );
    Assertions.assertEquals(
        ParcelStatus.OUT_FOR_DELIVERY,
        CarrierStatusResolver.resolve("Votre colis est en cours de livraison")
    );
  }
}
