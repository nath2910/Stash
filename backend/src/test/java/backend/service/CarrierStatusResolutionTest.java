package backend.service;

import backend.entity.ParcelStatus;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CarrierStatusResolutionTest {

  @Test
  void laposteChronopostPrefersDeliveredSummaryOverOlderTransitEvent() {
    TrackingEventSnapshot latest = new TrackingEventSnapshot(
        ParcelStatus.IN_TRANSIT,
        "PC1",
        "Votre colis est pris en charge",
        null,
        OffsetDateTime.parse("2026-07-12T10:15:00Z"),
        java.util.Map.of()
    );

    LaPosteTrackingClient.ResolvedStatus resolved = LaPosteTrackingClient.resolveStatus(
        java.util.List.of(latest),
        "Votre colis a ete livre",
        "Distribution effectuee",
        java.util.List.of(),
        null
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, resolved.status());
    Assertions.assertEquals("Votre colis a ete livre", resolved.label());
  }

  @Test
  void mondialRelayPrefersRelayAvailabilitySummaryOverOlderTransitEvent() {
    TrackingEventSnapshot latest = new TrackingEventSnapshot(
        ParcelStatus.IN_TRANSIT,
        null,
        "Colis pris en charge par Mondial Relay",
        null,
        OffsetDateTime.parse("2026-07-12T10:15:00Z"),
        java.util.Map.of()
    );

    MondialRelayTrackingClient.ResolvedStatus resolved = MondialRelayTrackingClient.resolveStatus(
        java.util.List.of(latest),
        "Colis disponible au Point Relais",
        "Votre colis est a disposition du point relais"
    );

    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, resolved.status());
    Assertions.assertEquals("Colis disponible au Point Relais", resolved.label());
  }

  @Test
  void laposteDetectsDeliveredWhenSummaryContainsAccents() {
    LaPosteTrackingClient.ResolvedStatus resolved = LaPosteTrackingClient.resolveStatus(
        java.util.List.of(),
        "Votre colis a ete livre",
        "Distribue",
        java.util.List.of(),
        null
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, resolved.status());
    Assertions.assertEquals("Votre colis a ete livre", resolved.label());
  }

  @Test
  void mondialRelayDetectsDeliveredWhenSummaryContainsAccents() {
    MondialRelayTrackingClient.ResolvedStatus resolved = MondialRelayTrackingClient.resolveStatus(
        java.util.List.of(),
        "Colis retire par le destinataire",
        null
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, resolved.status());
    Assertions.assertEquals("Colis retire par le destinataire", resolved.label());
  }

  @Test
  void mondialRelayKeepsDeliveredIfAnOlderEventIsDeliveredButLastEventLooksLikeException() {
    TrackingEventSnapshot delivered = new TrackingEventSnapshot(
        ParcelStatus.DELIVERED,
        null,
        "Colis retire par le destinataire",
        null,
        OffsetDateTime.parse("2026-07-12T10:15:00Z"),
        java.util.Map.of()
    );
    TrackingEventSnapshot exception = new TrackingEventSnapshot(
        ParcelStatus.EXCEPTION,
        null,
        "Incident de livraison",
        null,
        OffsetDateTime.parse("2026-07-11T08:00:00Z"),
        java.util.Map.of()
    );

    MondialRelayTrackingClient.ResolvedStatus resolved = MondialRelayTrackingClient.resolveStatus(
        java.util.List.of(exception, delivered),
        null,
        null
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, resolved.status());
  }
}
