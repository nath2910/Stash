package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BrowserTrackingFallbackClientsTest {

  @Test
  void laposteBrowserSnapshotDetectsDeliveredStatus() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("colissimo");
    parcel.setTrackingNumber("8A00000000000");
    parcel.setNormalizedTrackingNumber("8A00000000000");

    TrackingSnapshot snapshot = LaPosteTrackingClient.toBrowserSnapshot(
        parcel,
        new BrowserTrackingScriptRunner.BrowserPagePayload(
            "local_browser",
            "Suivre un colis",
            """
                Votre colis a ete livre
                12/07/2026 10:15
                Votre colis a ete livre dans votre boite aux lettres
                """,
            """
                <div class="tracking-status">Votre colis a ete livre</div>
                <div>12/07/2026 10:15</div>
                """,
            "https://www.laposte.fr/outils/suivre-vos-envois?code=8A00000000000"
        )
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, snapshot.status());
    Assertions.assertNotNull(snapshot.deliveredAt());
    Assertions.assertEquals("LA_POSTE_BROWSER_PAGE", snapshot.provider());
  }

  @Test
  void mondialRelayBrowserSnapshotDetectsRelayAvailability() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("mondial-relay");
    parcel.setTrackingNumber("12345678");
    parcel.setNormalizedTrackingNumber("12345678");

    TrackingSnapshot snapshot = MondialRelayTrackingClient.toBrowserSnapshot(
        parcel,
        new BrowserTrackingScriptRunner.BrowserPagePayload(
            "local_browser",
            "Suivi de colis Mondial Relay",
            """
                Colis disponible au Point Relais
                11/07/2026 08:30
                Colis disponible au Point Relais
                """,
            """
                <div class="status-banner">Colis disponible au Point Relais</div>
                <div>11/07/2026 08:30</div>
                """,
            "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=12345678"
        )
    );

    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, snapshot.status());
    Assertions.assertEquals("MONDIAL_RELAY_BROWSER_PAGE", snapshot.provider());
    Assertions.assertEquals("Colis disponible au Point Relais", snapshot.statusLabel());
  }

  @Test
  void mondialRelayBrowserSnapshotMarksTerminalExpiredTrackingAsFinished() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("mondial-relay");
    parcel.setTrackingNumber("90651136");
    parcel.setNormalizedTrackingNumber("90651136");

    TrackingSnapshot snapshot = MondialRelayTrackingClient.toBrowserSnapshot(
        parcel,
        new BrowserTrackingScriptRunner.BrowserPagePayload(
            "local_browser",
            "Suivi Mondial Relay",
            """
                Ce suivi n'est plus disponible.
                Suivi termine et archive.
                """,
            """
                <div class="status-banner">Ce suivi n'est plus disponible</div>
                <div>Suivi termine et archive</div>
                """,
            "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=90651136&codePostal=17000"
        )
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, snapshot.status());
    Assertions.assertEquals(
        "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=90651136&codePostal=17000",
        snapshot.trackingUrl()
    );
  }

  @Test
  void mondialRelayBrowserSnapshotKeepsPostalCodeRequirementVisible() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("mondial-relay");
    parcel.setTrackingNumber("90651136");
    parcel.setNormalizedTrackingNumber("90651136");

    TrackingSnapshot snapshot = MondialRelayTrackingClient.toBrowserSnapshot(
        parcel,
        new BrowserTrackingScriptRunner.BrowserPagePayload(
            "local_browser",
            "Suivi Mondial Relay",
            """
                Merci de renseigner votre code postal pour consulter ce suivi.
                """,
            """
                <div class="status-banner">Merci de renseigner votre code postal pour consulter ce suivi.</div>
                """,
            "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=90651136"
        )
    );

    Assertions.assertEquals(ParcelStatus.UNKNOWN, snapshot.status());
    Assertions.assertEquals(
        "Merci de renseigner votre code postal pour consulter ce suivi.",
        snapshot.statusLabel()
    );
  }

  @Test
  void laposteBrowserSnapshotIgnoresGenericPageTitleWithoutTrackingStatus() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("colissimo");
    parcel.setTrackingNumber("8A00000000000");
    parcel.setNormalizedTrackingNumber("8A00000000000");

    TrackingSnapshot snapshot = LaPosteTrackingClient.toBrowserSnapshot(
        parcel,
        new BrowserTrackingScriptRunner.BrowserPagePayload(
            "local_browser",
            "Suivre une lettre, un Colissimo ou un Chronopost - La Poste",
            """
                Le respect de votre vie privee, notre priorite
                Ne pas accepter et fermer
                Accepter et fermer
                """,
            "<div>Cookies</div>",
            "https://www.laposte.fr/outils/suivre-vos-envois"
        )
    );

    Assertions.assertEquals(ParcelStatus.UNKNOWN, snapshot.status());
    Assertions.assertNull(snapshot.statusLabel());
  }
}
