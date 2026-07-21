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
    Assertions.assertEquals("colissimo", snapshot.carrierSlug());
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
  void laposteBrowserSnapshotKeepsColissimoCarrierAndTrustedUrl() {
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
                Produit: colissimo
                Statut: Livraison effectuee
                Livraison: 2026-06-29T12:16:00+02:00
                2026-06-29T12:16:00+02:00 Livraison effectuee
                """,
            """
                <div class="tracking-provider" data-provider="laposte" data-product="colissimo">
                  <div class="tracking-status">Livraison effectuee</div>
                  <div class="tracking-event">2026-06-29T12:16:00+02:00 Livraison effectuee</div>
                  <script type="application/json" data-role="laposte-tracking-response">
                    {"shipment":{"product":"colissimo","deliveryDate":"2026-06-29T12:16:00+02:00"}}
                  </script>
                </div>
                """,
            "https://www.laposte.fr/outils/suivre-vos-envois?code=05308083313940F"
        )
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, snapshot.status());
    Assertions.assertEquals("colissimo", snapshot.carrierSlug());
    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=05308083313940F",
        snapshot.trackingUrl()
    );
    Assertions.assertNotNull(snapshot.deliveredAt());
  }
}
