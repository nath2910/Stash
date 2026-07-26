package backend.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TrackingLinkResolverTest {

  @Test
  void detectsColissimoFromTrustedTrackingUrl() {
    Assertions.assertEquals(
        "colissimo",
        TrackingLinkResolver.detectCarrierSlug(
            "https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970"
        )
    );
  }

  @Test
  void keepsTrustedColissimoTrackingUrl() {
    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970",
        TrackingLinkResolver.preferredTrackingUrl(
            "https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970",
            "colissimo",
            "6A04296519970"
        )
    );
  }

  @Test
  void detectsChronopostFromTrustedTrackingUrl() {
    Assertions.assertEquals(
        "chronopost",
        TrackingLinkResolver.detectCarrierSlug(
            "https://www.laposte.fr/outils/suivre-vos-envois?code=XR646836167TS"
        )
    );
  }

  @Test
  void keepsTrustedChronopostTrackingUrl() {
    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=XR646836167TS",
        TrackingLinkResolver.preferredTrackingUrl(
            "https://www.laposte.fr/outils/suivre-vos-envois?code=XR646836167TS",
            "chronopost",
            "XR646836167TS"
        )
    );
  }

  @Test
  void fallsBackToOfficialColissimoUrlWhenRawUrlIsUntrusted() {
    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970",
        TrackingLinkResolver.preferredTrackingUrl(
            "https://example.com/track/6A04296519970",
            "colissimo",
            "6A04296519970"
        )
    );
  }

  @Test
  void fallsBackToOfficialChronopostUrlWhenRawUrlIsUntrusted() {
    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=XR646836167TS",
        TrackingLinkResolver.preferredTrackingUrl(
            "https://example.com/track/XR646836167TS",
            "chronopost",
            "XR646836167TS"
        )
    );
  }
}
