package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.repository.ParcelEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ParcelTrackingUpdateServiceTest {

  @Test
  void appliesSnapshotAndCreatesTimelineEvent() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(42L);
    parcel.setStatus(ParcelStatus.PENDING);

    OffsetDateTime eventTime = OffsetDateTime.of(2026, 5, 19, 12, 0, 0, 0, ZoneOffset.UTC);
    TrackingSnapshot snapshot = new TrackingSnapshot(
        "LA_POSTE_OKAPI",
        "LA123456789FR",
        "colissimo",
        ParcelStatus.IN_TRANSIT,
        "Votre colis est en transit",
        null,
        null,
        "https://www.laposte.fr/outils/suivre-vos-envois?code=LA123456789FR",
        "FR",
        "FR",
        "Colissimo",
        null,
        Map.of("source", "test"),
        List.of(new TrackingEventSnapshot(
            ParcelStatus.IN_TRANSIT,
            "ET1",
            "Votre colis est en transit",
            "Paris",
            eventTime,
            Map.of("code", "ET1")
        ))
    );

    service.applySnapshot(parcel, snapshot);

    Assertions.assertEquals("LA_POSTE_OKAPI", parcel.getAggregator());
    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, parcel.getStatus());
    Assertions.assertEquals(eventTime, parcel.getLastEventAt());
    Assertions.assertEquals("FR", parcel.getRawCurrentPayload().get("destination_raw_location"));
    Mockito.verify(eventRepository).save(Mockito.any());
  }

  @Test
  void doesNotDowngradeDeliveredParcelWithOlderCarrierSnapshot() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(43L);
    parcel.setStatus(ParcelStatus.DELIVERED);
    parcel.setStatusLabel("Livre selon email transporteur");

    TrackingSnapshot snapshot = new TrackingSnapshot(
        "PUBLIC_TRACKING_PAGE",
        "XY123456789FR",
        "chronopost",
        ParcelStatus.IN_TRANSIT,
        "Votre colis est en cours de livraison",
        null,
        null,
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR",
        null,
        null,
        null,
        null,
        Map.of("source", "test"),
        List.of()
    );

    service.applySnapshot(parcel, snapshot);

    Assertions.assertEquals(ParcelStatus.DELIVERED, parcel.getStatus());
    Assertions.assertEquals("Livre selon email transporteur", parcel.getStatusLabel());
  }

  @Test
  void deliveredSnapshotOverridesPreviousExceptionStatus() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(44L);
    parcel.setStatus(ParcelStatus.EXCEPTION);
    parcel.setStatusLabel("Incident de livraison");

    TrackingSnapshot snapshot = new TrackingSnapshot(
        "LA_POSTE_OKAPI",
        "XY123456789FR",
        "chronopost",
        ParcelStatus.DELIVERED,
        "Votre colis a été livré",
        null,
        OffsetDateTime.now(ZoneOffset.UTC),
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR",
        null,
        null,
        null,
        null,
        Map.of("source", "test"),
        List.of()
    );

    service.applySnapshot(parcel, snapshot);

    Assertions.assertEquals(ParcelStatus.DELIVERED, parcel.getStatus());
    Assertions.assertEquals("Votre colis a été livré", parcel.getStatusLabel());
  }
  @Test
  void promotesRegisteredSnapshotToDeliveredWhenTimelineContainsDeliveredSignal() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(45L);
    parcel.setStatus(ParcelStatus.REGISTERED);
    parcel.setStatusLabel("Bordereau cree");

    TrackingSnapshot snapshot = new TrackingSnapshot(
        "PUBLIC_TRACKING_PAGE",
        "XY123456789FR",
        "chronopost",
        ParcelStatus.REGISTERED,
        "Bordereau cree",
        null,
        null,
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR",
        null,
        null,
        null,
        null,
        Map.of("source", "test"),
        List.of(new TrackingEventSnapshot(
            ParcelStatus.DELIVERED,
            null,
            "Votre colis a ete livre",
            null,
            OffsetDateTime.parse("2026-07-13T10:00:00Z"),
            Map.of()
        ))
    );

    service.applySnapshot(parcel, snapshot);

    Assertions.assertEquals(ParcelStatus.DELIVERED, parcel.getStatus());
    Assertions.assertEquals("Votre colis a ete livre", parcel.getStatusLabel());
    Assertions.assertNotNull(parcel.getDeliveredAt());
  }

  @Test
  void marksTerminalArchivedTrackingAsDeliveredWhenCarrierPageSaysItIsNoLongerAvailable() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(46L);
    parcel.setStatus(ParcelStatus.IN_TRANSIT);
    parcel.setStatusLabel("En transit");

    TrackingSnapshot snapshot = new TrackingSnapshot(
        "MONDIAL_RELAY_BROWSER_PAGE",
        "90651136",
        "mondial-relay",
        ParcelStatus.UNKNOWN,
        "Ce suivi n'est plus disponible",
        null,
        null,
        "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=90651136&codePostal=17000",
        null,
        null,
        null,
        null,
        Map.of("source", "test"),
        List.of()
    );

    service.applySnapshot(parcel, snapshot);

    Assertions.assertEquals(ParcelStatus.DELIVERED, parcel.getStatus());
    Assertions.assertEquals("Ce suivi n'est plus disponible", parcel.getStatusLabel());
    Assertions.assertNotNull(parcel.getDeliveredAt());
  }

  @Test
  void keepsExistingTrackingContextWhenApplyingFreshSnapshot() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(47L);
    parcel.setStatus(ParcelStatus.REGISTERED);
    parcel.setRawCurrentPayload(new java.util.HashMap<>(Map.of(
        "tracking_url", "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=05308083313940F&langue=fr_FR&zipCode=29900",
        "destination_postal_code", "29900",
        "source_context_snippet", "Lien Chronopost avec code postal"
    )));

    TrackingSnapshot snapshot = new TrackingSnapshot(
        "CHRONOPOST_DIRECT",
        "05308083313940F",
        "chronopost",
        ParcelStatus.DELIVERED,
        "Livraison effectuee",
        null,
        OffsetDateTime.parse("2026-06-29T10:16:00Z"),
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=05308083313940F&langue=fr_FR&zipCode=29900",
        null,
        null,
        "Chronopost",
        null,
        Map.of("tab", "<table></table>"),
        List.of()
    );

    service.applySnapshot(parcel, snapshot);

    Assertions.assertEquals("29900", parcel.getRawCurrentPayload().get("destination_postal_code"));
    Assertions.assertEquals(
        "Lien Chronopost avec code postal",
        parcel.getRawCurrentPayload().get("source_context_snippet")
    );
    Assertions.assertTrue(String.valueOf(parcel.getRawCurrentPayload().get("tracking_url")).contains("zipCode=29900"));
  }

  @Test
  void truncatesOverlongCarrierStatusLabelToFitParcelColumn() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(48L);
    parcel.setStatus(ParcelStatus.IN_TRANSIT);

    String longLabel =
        "Votre Colissimo vous attend dans votre point de retrait. "
            + "Le delai de retrait est de 5 jours en consigne Pickup et 14 jours en relais commercant "
            + "(en jours calendaires). Pour le retirer, n'oubliez pas votre piece d'identite "
            + "originale ou votre code de retrait en consigne.";

    TrackingSnapshot snapshot = new TrackingSnapshot(
        "LA_POSTE_BROWSER_PAGE",
        "6Y11138575506",
        "colissimo",
        ParcelStatus.IN_TRANSIT,
        longLabel,
        null,
        null,
        "https://www.laposte.fr/outils/suivre-vos-envois?code=6Y11138575506",
        null,
        null,
        "Colissimo",
        null,
        Map.of("source", "test"),
        List.of()
    );

    service.applySnapshot(parcel, snapshot);

    Assertions.assertNotNull(parcel.getStatusLabel());
    Assertions.assertEquals(255, parcel.getStatusLabel().length());
    Assertions.assertTrue(longLabel.startsWith(parcel.getStatusLabel()));
  }

  @Test
  void carrierSnapshotCanOverrideDeliveredStatusThatCameOnlyFromMailHint() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(49L);
    parcel.setStatus(ParcelStatus.DELIVERED);
    parcel.setDeliveredAt(OffsetDateTime.parse("2026-07-21T10:00:00Z"));
    parcel.setStatusLabel("Livre selon email Colissimo");

    TrackingSnapshot snapshot = new TrackingSnapshot(
        "LA_POSTE_BROWSER_PAGE",
        "6Y11138575506",
        "colissimo",
        ParcelStatus.IN_TRANSIT,
        "Votre Colissimo vous attend dans votre point de retrait.",
        null,
        null,
        "https://www.laposte.fr/outils/suivre-vos-envois?code=6Y11138575506",
        null,
        null,
        "Colissimo",
        null,
        Map.of("source", "test"),
        List.of()
    );

    service.applySnapshot(parcel, snapshot);

    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, parcel.getStatus());
    Assertions.assertNull(parcel.getDeliveredAt());
    Assertions.assertEquals(
        "Votre Colissimo vous attend dans votre point de retrait.",
        parcel.getStatusLabel()
    );
  }

  @Test
  void localFallbackKeepsDeliveredStatusButMarksLaposteSourceUnavailable() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(50L);
    parcel.setStatus(ParcelStatus.DELIVERED);
    parcel.setDeliveredAt(OffsetDateTime.parse("2026-07-21T10:00:00Z"));
    parcel.setStatusLabel("Livre selon email Colissimo");

    service.markLocalFallback(parcel, "DIRECT_CARRIER", "Source La Poste indisponible");

    Assertions.assertEquals(ParcelStatus.DELIVERED, parcel.getStatus());
    Assertions.assertEquals("DIRECT_CARRIER", parcel.getAggregator());
    Assertions.assertEquals("Source La Poste indisponible", parcel.getStatusLabel());
    Assertions.assertEquals(OffsetDateTime.parse("2026-07-21T10:00:00Z"), parcel.getDeliveredAt());
  }
}
