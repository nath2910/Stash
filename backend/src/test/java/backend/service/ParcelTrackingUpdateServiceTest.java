package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.repository.ParcelEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ParcelTrackingUpdateServiceTest {

  @Test
  void appliesSnapshotAndCreatesTimelineEvent() {
    ParcelEventRepository eventRepository = Mockito.mock(ParcelEventRepository.class);
    ParcelTrackingUpdateService service = new ParcelTrackingUpdateService(eventRepository);
    Parcel parcel = new Parcel();
    parcel.setId(42L);
    parcel.setStatus(ParcelStatus.PENDING);

    OffsetDateTime eventTime = OffsetDateTime.of(2026, 5, 19, 12, 0, 0, 0, ZoneOffset.UTC);
    TrackingSnapshot snapshot = new TrackingSnapshot(
        "LA_POSTE_OKAPI",
        "LA123456789FR",
        "colissimo",
        ParcelStatus.IN_TRANSIT,
        "Votre colis est en transit",
        null,
        null,
        "https://www.laposte.fr/outils/suivre-vos-envois?code=LA123456789FR",
        "FR",
        "FR",
        "Colissimo",
        null,
        Map.of("source", "test"),
        List.of(new TrackingEventSnapshot(
            ParcelStatus.IN_TRANSIT,
            "ET1",
            "Votre colis est en transit",
            "Paris",
            eventTime,
            Map.of("code", "ET1")
        ))
    );

    service.applySnapshot(parcel, snapshot);

    Assertions.assertEquals("LA_POSTE_OKAPI", parcel.getAggregator());
    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, parcel.getStatus());
    Assertions.assertEquals(eventTime, parcel.getLastEventAt());
    Assertions.assertEquals("FR", parcel.getRawCurrentPayload().get("destination_raw_location"));
    Mockito.verify(eventRepository).save(Mockito.any());
  }
}
