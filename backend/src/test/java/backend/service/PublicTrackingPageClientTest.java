package backend.service;

import backend.entity.Parcel;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PublicTrackingPageClientTest {

  private final PublicTrackingPageClient client = new PublicTrackingPageClient();

  @Test
  void supportsTrustedTrackingUrlEvenWhenCarrierWasUnknown() {
    Parcel parcel = new Parcel();
    parcel.setCarrierSlug("unknown");
    parcel.setTrackingNumber("XY123456789FR");
    parcel.setNormalizedTrackingNumber("XY123456789FR");
    HashMap<String, Object> payload = new HashMap<>();
    payload.put("tracking_url", "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR");
    parcel.setRawCurrentPayload(payload);

    Assertions.assertTrue(client.supports(parcel));
  }

  @Test
  void detectsCloudflareChallengeAsNonTrackingPage() {
    Assertions.assertTrue(PublicTrackingPageClient.looksLikeBotChallenge("""
        <html>
          <head><title>Just a moment...</title></head>
          <body>Enable JavaScript and cookies to continue</body>
        </html>
        """));
  }

  @Test
  void detectsAkamaiAccessDeniedAsNonTrackingPage() {
    Assertions.assertTrue(PublicTrackingPageClient.looksLikeBotChallenge("""
        <html>
          <head><title>Access Denied</title></head>
          <body>You don't have permission to access this resource.</body>
        </html>
        """));
  }
}
