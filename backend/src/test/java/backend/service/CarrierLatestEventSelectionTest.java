package backend.service;

import backend.entity.ParcelStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CarrierLatestEventSelectionTest {

  @Test
  void laposteUsesMostRecentEventInsteadOfListTail() {
    TrackingEventSnapshot delivered = new TrackingEventSnapshot(
        ParcelStatus.DELIVERED,
        null,
        "Votre colis a ete livre",
        null,
        OffsetDateTime.parse("2026-07-12T10:15:00Z"),
        java.util.Map.of()
    );
    TrackingEventSnapshot accepted = new TrackingEventSnapshot(
        ParcelStatus.IN_TRANSIT,
        null,
        "Votre colis est pris en charge",
        null,
        OffsetDateTime.parse("2026-07-10T08:00:00Z"),
        java.util.Map.of()
    );

    TrackingEventSnapshot latest = LaPosteTrackingClient.latestEvent(List.of(delivered, accepted));

    Assertions.assertEquals(delivered, latest);
  }

  @Test
  void mondialRelayUsesMostRecentEventInsteadOfListTail() {
    TrackingEventSnapshot delivered = new TrackingEventSnapshot(
        ParcelStatus.DELIVERED,
        null,
        "Colis livre",
        null,
        OffsetDateTime.parse("2026-07-12T10:15:00Z"),
        java.util.Map.of()
    );
    TrackingEventSnapshot accepted = new TrackingEventSnapshot(
        ParcelStatus.IN_TRANSIT,
        null,
        "Colis pris en charge",
        null,
        OffsetDateTime.parse("2026-07-10T08:00:00Z"),
        java.util.Map.of()
    );

    TrackingEventSnapshot latest = MondialRelayTrackingClient.latestEvent(List.of(delivered, accepted));

    Assertions.assertEquals(delivered, latest);
  }
}
