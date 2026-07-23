package backend.service;

import backend.entity.Parcel;
import backend.repository.ParcelRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectCarrierTrackingService {

  public static final String PROVIDER = "DIRECT_CARRIER";

  private final LaPosteTrackingClient laPosteTrackingClient;
  private final ChronopostTrackingClient chronopostTrackingClient;
  private final List<CarrierTrackingClient> carrierTrackingClients;
  private final ParcelTrackingUpdateService parcelTrackingUpdateService;
  private final ParcelRepository parcelRepository;

  public DirectCarrierTrackingService(
      LaPosteTrackingClient laPosteTrackingClient,
      ChronopostTrackingClient chronopostTrackingClient,
      ParcelTrackingUpdateService parcelTrackingUpdateService,
      ParcelRepository parcelRepository
  ) {
    this.laPosteTrackingClient = laPosteTrackingClient;
    this.chronopostTrackingClient = chronopostTrackingClient;
    this.carrierTrackingClients = List.of(laPosteTrackingClient, chronopostTrackingClient);
    this.parcelTrackingUpdateService = parcelTrackingUpdateService;
    this.parcelRepository = parcelRepository;
  }

  public boolean supports(Parcel parcel) {
    return resolveClient(parcel).isPresent();
  }

  @Transactional
  public Parcel refreshTracking(Parcel parcel) {
    if (parcel == null || parcel.getId() == null) {
      return parcel;
    }
    Optional<CarrierTrackingClient> resolvedClient = resolveClient(parcel);
    if (resolvedClient.isEmpty()) {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Transporteur non gere pour le suivi direct");
      return parcelRepository.save(parcel);
    }
    CarrierTrackingClient client = resolvedClient.get();
    if (!client.isConfigured()) {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, unavailableSourceMessage(parcel, client));
      return parcelRepository.save(parcel);
    }

    Optional<TrackingSnapshot> snapshot = client.fetchTracking(parcel);
    if (snapshot.isPresent()) {
      parcelTrackingUpdateService.applySnapshot(parcel, snapshot.get());
    } else {
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, unavailableStatusMessage(parcel));
    }
    return parcelRepository.save(parcel);
  }

  private Optional<CarrierTrackingClient> resolveClient(Parcel parcel) {
    String carrier = normalizedCarrier(parcel);
    if ("colissimo".equals(carrier)) {
      return laPosteTrackingClient.supports(parcel) ? Optional.of(laPosteTrackingClient) : Optional.empty();
    }
    if ("chronopost".equals(carrier)) {
      return chronopostTrackingClient.supports(parcel) ? Optional.of(chronopostTrackingClient) : Optional.empty();
    }
    return carrierTrackingClients.stream().filter(client -> client.supports(parcel)).findFirst();
  }

  private String unavailableSourceMessage(Parcel parcel, CarrierTrackingClient client) {
    String baseMessage = switch (normalizedCarrier(parcel)) {
      case "chronopost" -> "Source Chronopost indisponible";
      case "colissimo" -> "Source La Poste indisponible";
      default -> "Source transporteur indisponible";
    };
    String detail = unavailableSourceDetail(client);
    if (detail == null || detail.isBlank()) {
      return baseMessage;
    }
    return baseMessage + ": " + detail;
  }

  private String unavailableStatusMessage(Parcel parcel) {
    return switch (normalizedCarrier(parcel)) {
      case "chronopost" -> "Statut Chronopost indisponible";
      case "colissimo" -> "Statut Colissimo indisponible";
      default -> "Statut transporteur indisponible";
    };
  }

  private String normalizedCarrier(Parcel parcel) {
    return parcel == null ? null : TrackingCarrierRules.normalizeCarrierSlug(parcel.getCarrierSlug());
  }

  private String unavailableSourceDetail(CarrierTrackingClient client) {
    if (client instanceof LaPosteTrackingClient laPosteTrackingClient) {
      return laPosteTrackingClient.unavailableReason();
    }
    return null;
  }
}
