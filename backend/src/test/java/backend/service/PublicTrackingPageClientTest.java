package backend.service;

import backend.entity.Parcel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PublicTrackingPageClientTest {

  @Test
  void remainsOutOfScopeForColissimoTracking() {
    PublicTrackingPageClient client = new PublicTrackingPageClient();
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("colissimo");
    parcel.setTrackingNumber("LA123456789FR");
    parcel.setNormalizedTrackingNumber("LA123456789FR");

    Assertions.assertFalse(client.supports(parcel));
  }

  @Test
  void stillSupportsConfiguredGenericCarrierFallbacks() {
    PublicTrackingPageClient client = new PublicTrackingPageClient();
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("chronopost");
    parcel.setTrackingNumber("XY123456789FR");
    parcel.setNormalizedTrackingNumber("XY123456789FR");

    Assertions.assertTrue(client.supports(parcel));
  }
}
