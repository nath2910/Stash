package backend.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TrackingCarrierRulesTest {

  @Test
  void keepsOnlyColissimoFormatsSupported() {
    Assertions.assertEquals("colissimo", TrackingCarrierRules.inferSupportedCarrier("LA123456789FR"));
    Assertions.assertEquals("colissimo", TrackingCarrierRules.inferSupportedCarrier("6A28526502105"));
    Assertions.assertEquals("colissimo", TrackingCarrierRules.inferSupportedCarrier("05308083313940F"));
    Assertions.assertNull(TrackingCarrierRules.inferSupportedCarrier("XY123456789FR"));
    Assertions.assertNull(TrackingCarrierRules.inferSupportedCarrier("12345678"));
  }

  @Test
  void validatesOnlyColissimoCarrierSpecificFormats() {
    Assertions.assertTrue(TrackingCarrierRules.isValidForCarrier("LA123456789FR", "colissimo"));
    Assertions.assertTrue(TrackingCarrierRules.isValidForCarrier("05308083313940F", "colissimo"));
    Assertions.assertFalse(TrackingCarrierRules.isValidForCarrier("XY123456789FR", "colissimo"));
    Assertions.assertFalse(TrackingCarrierRules.isValidForCarrier("12345678", "mondial-relay"));
  }

  @Test
  void centralizesOfficialColissimoTrackingLink() {
    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=LA123456789FR",
        TrackingCarrierRules.officialTrackingUrl("colissimo", "LA123456789FR")
    );
    Assertions.assertEquals(List.of("colissimo"), TrackingCarrierRules.matchSupportedCarriers("LA123456789FR"));
  }
}
