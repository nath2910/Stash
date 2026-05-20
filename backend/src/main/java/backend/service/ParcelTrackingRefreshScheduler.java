package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.repository.ParcelRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ParcelTrackingRefreshScheduler {

  private static final Logger log = LoggerFactory.getLogger(ParcelTrackingRefreshScheduler.class);
  private static final List<ParcelStatus> ACTIVE_STATUSES = List.of(
      ParcelStatus.PENDING,
      ParcelStatus.REGISTERED,
      ParcelStatus.IN_TRANSIT,
      ParcelStatus.OUT_FOR_DELIVERY,
      ParcelStatus.EXCEPTION,
      ParcelStatus.UNKNOWN
  );

  private final ParcelRepository parcelRepository;
  private final TrackingAggregatorService trackingAggregatorService;
  private final int batchSize;

  public ParcelTrackingRefreshScheduler(
      ParcelRepository parcelRepository,
      TrackingAggregatorService trackingAggregatorService,
      @Value("${app.delivery.tracking-refresh-batch-size:50}") int batchSize
  ) {
    this.parcelRepository = parcelRepository;
    this.trackingAggregatorService = trackingAggregatorService;
    this.batchSize = batchSize;
  }

  @Scheduled(fixedDelayString = "${app.delivery.tracking-refresh-fixed-delay-ms:900000}")
  public void refreshDueParcels() {
    List<Parcel> parcels = parcelRepository.findDueForTrackingRefresh(
        ACTIVE_STATUSES,
        OffsetDateTime.now(ZoneOffset.UTC),
        PageRequest.of(0, Math.max(1, batchSize))
    );

    for (Parcel parcel : parcels) {
      try {
        trackingAggregatorService.refreshTracking(parcel);
      } catch (Exception ex) {
        log.warn("Delivery tracking refresh skipped parcel {} after error", parcel.getId());
      }
    }
  }
}
