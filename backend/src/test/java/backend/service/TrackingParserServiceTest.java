package backend.service;

import backend.service.TrackingParserService.TrackingConfidence;
import backend.service.TrackingParserService.TrackingDetectionResult;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TrackingParserServiceTest {

  private final TrackingParserService parser = new TrackingParserService();

  @Test
  void parsesColissimoTrackingAndNormalizesSeparators() {
    List<TrackingParserService.TrackingCandidate> candidates = parser.parse(
        "no-reply@laposte.fr",
        "Suivi Colissimo",
        "Votre colis LA-123456789-FR sera livre prochainement"
    );

    Assertions.assertEquals(1, candidates.size());
    Assertions.assertEquals("colissimo", candidates.get(0).carrierSlug());
    Assertions.assertEquals("LA123456789FR", candidates.get(0).normalizedTrackingNumber());
    Assertions.assertEquals(TrackingConfidence.HIGH, candidates.get(0).confidenceLevel());
  }

  @Test
  void shouldInspectColissimoMessageRejectsOtherCarriers() {
    Assertions.assertTrue(parser.shouldInspectColissimoMessage(
        "La Poste <noreply@laposte.fr>",
        "Suivi de livraison Colissimo",
        "Votre colis est en route"
    ));

    Assertions.assertFalse(parser.shouldInspectColissimoMessage(
        "UPS <tracking@ups.com>",
        "Votre livraison UPS est en route",
        "Numero de suivi: 1Z999AA10123456784"
    ));
  }

  @Test
  void detectColissimoKeepsOnlyColissimoCandidates() {
    TrackingDetectionResult result = parser.detectColissimo(
        "La Poste <noreply@laposte.fr>",
        "Suivi Colissimo",
        """
            Votre colis est en route.
            Numero de suivi : 6A04296519970
            """
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals("colissimo", result.autoImportCandidates().get(0).carrierSlug());
    Assertions.assertEquals("6A04296519970", result.autoImportCandidates().get(0).normalizedTrackingNumber());
  }

  @Test
  void inspectsForwardedDeliveryMailThenParsesColissimoFromBody() {
    Assertions.assertTrue(parser.shouldInspectMessage(
        "Nathan Talvasson <nathan@example.com>",
        "Fwd: Votre colis arrive !",
        """
            ---------- Forwarded message ---------
            From: La Poste-Colissimo <noreply@notif-colissimo-laposte.info>
            Subject: Votre colis arrive !

            Votre colis numero de suivi 6A28526502105 est en route.
            """
    ));

    List<TrackingParserService.TrackingCandidate> candidates = parser.parse(
        "Nathan Talvasson <nathan@example.com>",
        "Fwd: Votre colis arrive !",
        """
            ---------- Forwarded message ---------
            From: La Poste-Colissimo <noreply@notif-colissimo-laposte.info>
            Subject: Votre colis arrive !

            Votre colis numero de suivi 6A28526502105 est en route.
            """
    );

    Assertions.assertEquals(1, candidates.size());
    Assertions.assertEquals("6A28526502105", candidates.get(0).normalizedTrackingNumber());
  }

  @Test
  void ignoresUnsupportedOrAmbiguousFormats() {
    Assertions.assertNull(parser.inferCarrierSlug("XY123456789FR"));
    Assertions.assertNull(parser.inferCarrierSlug("12345678"));
    Assertions.assertFalse(parser.isValidTrackingNumber("12345678"));
  }

  @Test
  void usesTrustedLaposteLinkWhenPresent() {
    TrackingDetectionResult result = parser.detect(
        "La Poste <noreply@laposte.fr>",
        "Votre colis est expedie",
        "Suivre votre colis",
        List.of("https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970")
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970",
        result.autoImportCandidates().get(0).trackingUrl()
    );
  }

  @Test
  void detectsPickupRelayMailWithoutMarkingItAsDelivered() {
    TrackingDetectionResult result = parser.detectColissimo(
        "Colissimo <colissimo@network1.pickup.fr>",
        "Votre colis AMAZON est arrive en relais Pickup",
        """
            Pour retirer votre colis, presentez ce Pickup Pass.
            Numero de colis : 6Y11138575506
            Statut : Disponible en relais
            Code de retrait : 955151
            """
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals("6Y11138575506", result.autoImportCandidates().get(0).normalizedTrackingNumber());
    Assertions.assertEquals("AVAILABLE_FOR_PICKUP", result.autoImportCandidates().get(0).rawStatus());
  }
}
