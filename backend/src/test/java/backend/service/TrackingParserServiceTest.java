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
  void parsesMondialRelayTrackingWhenContextIsExplicit() {
    TrackingDetectionResult result = parser.detect(
        "Mondial Relay <suivi@mondialrelay.fr>",
        "Votre colis est disponible",
        """
            Votre colis est disponible au Point Relais.
            Numero d expedition: 12345678
            """
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals("mondial-relay", result.autoImportCandidates().get(0).carrierSlug());
    Assertions.assertEquals("12345678", result.autoImportCandidates().get(0).normalizedTrackingNumber());
  }

  @Test
  void enrichesMondialRelayTrackingUrlWithPostalCodeFromMailBody() {
    TrackingDetectionResult result = parser.detect(
        "Mondial Relay <suivi@mondialrelay.fr>",
        "Votre colis est disponible au Point Relais",
        """
            Votre colis est disponible au Point Relais.
            Numero d expedition : 90651136
            Code postal : 17000
            """
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals(
        "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=90651136&codePostal=17000",
        result.autoImportCandidates().get(0).trackingUrl()
    );
  }

  @Test
  void keepsMondialRelayPostalCodeAlreadyPresentInTrustedLink() {
    TrackingDetectionResult result = parser.detect(
        "Mondial Relay <suivi@mondialrelay.fr>",
        "Suivi Mondial Relay",
        "Suivre votre colis 90651136",
        List.of("https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=90651136&codePostal=17000")
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals(
        "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=90651136&codePostal=17000",
        result.autoImportCandidates().get(0).trackingUrl()
    );
  }

  @Test
  void doesNotInventMondialRelayPostalCodeWhenOnlyGenericNumbersArePresent() {
    TrackingDetectionResult result = parser.detect(
        "Mondial Relay <suivi@mondialrelay.fr>",
        "Votre colis est disponible",
        """
            Votre colis est disponible au Point Relais.
            Numero d expedition : 90651136
            Commande : 17000
            """
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertNull(result.autoImportCandidates().get(0).trackingUrl());
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
    Assertions.assertEquals("colissimo", parser.inferCarrierSlug("05308083313940F"));
    Assertions.assertEquals("chronopost", parser.inferCarrierSlug("XY123456789FR"));
    Assertions.assertEquals("chronopost", parser.inferCarrierSlug("XR646836167TS"));
    Assertions.assertEquals("chronopost", parser.inferCarrierSlug("XW496078433TS"));
    Assertions.assertEquals("ups", parser.inferCarrierSlug("1Z999AA10123456784"));
    Assertions.assertEquals("amazon-logistics", parser.inferCarrierSlug("TBA123456789012"));
    Assertions.assertNull(parser.inferCarrierSlug("1234567890"));
    Assertions.assertNull(parser.inferCarrierSlug("ABC"));
  }

  @Test
  void parsesLaPoste15CharacterTrackingWhenCarrierContextIsExplicit() {
    TrackingDetectionResult result = parser.detect(
        "La Poste <noreply@laposte.fr>",
        "Votre courrier suivi est en route",
        "Numero de suivi : 05308083313940F"
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals("colissimo", result.autoImportCandidates().get(0).carrierSlug());
    Assertions.assertEquals("05308083313940F", result.autoImportCandidates().get(0).normalizedTrackingNumber());
  }

  @Test
  void doesNotAutoImportWeakMerchantTrackingLikeValuesFromContextOnlyCarrierHints() {
    TrackingDetectionResult result = parser.detect(
        "HHV <service@hhv.de>",
        "Suivi livraison UPS",
        "Reference de suivi ZF-12-AB-34-CD",
        List.of("https://www.hhv.de/account/track?code=ZF-12-AB-34-CD")
    );

    Assertions.assertTrue(result.autoImportCandidates().isEmpty());
    Assertions.assertTrue(result.reviewCandidates().isEmpty());
  }

  @Test
  void rejectsWeakFragmentedCodesEvenWithStrongTrackingWording() {
    TrackingDetectionResult result = parser.detect(
        "shop@example.com",
        "Votre colis arrive",
        "Numero de suivi: ZF-12-AB-34-CD. Suivre votre colis ici."
    );

    Assertions.assertTrue(result.autoImportCandidates().isEmpty());
    Assertions.assertTrue(result.reviewCandidates().isEmpty());
  }

  @Test
  void keepsOnlyTrustedCarrierLinksForTrackingUrl() {
    TrackingDetectionResult result = parser.detect(
        "UPS <tracking@ups.com>",
        "Tracking number for your shipment",
        "Tracking number: 1Z999AA10123456784",
        List.of(
            "https://www.hhv.de/account/track?code=1Z999AA10123456784",
            "https://www.ups.com/track?tracknum=1Z999AA10123456784"
        )
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals(
        "https://www.ups.com/track?tracknum=1Z999AA10123456784",
        result.autoImportCandidates().get(0).trackingUrl()
    );
  }

  @Test
  void prefersDeliveredMailStatusWhenBodyStillContainsOlderOutForDeliveryStep() {
    TrackingDetectionResult result = parser.detect(
        "Chronopost <notifications@chronopost.fr>",
        "Votre colis Chronopost est livre",
        """
            Votre colis a ete livre aujourd'hui.
            Historique:
            - En cours de livraison
            - Pris en charge sur notre site

            Numero de suivi: XY123456789FR
            """
    );

    Assertions.assertEquals(1, result.autoImportCandidates().size());
    Assertions.assertEquals("DELIVERED", result.autoImportCandidates().get(0).rawStatus());
  }
}
