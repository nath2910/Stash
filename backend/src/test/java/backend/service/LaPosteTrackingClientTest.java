package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
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
    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, snapshot.status());
    Assertions.assertEquals("Votre colis est en transit sur nos plateformes logistiques.", snapshot.statusLabel());
  }

  @Test
  void usesTimelineWhenLaposteResponseDoesNotProvideEvents() throws Exception {
    MockEnvironment environment = new MockEnvironment();
    ObjectMapper objectMapper = new ObjectMapper();
    LaPosteTrackingClient client = new LaPosteTrackingClient(
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
                "timeline": [
                  {
                    "shortLabel": "Votre colissimo est pris en charge"
                  },
                  {
                    "shortLabel": "Il est en traitement dans notre réseau."
                  },
                  {
                    "shortLabel": "La livraison de votre Colissimo est prévue"
                  }
                ],
                "event": [],
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

    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, snapshot.status());
    Assertions.assertEquals("Votre colissimo est pris en charge", snapshot.statusLabel());
  }

  @Test
  void detectsMailboxDeliveryFromLaposteEventLabel() throws Exception {
    MockEnvironment environment = new MockEnvironment();
    ObjectMapper objectMapper = new ObjectMapper();
    LaPosteTrackingClient client = new LaPosteTrackingClient(
        objectMapper,
        environment
    );

    Parcel parcel = new Parcel();
    parcel.setTrackingNumber("6A06903191241");
    parcel.setNormalizedTrackingNumber("6A06903191241");
    parcel.setCarrierSlug("colissimo");

    Map<String, Object> response = objectMapper.readValue(
        """
            {
              "returnCode": 200,
              "shipment": {
                "idShip": "6A06903191241",
                "product": "colissimo",
                "deliveryDate": "2026-06-25T09:03:48+02:00",
                "event": [
                  {
                    "code": "DI1",
                    "label": "Votre colis est livré dans votre boite aux lettres.",
                    "date": "2026-06-25T09:03:48+02:00"
                  }
                ],
                "url": "https://www.laposte.fr/outils/suivre-vos-envois?code=6A06903191241"
              }
            }
            """,
        new TypeReference<>() {}
    );

    var method = LaPosteTrackingClient.class.getDeclaredMethod("toSnapshot", Parcel.class, Map.class);
    method.setAccessible(true);
    TrackingSnapshot snapshot = (TrackingSnapshot) method.invoke(client, parcel, response);

    Assertions.assertEquals(ParcelStatus.DELIVERED, snapshot.status());
    Assertions.assertEquals("Votre colis est livré dans votre boite aux lettres.", snapshot.statusLabel());
    Assertions.assertEquals(
        OffsetDateTime.parse("2026-06-25T09:03:48+02:00"),
        snapshot.deliveredAt()
    );
  }
}
