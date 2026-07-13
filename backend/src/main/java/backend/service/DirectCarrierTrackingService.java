package backend.service;

import backend.entity.Parcel;
import backend.repository.ParcelRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectCarrierTrackingService {

  public static final String PROVIDER = "DIRECT_CARRIER";

  private static final Logger log = LoggerFactory.getLogger(DirectCarrierTrackingService.class);

  private final List<CarrierTrackingClient> clients;
  private final ParcelTrackingUpdateService parcelTrackingUpdateService;
  private final ParcelRepository parcelRepository;

  public DirectCarrierTrackingService(
      List<CarrierTrackingClient> clients,
      ParcelTrackingUpdateService parcelTrackingUpdateService,
      ParcelRepository parcelRepository
  ) {
    this.clients = clients;
    this.parcelTrackingUpdateService = parcelTrackingUpdateService;
    this.parcelRepository = parcelRepository;
  }

  public boolean supports(Parcel parcel) {
    return !supportingClients(parcel).isEmpty();
  }

  @Transactional
  public Parcel refreshTracking(Parcel parcel) {
    if (parcel == null || parcel.getId() == null) {
      return parcel;
    }

    List<CarrierTrackingClient> supportingClients = supportingClients(parcel);
    if (supportingClients.isEmpty()) {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Transporteur direct non supporte");
      return parcelRepository.save(parcel);
    }

    List<CarrierTrackingClient> configuredClients = supportingClients.stream()
        .filter(CarrierTrackingClient::isConfigured)
        .toList();
    if (configuredClients.isEmpty()) {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "API transporteur non configuree");
      return parcelRepository.save(parcel);
    }

    for (CarrierTrackingClient trackingClient : configuredClients) {
      try {
        Optional<TrackingSnapshot> snapshot = trackingClient.fetchTracking(parcel);
        if (snapshot.isPresent()) {
          parcelTrackingUpdateService.applySnapshot(parcel, snapshot.get());
          return parcelRepository.save(parcel);
        }
      } catch (Exception ex) {
        log.warn("Direct carrier tracking failed for parcel {} with client {}", parcel.getId(), trackingClient.getClass().getSimpleName());
      }
    }

    parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Suivi non trouve chez le transporteur");
    return parcelRepository.save(parcel);
  }

  private List<CarrierTrackingClient> supportingClients(Parcel parcel) {
    if (parcel == null) {
      return List.of();
    }
    return clients.stream()
        .filter(client -> client.supports(parcel))
        .toList();
  }
}
