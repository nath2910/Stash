package backend.service;

import backend.repository.ParcelEventRepository;
import backend.repository.ParcelRepository;
import backend.repository.TrackingWebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AfterShipTrackingClientTest {

  @Test
  void acceptsHmacSignatureOrSharedSecret() throws Exception {
    AfterShipTrackingClient client = new AfterShipTrackingClient(
        "",
        "webhook-secret",
        new ObjectMapper(),
        Mockito.mock(ParcelRepository.class),
        Mockito.mock(ParcelEventRepository.class),
        Mockito.mock(TrackingWebhookEventRepository.class)
    );
    String payload = "{\"event\":\"tracking_update\"}";
    String signature = Base64.getEncoder().encodeToString(hmac(payload, "webhook-secret"));

    Assertions.assertTrue(client.isValidSignature(payload, signature, null));
    Assertions.assertTrue(client.isValidSignature(payload, null, "webhook-secret"));
    Assertions.assertFalse(client.isValidSignature(payload, "bad-signature", null));
  }

  private byte[] hmac(String payload, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
  }
}
