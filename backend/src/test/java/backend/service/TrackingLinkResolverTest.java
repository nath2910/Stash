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
}
