package backend.dto;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParcelResponseTest {

  @Test
  void keepsOfficialColissimoTrackingUrl() {
    Parcel parcel = new Parcel();
    parcel.setId(1L);
    parcel.setCarrierSlug("colissimo");
    parcel.setTrackingNumber("6A04296519970");
    parcel.setNormalizedTrackingNumber("6A04296519970");
    parcel.setStatus(ParcelStatus.IN_TRANSIT);
    HashMap<String, Object> payload = new HashMap<>();
    payload.put("tracking_url", "https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970");
    parcel.setRawCurrentPayload(payload);

    ParcelResponse response = ParcelResponse.fromEntity(parcel, List.of());

    Assertions.assertEquals(
        "https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970",
        response.trackingUrl()
    );
  }
}
