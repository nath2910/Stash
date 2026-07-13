package backend.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TrackingLinkResolverTest {

  @Test
  void detectsCarrierFromTrustedTrackingUrl() {
    Assertions.assertEquals(
        "chronopost",
        TrackingLinkResolver.detectCarrierSlug(
            "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR"
        )
    );
  }

  @Test
  void keepsTrustedTrackingUrlWhenCarrierIsMissing() {
    Assertions.assertEquals(
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR",
        TrackingLinkResolver.preferredTrackingUrl(
            "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR",
            null,
            "XY123456789FR"
        )
    );
  }
}
