package backend.service;

import backend.entity.Parcel;
import backend.repository.ParcelRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectCarrierTrackingService {

  public static final String PROVIDER = "DIRECT_CARRIER";

  private final LaPosteTrackingClient laPosteTrackingClient;
  private final ParcelTrackingUpdateService parcelTrackingUpdateService;
  private final ParcelRepository parcelRepository;

  public DirectCarrierTrackingService(
      LaPosteTrackingClient laPosteTrackingClient,
      ParcelTrackingUpdateService parcelTrackingUpdateService,
      ParcelRepository parcelRepository
  ) {
    this.laPosteTrackingClient = laPosteTrackingClient;
    this.parcelTrackingUpdateService = parcelTrackingUpdateService;
    this.parcelRepository = parcelRepository;
  }

  public boolean supports(Parcel parcel) {
    return laPosteTrackingClient.supports(parcel);
  }

  @Transactional
  public Parcel refreshTracking(Parcel parcel) {
    if (parcel == null || parcel.getId() == null) {
      return parcel;
    }
    if (!supports(parcel)) {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Suivi reserve aux colis Colissimo");
      return parcelRepository.save(parcel);
    }
    if (!laPosteTrackingClient.isConfigured()) {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Source La Poste indisponible");
      return parcelRepository.save(parcel);
    }

    Optional<TrackingSnapshot> snapshot = laPosteTrackingClient.fetchTracking(parcel);
    if (snapshot.isPresent()) {
      parcelTrackingUpdateService.applySnapshot(parcel, snapshot.get());
    } else {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Statut Colissimo indisponible");
    }
    return parcelRepository.save(parcel);
  }
}
