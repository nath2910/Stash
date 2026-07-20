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

  @Test
  void laposteBrowserSnapshotUsesEndpointPayloadAndPromotesChronopostCarrier() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("colissimo");
    parcel.setTrackingNumber("05308083313940F");
    parcel.setNormalizedTrackingNumber("05308083313940F");

    TrackingSnapshot snapshot = LaPosteTrackingClient.toBrowserSnapshot(
        parcel,
        new BrowserTrackingScriptRunner.BrowserPagePayload(
            "local_browser",
            "Suivi de ma lettre ou mon colis - La Poste",
            """
                Numero de suivi: 05308083313940F
                Produit: chronopost
                Statut: Livraison effectuee
                Livraison: 2026-06-29T12:16:00+02:00
                2026-06-29T12:16:00+02:00 Livraison effectuee
                2026-06-26T09:05:00+02:00 Colis en cours de livraison par le livreur
                """,
            """
                <div class="tracking-provider" data-provider="laposte" data-product="chronopost">
                  <div class="tracking-status">Livraison effectuee</div>
                  <div class="tracking-event">2026-06-29T12:16:00+02:00 Livraison effectuee</div>
                  <script type="application/json" data-role="laposte-tracking-response">
                    {"shipment":{"product":"chronopost","deliveryDate":"2026-06-29T12:16:00+02:00"}}
                  </script>
                </div>
                """,
            "https://www.chronopost.fr/tracking-no-cms/suivi-page?langue=fr&listeNumerosLT=05308083313940F"
        )
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, snapshot.status());
    Assertions.assertEquals("chronopost", snapshot.carrierSlug());
    Assertions.assertEquals(
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?langue=fr&listeNumerosLT=05308083313940F",
        snapshot.trackingUrl()
    );
    Assertions.assertNotNull(snapshot.deliveredAt());
  }

  @Test
  void mondialRelayBrowserSnapshotMarksMissingParcelAsException() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("mondial-relay");
    parcel.setTrackingNumber("26716323");
    parcel.setNormalizedTrackingNumber("26716323");

    TrackingSnapshot snapshot = MondialRelayTrackingClient.toBrowserSnapshot(
        parcel,
        new BrowserTrackingScriptRunner.BrowserPagePayload(
            "local_browser",
            "Suivi de colis - Mondial Relay",
            """
                Nous n'avons identifie aucun colis portant ce numero de suivi.
                Il n'existe pas de colis pour ces criteres de recherche.
                """,
            """
                <div class="status-banner">Nous n'avons identifie aucun colis portant ce numero de suivi.</div>
                <div>Il n'existe pas de colis pour ces criteres de recherche.</div>
                """,
            "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=26716323&codePostal=29900"
        )
    );

    Assertions.assertEquals(ParcelStatus.EXCEPTION, snapshot.status());
    Assertions.assertEquals("Nous n'avons identifie aucun colis portant ce numero de suivi.", snapshot.statusLabel());
  }
}
