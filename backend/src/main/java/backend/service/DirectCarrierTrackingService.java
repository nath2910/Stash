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
    return clientFor(parcel).isPresent();
  }

  @Transactional
  public Parcel refreshTracking(Parcel parcel) {
    if (parcel == null || parcel.getId() == null) {
      return parcel;
    }

    Optional<CarrierTrackingClient> client = clientFor(parcel);
    if (client.isEmpty()) {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Transporteur direct non supporte");
      return parcelRepository.save(parcel);
    }

    CarrierTrackingClient trackingClient = client.get();
    if (!trackingClient.isConfigured()) {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "API transporteur non configuree");
      return parcelRepository.save(parcel);
    }

    try {
      Optional<TrackingSnapshot> snapshot = trackingClient.fetchTracking(parcel);
      if (snapshot.isPresent()) {
        parcelTrackingUpdateService.applySnapshot(parcel, snapshot.get());
      } else {
        parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Suivi non trouve chez le transporteur");
      }
    } catch (Exception ex) {
      log.warn("Direct carrier tracking failed for parcel {}", parcel.getId());
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Suivi transporteur indisponible");
    }

    return parcelRepository.save(parcel);
  }

  private Optional<CarrierTrackingClient> clientFor(Parcel parcel) {
    if (parcel == null) {
      return Optional.empty();
    }
    return clients.stream()
        .filter(client -> client.supports(parcel))
        .findFirst();
  }
}
