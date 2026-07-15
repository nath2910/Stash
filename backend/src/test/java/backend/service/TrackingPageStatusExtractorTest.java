package backend.service;

import backend.entity.ParcelStatus;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TrackingPageStatusExtractorTest {

  @Test
  void extractsDeliveredStatusFromSimpleCarrierMarkup() {
    Optional<TrackingPageStatusExtractor.ExtractedStatus> status = TrackingPageStatusExtractor.extractStatus("""
        <html>
          <head>
            <title>Chronopost - Colis livre</title>
            <meta name="description" content="Votre colis est remis au destinataire" />
          </head>
          <body>
            <main>
              <h1>Colis livre</h1>
              <p>Remis au destinataire</p>
            </main>
          </body>
        </html>
        """);

    Assertions.assertTrue(status.isPresent());
    Assertions.assertEquals(ParcelStatus.DELIVERED, status.get().status());
    Assertions.assertFalse(status.get().label().isBlank());
  }

  @Test
  void extractsDeliveryInProgressAsOutForDeliveryStatus() {
    Optional<TrackingPageStatusExtractor.ExtractedStatus> status = TrackingPageStatusExtractor.extractStatus("""
        <html>
          <body>
            <section class="tracking-status">Votre colis est en cours de livraison</section>
          </body>
        </html>
        """);

    Assertions.assertTrue(status.isPresent());
    Assertions.assertEquals(ParcelStatus.OUT_FOR_DELIVERY, status.get().status());
  }

  @Test
  void prefersDeliveredStatusWhenPageStillContainsOlderDeliveryStep() {
    Optional<TrackingPageStatusExtractor.ExtractedStatus> status = TrackingPageStatusExtractor.extractStatus("""
        <html>
          <head>
            <title>Suivi Chronopost</title>
            <meta name="description" content="Votre colis a ete livre" />
          </head>
          <body>
            <section class="tracking-status">Votre colis a ete livre</section>
            <ol class="tracking-steps">
              <li>En cours de livraison</li>
              <li>Pris en charge</li>
            </ol>
          </body>
        </html>
        """);

    Assertions.assertTrue(status.isPresent());
    Assertions.assertEquals(ParcelStatus.DELIVERED, status.get().status());
  }

  @Test
  void ignoresPagesWithoutClearTrackingStatus() {
    Optional<TrackingPageStatusExtractor.ExtractedStatus> status = TrackingPageStatusExtractor.extractStatus("""
        <html>
          <body>
            <h1>Aide et contact</h1>
            <p>Retrouvez ici toutes les informations sur nos expeditions.</p>
          </body>
        </html>
        """);

    Assertions.assertTrue(status.isEmpty());
  }

  @Test
  void extractsDeliveredStatusFromEmbeddedJsonState() {
    Optional<TrackingPageStatusExtractor.ExtractedStatus> status = TrackingPageStatusExtractor.extractStatus("""
        <html>
          <body>
            <script type="application/json">
              {"tracking":{"currentStatus":"Votre colis a ete livre","history":["En cours de livraison"]}}
            </script>
          </body>
        </html>
        """);

    Assertions.assertTrue(status.isPresent());
    Assertions.assertEquals(ParcelStatus.DELIVERED, status.get().status());
  }

  @Test
  void extractsRegisteredStatusFromLabelCreatedSnippet() {
    Optional<TrackingPageStatusExtractor.ExtractedStatus> status = TrackingPageStatusExtractor.extractStatus("""
        <html>
          <body>
            <div class="tracking-status">Bordereau cree chez le transporteur</div>
          </body>
        </html>
        """);

    Assertions.assertTrue(status.isPresent());
    Assertions.assertEquals(ParcelStatus.REGISTERED, status.get().status());
  }

  @Test
  void extractsDeliveredStatusFromShortActiveStepperLabel() {
    Optional<TrackingPageStatusExtractor.ExtractedStatus> status = TrackingPageStatusExtractor.extractStatus("""
        <html>
          <body>
            <div id="step4" class="tracking-step done"><span>En cours de livraison</span></div>
            <div id="step5" class="tracking-step active"><span>Livré</span></div>
          </body>
        </html>
        """);

    Assertions.assertTrue(status.isPresent());
    Assertions.assertEquals(ParcelStatus.DELIVERED, status.get().status());
    Assertions.assertEquals("Livré", status.get().label());
  }

  @Test
  void prefersActiveTransitStepOverFutureDeliveredStep() {
    Optional<TrackingPageStatusExtractor.ExtractedStatus> status = TrackingPageStatusExtractor.extractStatus("""
        <html>
          <body>
            <div id="step3" class="tracking-step active"><span>En cours d'acheminement</span></div>
            <div id="step4" class="tracking-step"><span>En cours de livraison</span></div>
            <div id="step5" class="tracking-step"><span>Livré</span></div>
          </body>
        </html>
        """);

    Assertions.assertTrue(status.isPresent());
    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, status.get().status());
  }
}
