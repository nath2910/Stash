package backend.dto;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParcelResponseTest {

  @Test
  void fallsBackToTrustedCarrierUrlWhenPayloadContainsMerchantLink() {
    Parcel parcel = new Parcel();
    parcel.setId(1L);
    parcel.setCarrierSlug("ups");
    parcel.setTrackingNumber("1Z999AA10123456784");
    parcel.setNormalizedTrackingNumber("1Z999AA10123456784");
    parcel.setStatus(ParcelStatus.IN_TRANSIT);
    HashMap<String, Object> payload = new HashMap<>();
    payload.put("tracking_url", "https://www.hhv.de/account/track?code=1Z999AA10123456784");
    parcel.setRawCurrentPayload(payload);

    ParcelResponse response = ParcelResponse.fromEntity(parcel, List.of());

    Assertions.assertEquals(
        "https://www.ups.com/track?tracknum=1Z999AA10123456784",
        response.trackingUrl()
    );
  }
}
