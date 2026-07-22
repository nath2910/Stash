package backend.service;

import backend.entity.Parcel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LaPosteTrackingClientTest {

  @Test
  void doesNotBlockProdStartupWhenLaposteSourceIsMissing() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    LaPosteTrackingClient client = new LaPosteTrackingClient(
        "",
        "https://api.laposte.fr/suivi/v2/idships",
        new ObjectMapper(),
        environment
    );

    Assertions.assertDoesNotThrow(client::validateProductionConfig);
  }

  @Test
  void usesEstimDateWhenLaposteDoesNotProvideEstimatedDeliveryDate() throws Exception {
    MockEnvironment environment = new MockEnvironment();
    ObjectMapper objectMapper = new ObjectMapper();
    LaPosteTrackingClient client = new LaPosteTrackingClient(
        "",
        "https://api.laposte.fr/suivi/v2/idships",
        objectMapper,
        environment
    );

    Parcel parcel = new Parcel();
    parcel.setTrackingNumber("6Y11138937441");
    parcel.setNormalizedTrackingNumber("6Y11138937441");
    parcel.setCarrierSlug("colissimo");

    Map<String, Object> response = objectMapper.readValue(
        """
            {
              "returnCode": 200,
              "shipment": {
                "idShip": "6Y11138937441",
                "product": "colissimo",
                "estimDate": "2026-07-23T02:00:00+02:00",
                "event": [
                  {
                    "code": "ET1",
                    "label": "Votre colis est en transit sur nos plateformes logistiques.",
                    "date": "2026-07-21T17:32:09+02:00"
                  }
                ],
                "contextData": {
                  "originCountry": "FR",
                  "arrivalCountry": "FR"
                },
                "url": "https://www.laposte.fr/outils/suivre-vos-envois?code=6Y11138937441"
              }
            }
            """,
        new TypeReference<>() {}
    );

    var method = LaPosteTrackingClient.class.getDeclaredMethod("toSnapshot", Parcel.class, Map.class);
    method.setAccessible(true);
    TrackingSnapshot snapshot = (TrackingSnapshot) method.invoke(client, parcel, response);

    Assertions.assertEquals(
        OffsetDateTime.parse("2026-07-23T02:00:00+02:00"),
        snapshot.estimatedDeliveryAt()
    );
    Assertions.assertEquals("Votre colis est en transit sur nos plateformes logistiques.", snapshot.statusLabel());
  }
}
