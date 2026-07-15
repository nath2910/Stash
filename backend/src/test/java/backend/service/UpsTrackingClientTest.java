package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UpsTrackingClientTest {

  @Test
  void supportsUpsTrackingNumberWhenCarrierIsUnknown() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("unknown");
    parcel.setTrackingNumber("1Z12345E0205271688");
    parcel.setNormalizedTrackingNumber("1Z12345E0205271688");

    Assertions.assertTrue(new UpsTrackingClient().supports(parcel));
  }

  @Test
  void parsesDeliveredUpsBrowserPayload() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("ups");
    parcel.setTrackingNumber("1Z12345E0205271688");
    parcel.setNormalizedTrackingNumber("1Z12345E0205271688");

    TrackingSnapshot snapshot = UpsTrackingClient.toSnapshot(
        parcel,
        new BrowserTrackingScriptRunner.BrowserPagePayload(
            "local_browser",
            "UPS Tracking",
            """
                Delivered
                July 12, 2026 10:15 AM
                Delivered
                Your package was delivered.
                """,
            """
                <section class="status-banner">Delivered</section>
                <div>July 12, 2026 10:15 AM</div>
                """,
            "https://www.ups.com/track?tracknum=1Z12345E0205271688"
        )
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, snapshot.status());
    Assertions.assertEquals("ups", snapshot.carrierSlug());
    Assertions.assertEquals("UPS_BROWSER_PAGE", snapshot.provider());
    Assertions.assertNotNull(snapshot.deliveredAt());
    Assertions.assertFalse(snapshot.events().isEmpty());
  }
}
