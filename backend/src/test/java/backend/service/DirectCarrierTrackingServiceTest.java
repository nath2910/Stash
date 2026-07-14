package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.repository.ParcelRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DirectCarrierTrackingServiceTest {

  @Test
  void prefersConfiguredPublicClientOverUnconfiguredApiClient() {
    CarrierTrackingClient apiClient = Mockito.mock(CarrierTrackingClient.class);
    CarrierTrackingClient publicClient = Mockito.mock(CarrierTrackingClient.class);
    ParcelTrackingUpdateService updateService = Mockito.mock(ParcelTrackingUpdateService.class);
    ParcelRepository parcelRepository = Mockito.mock(ParcelRepository.class);
    Parcel parcel = parcel(12L, "chronopost");

    Mockito.when(apiClient.supports(parcel)).thenReturn(true);
    Mockito.when(apiClient.isConfigured()).thenReturn(false);
    Mockito.when(publicClient.supports(parcel)).thenReturn(true);
    Mockito.when(publicClient.isConfigured()).thenReturn(true);
    Mockito.when(publicClient.fetchTracking(parcel)).thenReturn(Optional.of(new TrackingSnapshot(
        "PUBLIC_TRACKING_PAGE",
        parcel.getNormalizedTrackingNumber(),
        "chronopost",
        ParcelStatus.IN_TRANSIT,
        "Colis en transit",
        null,
        null,
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR",
        null,
        null,
        null,
        null,
        java.util.Map.of(),
        List.of()
    )));
    Mockito.when(parcelRepository.save(parcel)).thenReturn(parcel);

    DirectCarrierTrackingService service = new DirectCarrierTrackingService(
        List.of(apiClient, publicClient),
        updateService,
        parcelRepository
    );

    service.refreshTracking(parcel);

    Mockito.verify(publicClient).fetchTracking(parcel);
    Mockito.verify(updateService).applySnapshot(Mockito.eq(parcel), Mockito.any(TrackingSnapshot.class));
    Mockito.verify(updateService, Mockito.never()).markLocalFallback(
        Mockito.eq(parcel),
        Mockito.eq(DirectCarrierTrackingService.PROVIDER),
        Mockito.anyString()
    );
  }

  @Test
  void triesNextConfiguredClientWhenFirstOneReturnsNoSnapshot() {
    CarrierTrackingClient publicClient = Mockito.mock(CarrierTrackingClient.class);
    CarrierTrackingClient apiClient = Mockito.mock(CarrierTrackingClient.class);
    ParcelTrackingUpdateService updateService = Mockito.mock(ParcelTrackingUpdateService.class);
    ParcelRepository parcelRepository = Mockito.mock(ParcelRepository.class);
    Parcel parcel = parcel(13L, "chronopost");

    Mockito.when(publicClient.supports(parcel)).thenReturn(true);
    Mockito.when(publicClient.isConfigured()).thenReturn(true);
    Mockito.when(publicClient.fetchTracking(parcel)).thenReturn(Optional.empty());
    Mockito.when(apiClient.supports(parcel)).thenReturn(true);
    Mockito.when(apiClient.isConfigured()).thenReturn(true);
    Mockito.when(apiClient.fetchTracking(parcel)).thenReturn(Optional.of(new TrackingSnapshot(
        "LA_POSTE_OKAPI",
        parcel.getNormalizedTrackingNumber(),
        "chronopost",
        ParcelStatus.DELIVERED,
        "Votre colis a ete livre",
        null,
        null,
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR",
        null,
        null,
        null,
        null,
        java.util.Map.of(),
        List.of()
    )));
    Mockito.when(parcelRepository.save(parcel)).thenReturn(parcel);

    DirectCarrierTrackingService service = new DirectCarrierTrackingService(
        List.of(publicClient, apiClient),
        updateService,
        parcelRepository
    );

    service.refreshTracking(parcel);

    Mockito.verify(publicClient).fetchTracking(parcel);
    Mockito.verify(apiClient).fetchTracking(parcel);
    Mockito.verify(updateService).applySnapshot(Mockito.eq(parcel), Mockito.any(TrackingSnapshot.class));
  }

  @Test
  void prefersDeliveredSnapshotOverEarlierStaleTransitSnapshot() {
    CarrierTrackingClient laposteClient = Mockito.mock(CarrierTrackingClient.class);
    CarrierTrackingClient publicClient = Mockito.mock(CarrierTrackingClient.class);
    ParcelTrackingUpdateService updateService = Mockito.mock(ParcelTrackingUpdateService.class);
    ParcelRepository parcelRepository = Mockito.mock(ParcelRepository.class);
    Parcel parcel = parcel(14L, "chronopost");

    Mockito.when(laposteClient.supports(parcel)).thenReturn(true);
    Mockito.when(laposteClient.isConfigured()).thenReturn(true);
    Mockito.when(laposteClient.fetchTracking(parcel)).thenReturn(Optional.of(new TrackingSnapshot(
        "LA_POSTE_OKAPI",
        parcel.getNormalizedTrackingNumber(),
        "chronopost",
        ParcelStatus.IN_TRANSIT,
        "Votre colis est en transit",
        null,
        null,
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR",
        null,
        null,
        null,
        null,
        java.util.Map.of(),
        List.of(new TrackingEventSnapshot(
            ParcelStatus.IN_TRANSIT,
            "ET1",
            "Votre colis est en transit",
            null,
            java.time.OffsetDateTime.parse("2026-07-12T08:00:00Z"),
            java.util.Map.of()
        ))
    )));

    Mockito.when(publicClient.supports(parcel)).thenReturn(true);
    Mockito.when(publicClient.isConfigured()).thenReturn(true);
    Mockito.when(publicClient.fetchTracking(parcel)).thenReturn(Optional.of(new TrackingSnapshot(
        "PUBLIC_TRACKING_PAGE",
        parcel.getNormalizedTrackingNumber(),
        "chronopost",
        ParcelStatus.DELIVERED,
        "Votre colis a été livré",
        null,
        java.time.OffsetDateTime.parse("2026-07-12T10:15:00Z"),
        "https://www.chronopost.fr/tracking-no-cms/suivi-page?listeNumerosLT=XY123456789FR",
        null,
        null,
        null,
        null,
        java.util.Map.of(),
        List.of()
    )));
    Mockito.when(parcelRepository.save(parcel)).thenReturn(parcel);

    DirectCarrierTrackingService service = new DirectCarrierTrackingService(
        List.of(laposteClient, publicClient),
        updateService,
        parcelRepository
    );

    service.refreshTracking(parcel);

    ArgumentCaptor<TrackingSnapshot> captor = ArgumentCaptor.forClass(TrackingSnapshot.class);
    Mockito.verify(updateService).applySnapshot(Mockito.eq(parcel), captor.capture());
    Assertions.assertEquals(ParcelStatus.DELIVERED, captor.getValue().status());
    Assertions.assertEquals("PUBLIC_TRACKING_PAGE", captor.getValue().provider());
  }

  @Test
  void writesClientDiagnosticsWhenNoCarrierSnapshotIsReturned() {
    CarrierTrackingClient chronopostClient = Mockito.mock(CarrierTrackingClient.class);
    CarrierTrackingClient publicClient = Mockito.mock(CarrierTrackingClient.class);
    ParcelTrackingUpdateService updateService = Mockito.mock(ParcelTrackingUpdateService.class);
    ParcelRepository parcelRepository = Mockito.mock(ParcelRepository.class);
    Parcel parcel = parcel(15L, "chronopost");

    Mockito.when(chronopostClient.supports(parcel)).thenReturn(true);
    Mockito.when(chronopostClient.isConfigured()).thenReturn(true);
    Mockito.when(chronopostClient.fetchTracking(parcel)).thenThrow(new IllegalStateException("Connection refused"));

    Mockito.when(publicClient.supports(parcel)).thenReturn(true);
    Mockito.when(publicClient.isConfigured()).thenReturn(true);
    Mockito.when(publicClient.fetchTracking(parcel)).thenReturn(Optional.empty());

    Mockito.when(parcelRepository.save(parcel)).thenReturn(parcel);

    DirectCarrierTrackingService service = new DirectCarrierTrackingService(
        List.of(chronopostClient, publicClient),
        updateService,
        parcelRepository
    );

    service.refreshTracking(parcel);

    ArgumentCaptor<String> labelCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(updateService).markLocalFallback(
        Mockito.eq(parcel),
        Mockito.eq(DirectCarrierTrackingService.PROVIDER),
        labelCaptor.capture()
    );
    Assertions.assertTrue(labelCaptor.getValue().contains("IllegalStateException"));
    Assertions.assertTrue(labelCaptor.getValue().contains("vide"));
  }

  private Parcel parcel(Long id, String carrierSlug) {
    Parcel parcel = new Parcel();
    parcel.setId(id);
    parcel.setCarrierSlug(carrierSlug);
    parcel.setTrackingNumber("XY123456789FR");
    parcel.setNormalizedTrackingNumber("XY123456789FR");
    return parcel;
  }
}
