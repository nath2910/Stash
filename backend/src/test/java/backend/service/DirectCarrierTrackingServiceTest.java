package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.repository.ParcelRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DirectCarrierTrackingServiceTest {

  @Test
  void appliesLaPosteSnapshotWhenConfigured() {
    LaPosteTrackingClient laPosteTrackingClient = Mockito.mock(LaPosteTrackingClient.class);
    ParcelTrackingUpdateService updateService = Mockito.mock(ParcelTrackingUpdateService.class);
    ParcelRepository parcelRepository = Mockito.mock(ParcelRepository.class);
    Parcel parcel = parcel(12L);

    Mockito.when(laPosteTrackingClient.supports(parcel)).thenReturn(true);
    Mockito.when(laPosteTrackingClient.isConfigured()).thenReturn(true);
    Mockito.when(laPosteTrackingClient.fetchTracking(parcel)).thenReturn(Optional.of(new TrackingSnapshot(
        "LA_POSTE_BROWSER_PAGE",
        parcel.getNormalizedTrackingNumber(),
        "colissimo",
        ParcelStatus.DELIVERED,
        "Votre colis est livre",
        null,
        java.time.OffsetDateTime.parse("2026-07-19T10:15:00Z"),
        "https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970",
        null,
        null,
        null,
        null,
        Map.of(),
        List.of()
    )));
    Mockito.when(parcelRepository.save(parcel)).thenReturn(parcel);

    DirectCarrierTrackingService service = new DirectCarrierTrackingService(
        laPosteTrackingClient,
        updateService,
        parcelRepository
    );

    service.refreshTracking(parcel);

    Mockito.verify(updateService).applySnapshot(Mockito.eq(parcel), Mockito.any(TrackingSnapshot.class));
    Mockito.verify(updateService, Mockito.never()).markLocalFallback(
        Mockito.eq(parcel),
        Mockito.eq(DirectCarrierTrackingService.PROVIDER),
        Mockito.anyString()
    );
  }

  @Test
  void marksParcelWhenLaPosteSourceIsUnavailable() {
    LaPosteTrackingClient laPosteTrackingClient = Mockito.mock(LaPosteTrackingClient.class);
    ParcelTrackingUpdateService updateService = Mockito.mock(ParcelTrackingUpdateService.class);
    ParcelRepository parcelRepository = Mockito.mock(ParcelRepository.class);
    Parcel parcel = parcel(13L);

    Mockito.when(laPosteTrackingClient.supports(parcel)).thenReturn(true);
    Mockito.when(laPosteTrackingClient.isConfigured()).thenReturn(false);
    Mockito.when(parcelRepository.save(parcel)).thenReturn(parcel);

    DirectCarrierTrackingService service = new DirectCarrierTrackingService(
        laPosteTrackingClient,
        updateService,
        parcelRepository
    );

    service.refreshTracking(parcel);

    Mockito.verify(updateService).markLocalFallback(
        Mockito.eq(parcel),
        Mockito.eq(DirectCarrierTrackingService.PROVIDER),
        Mockito.eq("Source La Poste indisponible")
    );
  }

  @Test
  void refusesNonColissimoParcels() {
    LaPosteTrackingClient laPosteTrackingClient = Mockito.mock(LaPosteTrackingClient.class);
    ParcelTrackingUpdateService updateService = Mockito.mock(ParcelTrackingUpdateService.class);
    ParcelRepository parcelRepository = Mockito.mock(ParcelRepository.class);
    Parcel parcel = parcel(14L);
    parcel.setCarrierSlug("chronopost");

    Mockito.when(laPosteTrackingClient.supports(parcel)).thenReturn(false);
    Mockito.when(parcelRepository.save(parcel)).thenReturn(parcel);

    DirectCarrierTrackingService service = new DirectCarrierTrackingService(
        laPosteTrackingClient,
        updateService,
        parcelRepository
    );

    service.refreshTracking(parcel);

    Mockito.verify(updateService).markLocalFallback(
        Mockito.eq(parcel),
        Mockito.eq(DirectCarrierTrackingService.PROVIDER),
        Mockito.eq("Suivi reserve aux colis Colissimo")
    );
  }

  private Parcel parcel(Long id) {
    Parcel parcel = new Parcel();
    parcel.setId(id);
    parcel.setCarrierSlug("colissimo");
    parcel.setTrackingNumber("6A04296519970");
    parcel.setNormalizedTrackingNumber("6A04296519970");
    Assertions.assertEquals("colissimo", parcel.getCarrierSlug());
    return parcel;
  }
}
