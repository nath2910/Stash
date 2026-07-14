package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.repository.ParcelRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    TrackingSnapshot bestSnapshot = null;
    List<String> diagnostics = new ArrayList<>();
    for (CarrierTrackingClient trackingClient : configuredClients) {
      String clientName = trackingClient.getClass().getSimpleName();
      try {
        Optional<TrackingSnapshot> snapshot = trackingClient.fetchTracking(parcel);
        if (snapshot.isPresent()) {
          TrackingSnapshot candidate = snapshot.get();
          if (bestSnapshot == null || isBetterSnapshot(candidate, bestSnapshot)) {
            bestSnapshot = candidate;
          }
        } else {
          diagnostics.add(clientName + ": vide");
        }
      } catch (Exception ex) {
        diagnostics.add(clientName + ": " + summarizeException(ex));
        log.warn("Direct carrier tracking failed for parcel {} with client {}", parcel.getId(), clientName);
      }
    }

    if (bestSnapshot != null) {
      parcelTrackingUpdateService.applySnapshot(parcel, bestSnapshot);
      return parcelRepository.save(parcel);
    }

    parcelTrackingUpdateService.markLocalFallback(parcel, PROVIDER, fallbackLabel(diagnostics));
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

  private boolean isBetterSnapshot(TrackingSnapshot candidate, TrackingSnapshot currentBest) {
    Comparator<TrackingSnapshot> comparator = Comparator
        .comparingInt((TrackingSnapshot snapshot) -> statusPriority(snapshot.status()))
        .thenComparing(snapshot -> snapshot.deliveredAt() != null)
        .thenComparing(snapshot -> safeTime(snapshot.deliveredAt()))
        .thenComparing(snapshot -> safeTime(latestEventTime(snapshot)))
        .thenComparingInt(snapshot -> snapshot.events() == null ? 0 : snapshot.events().size())
        .thenComparing(snapshot -> snapshot.statusLabel() != null && !snapshot.statusLabel().isBlank());
    return comparator.compare(candidate, currentBest) > 0;
  }

  private int statusPriority(ParcelStatus status) {
    if (status == null) {
      return 0;
    }
    return switch (status) {
      case PENDING, UNKNOWN -> 0;
      case REGISTERED -> 1;
      case IN_TRANSIT -> 2;
      case OUT_FOR_DELIVERY -> 3;
      case EXCEPTION -> 4;
      case DELIVERED -> 5;
    };
  }

  private OffsetDateTime latestEventTime(TrackingSnapshot snapshot) {
    if (snapshot == null || snapshot.events() == null || snapshot.events().isEmpty()) {
      return null;
    }
    return snapshot.events().stream()
        .map(TrackingEventSnapshot::eventTime)
        .filter(java.util.Objects::nonNull)
        .max(OffsetDateTime::compareTo)
        .orElse(null);
  }

  private OffsetDateTime safeTime(OffsetDateTime value) {
    return value == null ? OffsetDateTime.MIN : value;
  }

  private String fallbackLabel(List<String> diagnostics) {
    if (diagnostics == null || diagnostics.isEmpty()) {
      return "Suivi non trouve chez le transporteur";
    }
    String summary = String.join(" | ", diagnostics);
    if (summary.length() > 180) {
      summary = summary.substring(0, 177) + "...";
    }
    return summary;
  }

  private String summarizeException(Exception ex) {
    Throwable current = ex;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    String type = current.getClass().getSimpleName();
    String message = current.getMessage();
    if (message == null || message.isBlank()) {
      return type;
    }
    String compact = message.replaceAll("\\s+", " ").trim();
    if (compact.length() > 80) {
      compact = compact.substring(0, 77) + "...";
    }
    return type + " - " + compact;
  }
}
