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

  private static final Logger log = LoggerFactory.getLogger(DirectCarrierTrackingService.class);
  private static final String CHECKPOINT_PREFIX = "[TRACKING][CHECKPOINT]";
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
    long startedAt = System.currentTimeMillis();
    log.info(
        "{} step=refresh.start parcelId={} carrier={} tracking={} aggregator={}",
        CHECKPOINT_PREFIX,
        parcel.getId(),
        normalizedCarrier(parcel),
        parcel.getNormalizedTrackingNumber(),
        parcel.getAggregator()
    );
    Optional<CarrierTrackingClient> resolvedClient = resolveClient(parcel);
    if (resolvedClient.isEmpty()) {
      log.warn(
          "{} step=refresh.client_unresolved parcelId={} carrier={} tracking={}",
          CHECKPOINT_PREFIX,
          parcel.getId(),
          normalizedCarrier(parcel),
          parcel.getNormalizedTrackingNumber()
      );
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, "Transporteur non gere pour le suivi direct");
      logSlowRefresh(parcel, "none", startedAt, "unsupported");
      return parcelRepository.save(parcel);
    }
    CarrierTrackingClient client = resolvedClient.get();
    log.info(
        "{} step=refresh.client_resolved parcelId={} carrier={} tracking={} client={}",
        CHECKPOINT_PREFIX,
        parcel.getId(),
        normalizedCarrier(parcel),
        parcel.getNormalizedTrackingNumber(),
        clientName(client)
    );
    if (!client.isConfigured()) {
      log.warn(
          "{} step=refresh.client_not_configured parcelId={} carrier={} tracking={} client={} reason={}",
          CHECKPOINT_PREFIX,
          parcel.getId(),
          normalizedCarrier(parcel),
          parcel.getNormalizedTrackingNumber(),
          clientName(client),
          unavailableSourceDetail(client)
      );
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, unavailableSourceMessage(parcel, client));
      logSlowRefresh(parcel, clientName(client), startedAt, "not_configured");
      return parcelRepository.save(parcel);
    }

    log.info(
        "{} step=refresh.fetch_start parcelId={} carrier={} tracking={} client={}",
        CHECKPOINT_PREFIX,
        parcel.getId(),
        normalizedCarrier(parcel),
        parcel.getNormalizedTrackingNumber(),
        clientName(client)
    );
    Optional<TrackingSnapshot> snapshot = client.fetchTracking(parcel);
    if (snapshot.isPresent()) {
      TrackingSnapshot trackingSnapshot = snapshot.get();
      log.info(
          "{} step=refresh.fetch_success parcelId={} carrier={} tracking={} client={} provider={} status={} label={} events={}",
          CHECKPOINT_PREFIX,
          parcel.getId(),
          normalizedCarrier(parcel),
          parcel.getNormalizedTrackingNumber(),
          clientName(client),
          trackingSnapshot.provider(),
          trackingSnapshot.status(),
          trackingSnapshot.statusLabel(),
          trackingSnapshot.events() == null ? 0 : trackingSnapshot.events().size()
      );
      parcelTrackingUpdateService.applySnapshot(parcel, trackingSnapshot);
      logSlowRefresh(parcel, clientName(client), startedAt, "snapshot");
    } else {
      log.warn(
          "{} step=refresh.fetch_empty parcelId={} carrier={} tracking={} client={} fallbackLabel={}",
          CHECKPOINT_PREFIX,
          parcel.getId(),
          normalizedCarrier(parcel),
          parcel.getNormalizedTrackingNumber(),
          clientName(client),
          unavailableStatusMessage(parcel)
      );
      parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, unavailableStatusMessage(parcel));
      logSlowRefresh(parcel, clientName(client), startedAt, "empty");
    }
    Parcel savedParcel = parcelRepository.save(parcel);
    log.info(
        "{} step=refresh.save_complete parcelId={} carrier={} tracking={} status={} label={} aggregator={}",
        CHECKPOINT_PREFIX,
        savedParcel.getId(),
        normalizedCarrier(savedParcel),
        savedParcel.getNormalizedTrackingNumber(),
        savedParcel.getStatus(),
        savedParcel.getStatusLabel(),
        savedParcel.getAggregator()
    );
    return savedParcel;
  }

  private Optional<CarrierTrackingClient> resolveClient(Parcel parcel) {
    String carrier = normalizedCarrier(parcel);
    if ("colissimo".equals(carrier)) {
      return laPosteTrackingClient.supports(parcel) ? Optional.of(laPosteTrackingClient) : Optional.empty();
    }
    if ("chronopost".equals(carrier)) {
      if (laPosteTrackingClient.supports(parcel)) {
        return Optional.of(laPosteTrackingClient);
      }
      return chronopostTrackingClient.supports(parcel) ? Optional.of(chronopostTrackingClient) : Optional.empty();
    }
    return carrierTrackingClients.stream().filter(client -> client.supports(parcel)).findFirst();
  }

  private String unavailableSourceMessage(Parcel parcel, CarrierTrackingClient client) {
    String baseMessage = client instanceof LaPosteTrackingClient
        ? "Source La Poste indisponible"
        : client instanceof ChronopostTrackingClient
            ? "Source Chronopost indisponible"
            : "Source transporteur indisponible";
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

  private void logSlowRefresh(Parcel parcel, String clientName, long startedAt, String outcome) {
    long durationMs = System.currentTimeMillis() - startedAt;
    if (durationMs < 5000) {
      return;
    }
    log.warn(
        "Delivery tracking refresh was slow for parcel {} ({}), carrier {}, client {}, outcome {}, duration {} ms",
        parcel.getId(),
        parcel.getTrackingNumber(),
        normalizedCarrier(parcel),
        clientName,
        outcome,
        durationMs
    );
  }

  private String clientName(CarrierTrackingClient client) {
    return client == null ? "none" : client.getClass().getSimpleName();
  }
}
