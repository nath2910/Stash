package backend.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TrackingCarrierRulesTest {

  @Test
  void infersSupportedCarrierFamiliesWithChronopostPriorityOverFrUpuFallback() {
    Assertions.assertEquals("chronopost", TrackingCarrierRules.inferSupportedCarrier("XY123456789FR"));
    Assertions.assertEquals("chronopost", TrackingCarrierRules.inferSupportedCarrier("XR646836167TS"));
    Assertions.assertEquals("colissimo", TrackingCarrierRules.inferSupportedCarrier("LA123456789FR"));
    Assertions.assertEquals("colissimo", TrackingCarrierRules.inferSupportedCarrier("6A28526502105"));
    Assertions.assertEquals("mondial-relay", TrackingCarrierRules.inferSupportedCarrier("12345678"));
  }

  @Test
  void validatesCarrierSpecificFormats() {
    Assertions.assertTrue(TrackingCarrierRules.isValidForCarrier("12345678", "mondial-relay"));
    Assertions.assertTrue(TrackingCarrierRules.isValidForCarrier("LA123456789FR", "colissimo"));
    Assertions.assertTrue(TrackingCarrierRules.isValidForCarrier("XY123456789FR", "chronopost"));
    Assertions.assertFalse(TrackingCarrierRules.isValidForCarrier("XY123456789FR", "colissimo"));
    Assertions.assertFalse(TrackingCarrierRules.isValidForCarrier("LA123456789FR", "chronopost"));
  }

  @Test
  void keepsOfficialTrackingLinksCentralized() {
    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=LA123456789FR",
        TrackingCarrierRules.officialTrackingUrl("colissimo", "LA123456789FR")
    );
    Assertions.assertEquals(
        "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition=12345678",
        TrackingCarrierRules.officialTrackingUrl("mondial-relay", "12345678")
    );
    Assertions.assertEquals(
        List.of("chronopost"),
        TrackingCarrierRules.matchSupportedCarriers("XY123456789FR")
    );
  }
}
