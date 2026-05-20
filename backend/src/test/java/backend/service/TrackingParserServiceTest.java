package backend.service;

import backend.service.TrackingParserService.TrackingConfidence;
import backend.service.TrackingParserService.TrackingDetectionResult;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TrackingParserServiceTest {

  private final TrackingParserService parser = new TrackingParserService();

  @Test
  void parsesUpsTrackingFromCarrierMessage() {
    List<TrackingParserService.TrackingCandidate> candidates = parser.parse(
        "UPS <tracking@ups.com>",
        "Votre livraison UPS est en route",
        "Numero de suivi: 1Z999AA10123456784"
    );

    Assertions.assertEquals(1, candidates.size());
    Assertions.assertEquals("ups", candidates.get(0).carrierSlug());
    Assertions.assertEquals("1Z999AA10123456784", candidates.get(0).normalizedTrackingNumber());
    Assertions.assertEquals(TrackingConfidence.HIGH, candidates.get(0).confidenceLevel());
  }

  @Test
  void parsesAmazonLogisticsTracking() {
    TrackingDetectionResult result = parser.detect(
        "Amazon <shipment-tracking@amazon.fr>",
        "Your order has shipped",
        "Tracking ID: TBA123456789012. Your package is on the way."
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals("amazon-logistics", result.autoImportCandidates().get(0).carrierSlug());
    Assertions.assertEquals("TBA123456789012", result.autoImportCandidates().get(0).normalizedTrackingNumber());
  }

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
  }

  @Test
  void inspectsGenericForwardedDeliverySubjectThenParsesCarrierFromBody() {
    Assertions.assertTrue(parser.shouldInspectMessage(
        "Nathan Talvasson <nathan@example.com>",
        "Fwd: Votre colis arrive !"
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
    Assertions.assertEquals("colissimo", candidates.get(0).carrierSlug());
    Assertions.assertEquals("6A28526502105", candidates.get(0).normalizedTrackingNumber());
  }

  @Test
  void ignoresOrderNumberOnly() {
    TrackingDetectionResult result = parser.detect(
        "shop@example.com",
        "Votre commande est confirmee",
        "Numero de commande: 123456789012. Merci pour votre achat."
    );

    Assertions.assertTrue(result.autoImportCandidates().isEmpty());
    Assertions.assertTrue(result.reviewCandidates().isEmpty());
  }

  @Test
  void ignoresInvoiceAmountAndReference() {
    TrackingDetectionResult result = parser.detect(
        "facture@example.com",
        "Facture de livraison",
        "Facture numero 20240520. Total TTC 123456789012 EUR."
    );

    Assertions.assertTrue(result.autoImportCandidates().isEmpty());
    Assertions.assertTrue(result.reviewCandidates().isEmpty());
    Assertions.assertTrue(result.rejectedCount() >= 1);
  }

  @Test
  void ignoresPhoneNumbers() {
    TrackingDetectionResult result = parser.detect(
        "support@example.com",
        "Informations de livraison",
        "Votre transporteur peut vous appeler au telephone 0612345678."
    );

    Assertions.assertTrue(result.autoImportCandidates().isEmpty());
    Assertions.assertTrue(result.reviewCandidates().isEmpty());
  }

  @Test
  void keepsOnlyTrackingWhenMessageContainsOrderAndTrackingNumbers() {
    TrackingDetectionResult result = parser.detect(
        "UPS <tracking@ups.com>",
        "Tracking number for your shipment",
        "Order number: 123456789012. Tracking number: 1Z999AA10123456784."
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals("1Z999AA10123456784", result.autoImportCandidates().get(0).normalizedTrackingNumber());
    Assertions.assertTrue(result.rejectedCount() >= 1);
  }

  @Test
  void detectsTrackingFromKnownTrackingLink() {
    TrackingDetectionResult result = parser.detect(
        "UPS <tracking@ups.com>",
        "Votre colis est expedie",
        "Suivre votre colis",
        List.of("https://www.ups.com/track?tracknum=1Z999AA10123456784")
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals("ups", result.autoImportCandidates().get(0).carrierSlug());
    Assertions.assertEquals("https://www.ups.com/track?tracknum=1Z999AA10123456784",
        result.autoImportCandidates().get(0).trackingUrl());
  }

  @Test
  void deduplicatesSameTrackingNumberInOneMessage() {
    TrackingDetectionResult result = parser.detect(
        "no-reply@laposte.fr",
        "Suivi Colissimo",
        "Numero de suivi 6A28526502105. Rappel: 6A28526502105."
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals("6A28526502105", result.autoImportCandidates().get(0).normalizedTrackingNumber());
    Assertions.assertTrue(result.duplicateCount() >= 1);
  }

  @Test
  void putsCarrierMentionWithAmbiguousNumericTrackingInReview() {
    TrackingDetectionResult result = parser.detect(
        "DPD <notification@dpd.fr>",
        "Votre livraison DPD",
        "Reference colis: 123456789012"
    );

    Assertions.assertTrue(result.autoImportCandidates().isEmpty());
    Assertions.assertEquals(1, result.reviewCandidates().size());
    Assertions.assertEquals(TrackingConfidence.MEDIUM, result.reviewCandidates().get(0).confidenceLevel());
  }

  @Test
  void ignoresTrackingLikeValuesWhenMessageIsNotDeliveryRelated() {
    List<TrackingParserService.TrackingCandidate> candidates = parser.parse(
        "facture@example.com",
        "Votre facture",
        "Reference 1Z999AA10123456784"
    );

    Assertions.assertTrue(candidates.isEmpty());
  }

  @Test
  void parsesFrenchAndEnglishDeliveryWording() {
    Assertions.assertEquals(1, parser.parse(
        "La Poste <noreply@laposte.fr>",
        "Votre colis est expedie",
        "Numero de suivi: LA123456789FR"
    ).size());

    Assertions.assertEquals(1, parser.parse(
        "FedEx <tracking@fedex.com>",
        "Shipment tracking number",
        "Tracking number 123456789012 for your package"
    ).size());
  }

  @Test
  void ignoresTooShortNumbersAndAnalyticsUrls() {
    TrackingDetectionResult tooShort = parser.detect(
        "shop@example.com",
        "Votre colis",
        "Numero de suivi 12345678"
    );
    Assertions.assertTrue(tooShort.autoImportCandidates().isEmpty());
    Assertions.assertTrue(tooShort.reviewCandidates().isEmpty());

    TrackingDetectionResult analytics = parser.detect(
        "newsletter@example.com",
        "Votre livraison",
        "Votre colis arrive",
        List.of("https://analytics.example.com/track?id=123456789012")
    );
    Assertions.assertTrue(analytics.autoImportCandidates().isEmpty());
    Assertions.assertTrue(analytics.reviewCandidates().isEmpty());
  }

  @Test
  void infersOnlyReliableCarrierFamiliesFromManualTrackingNumber() {
    Assertions.assertEquals("colissimo", parser.inferCarrierSlug("LA-123456789-FR"));
    Assertions.assertEquals("ups", parser.inferCarrierSlug("1Z999AA10123456784"));
    Assertions.assertEquals("amazon-logistics", parser.inferCarrierSlug("TBA123456789012"));
    Assertions.assertNull(parser.inferCarrierSlug("1234567890"));
    Assertions.assertNull(parser.inferCarrierSlug("ABC"));
  }
}
