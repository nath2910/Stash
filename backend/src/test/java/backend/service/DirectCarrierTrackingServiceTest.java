package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.repository.ParcelRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

  private Parcel parcel(Long id, String carrierSlug) {
    Parcel parcel = new Parcel();
    parcel.setId(id);
    parcel.setCarrierSlug(carrierSlug);
    parcel.setTrackingNumber("XY123456789FR");
    parcel.setNormalizedTrackingNumber("XY123456789FR");
    return parcel;
  }
}
