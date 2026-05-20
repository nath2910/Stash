package backend.service;

import backend.dto.TrackingWebhookResponse;
import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.repository.ParcelRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class DeliveryTrackingAggregatorService implements TrackingAggregatorService {

  private final DirectCarrierTrackingService directCarrierTrackingService;
  private final AfterShipTrackingClient afterShipTrackingClient;
  private final ParcelRepository parcelRepository;
  private final String trackingProvider;

  public DeliveryTrackingAggregatorService(
      DirectCarrierTrackingService directCarrierTrackingService,
      AfterShipTrackingClient afterShipTrackingClient,
      ParcelRepository parcelRepository,
      @Value("${app.delivery.tracking-provider:DIRECT}") String trackingProvider
  ) {
    this.directCarrierTrackingService = directCarrierTrackingService;
    this.afterShipTrackingClient = afterShipTrackingClient;
    this.parcelRepository = parcelRepository;
    this.trackingProvider = trackingProvider == null ? "DIRECT" : trackingProvider.trim().toUpperCase(Locale.ROOT);
  }

  @Override
  @Transactional
  public void registerTracking(Parcel parcel) {
    refreshTracking(parcel);
  }

  @Override
  @Transactional
  public Parcel refreshTracking(Parcel parcel) {
    if (parcel == null || parcel.getId() == null) {
      return parcel;
    }

    boolean success = true;
    try {
      Parcel refreshed = switch (trackingProvider) {
        case "AFTERSHIP" -> afterShipTrackingClient.refreshTracking(parcel);
        case "AUTO", "DIRECT_THEN_AFTERSHIP" -> refreshAuto(parcel);
        default -> directCarrierTrackingService.refreshTracking(parcel);
      };
      return scheduleNextRefresh(refreshed, true);
    } catch (Exception ex) {
      success = false;
      throw ex;
    } finally {
      if (!success) {
        scheduleNextRefresh(parcel, false);
      }
    }
  }

  @Override
  public TrackingWebhookResponse handleWebhook(String payload, String signatureHeader, String sharedSecretHeader) {
    return afterShipTrackingClient.handleWebhook(payload, signatureHeader, sharedSecretHeader);
  }

  private Parcel refreshAuto(Parcel parcel) {
    if (directCarrierTrackingService.supports(parcel)) {
      return directCarrierTrackingService.refreshTracking(parcel);
    }
    if (afterShipTrackingClient.isConfigured()) {
      return afterShipTrackingClient.refreshTracking(parcel);
    }
    return directCarrierTrackingService.refreshTracking(parcel);
  }

  private Parcel scheduleNextRefresh(Parcel parcel, boolean success) {
    if (parcel == null || parcel.getId() == null) {
      return parcel;
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    parcel.setLastTrackingRefreshAt(now);
    if (parcel.getStatus() == ParcelStatus.DELIVERED) {
      parcel.setNextTrackingRefreshAt(null);
    } else {
      long baseMinutes = success ? nextSuccessDelayMinutes(parcel.getStatus()) : 60;
      int jitterSeconds = ThreadLocalRandom.current().nextInt(0, 600);
      parcel.setNextTrackingRefreshAt(now.plusMinutes(baseMinutes).plusSeconds(jitterSeconds));
    }
    return parcelRepository.save(parcel);
  }

  private long nextSuccessDelayMinutes(ParcelStatus status) {
    if (status == null) {
      return 180;
    }
    return switch (status) {
      case OUT_FOR_DELIVERY -> 60;
      case IN_TRANSIT -> 180;
      case PENDING, REGISTERED, UNKNOWN -> 360;
      case EXCEPTION -> 240;
      case DELIVERED -> 0;
    };
  }
}
