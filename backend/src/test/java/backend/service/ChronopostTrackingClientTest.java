package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ChronopostTrackingClientTest {

  private final ChronopostTrackingClient client = new ChronopostTrackingClient();

  @Test
  void supportsChronopostTsTrackingNumberEvenWhenCarrierIsUnknown() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("unknown");
    parcel.setTrackingNumber("XR646836167TS");
    parcel.setNormalizedTrackingNumber("XR646836167TS");

    Assertions.assertTrue(client.supports(parcel));
  }

  @Test
  void supportsChronopostTrackingUrlEvenWhenCarrierIsUnknown() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("unknown");
    parcel.setTrackingNumber("XR646836167TS");
    parcel.setNormalizedTrackingNumber("XR646836167TS");
    HashMap<String, Object> payload = new HashMap<>();
    payload.put("tracking_url", "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XR646836167TS");
    parcel.setRawCurrentPayload(payload);

    Assertions.assertTrue(client.supports(parcel));
  }

  @Test
  void parsesDeliveredChronopostResponseFromAjaxEndpoint() {
    Parcel parcel = new Parcel();
    parcel.setTrackingNumber("XR646836167TS");
    parcel.setNormalizedTrackingNumber("XR646836167TS");
    parcel.setCarrierSlug("chronopost");

    TrackingSnapshot snapshot = ChronopostTrackingClient.toSnapshot(parcel, Map.of(
        "top", """
            <div class="ch-colis-information">
              Livraison effectuée<br />
              mardi 23/06/2026 à 15:26
            </div>
            <div id="step9" class="ch-suivi-colis-light-info active">
              <div class="ch-suivi-colis-light-text">Livré</div>
            </div>
            """,
        "tab", """
            <table>
              <tr class="toggleElmt">
                <td>mardi 23/06/2026<br />15:26</td>
                <td>CONCARNEAU - FR - CONSIGNE PICKUP LECLERC CONCAR<br />Livraison effectuée</td>
                <td colspan="2">Commentaire : colis récupéré en consigne</td>
              </tr>
              <tr class="toggleElmt">
                <td>vendredi 19/06/2026<br />19:41</td>
                <td>STRASBOURG CHRONOPOST<br />Colis pris en charge par Chronopost</td>
                <td colspan="2"></td>
              </tr>
            </table>
            """
    ));

    Assertions.assertEquals(ParcelStatus.DELIVERED, snapshot.status());
    Assertions.assertNotNull(snapshot.deliveredAt());
    Assertions.assertEquals("chronopost", snapshot.carrierSlug());
    Assertions.assertEquals("CHRONOPOST_DIRECT", snapshot.provider());
    Assertions.assertTrue(snapshot.statusLabel().contains("Livraison effectuée"));
    Assertions.assertEquals(2, snapshot.events().size());
  }

  @Test
  void latestEventUsesMostRecentChronopostEvent() {
    TrackingEventSnapshot delivered = new TrackingEventSnapshot(
        ParcelStatus.DELIVERED,
        null,
        "Livraison effectuée",
        null,
        OffsetDateTime.parse("2026-06-23T15:26:00Z"),
        Map.of()
    );
    TrackingEventSnapshot registered = new TrackingEventSnapshot(
        ParcelStatus.REGISTERED,
        null,
        "Colis en cours de préparation chez l'expéditeur",
        null,
        OffsetDateTime.parse("2026-06-19T12:32:00Z"),
        Map.of()
    );

    TrackingEventSnapshot latest = ChronopostTrackingClient.latestEvent(List.of(registered, delivered));

    Assertions.assertEquals(delivered, latest);
  }

  @Test
  void usesActiveChronopostStepperWhenItShowsTransitState() {
    Parcel parcel = new Parcel();
    parcel.setTrackingNumber("XR646836167TS");
    parcel.setNormalizedTrackingNumber("XR646836167TS");
    parcel.setCarrierSlug("chronopost");

    TrackingSnapshot snapshot = ChronopostTrackingClient.toSnapshot(parcel, Map.of(
        "top", """
            <div id="step1" class="ch-suivi-colis-light-info done">
              <div class="ch-suivi-colis-light-text">En preparation chez l'expediteur</div>
            </div>
            <div id="step2" class="ch-suivi-colis-light-info done">
              <div class="ch-suivi-colis-light-text">Pris en charge par Chronopost</div>
            </div>
            <div id="step3" class="ch-suivi-colis-light-info active">
              <div class="ch-suivi-colis-light-text">En cours d'acheminement</div>
            </div>
            <div id="step4" class="ch-suivi-colis-light-info">
              <div class="ch-suivi-colis-light-text">Envoi en cours de livraison</div>
            </div>
            <div id="step5" class="ch-suivi-colis-light-info">
              <div class="ch-suivi-colis-light-text">Livre</div>
            </div>
            """,
        "tab", "<table></table>"
    ));

    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, snapshot.status());
    Assertions.assertEquals("En cours d'acheminement", snapshot.statusLabel());
  }
}
