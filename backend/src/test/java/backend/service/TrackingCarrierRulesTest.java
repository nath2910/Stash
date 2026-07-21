package backend.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TrackingCarrierRulesTest {

  @Test
  void infersCarrierOnlyWhenFormatIsUnambiguous() {
    Assertions.assertNull(TrackingCarrierRules.inferSupportedCarrier("LA123456789FR"));
    Assertions.assertEquals("colissimo", TrackingCarrierRules.inferSupportedCarrier("6A28526502105"));
    Assertions.assertNull(TrackingCarrierRules.inferSupportedCarrier("05308083313940F"));
    Assertions.assertEquals("chronopost", TrackingCarrierRules.inferSupportedCarrier("XR646836167TS"));
    Assertions.assertEquals("chronopost", TrackingCarrierRules.inferSupportedCarrier("XY123456789FR"));
    Assertions.assertNull(TrackingCarrierRules.inferSupportedCarrier("12345678"));
  }

  @Test
  void validatesColissimoAndChronopostCarrierSpecificFormats() {
    Assertions.assertTrue(TrackingCarrierRules.isValidForCarrier("LA123456789FR", "colissimo"));
    Assertions.assertTrue(TrackingCarrierRules.isValidForCarrier("05308083313940F", "colissimo"));
    Assertions.assertTrue(TrackingCarrierRules.isValidForCarrier("XR646836167TS", "chronopost"));
    Assertions.assertTrue(TrackingCarrierRules.isValidForCarrier("XY123456789FR", "chronopost"));
    Assertions.assertFalse(TrackingCarrierRules.isValidForCarrier("XR646836167TS", "colissimo"));
    Assertions.assertFalse(TrackingCarrierRules.isValidForCarrier("12345678", "mondial-relay"));
  }

  @Test
  void centralizesOfficialTrackingLinks() {
    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=LA123456789FR",
        TrackingCarrierRules.officialTrackingUrl("colissimo", "LA123456789FR")
    );
    Assertions.assertEquals(
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XR646836167TS&langue=fr_FR",
        TrackingCarrierRules.officialTrackingUrl("chronopost", "XR646836167TS")
    );
    Assertions.assertEquals(List.of("colissimo", "chronopost"), TrackingCarrierRules.matchSupportedCarriers("LA123456789FR"));
    Assertions.assertEquals(List.of("colissimo", "chronopost"), TrackingCarrierRules.matchSupportedCarriers("05308083313940F"));
  }
}
