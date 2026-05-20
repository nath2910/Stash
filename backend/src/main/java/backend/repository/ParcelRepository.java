package backend.repository;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

  List<Parcel> findByUser_IdOrderByUpdatedAtDesc(Long userId);

  Optional<Parcel> findByIdAndUser_Id(Long id, Long userId);

  Optional<Parcel> findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
      Long userId,
      String normalizedTrackingNumber,
      String carrierSlug
  );

  Optional<Parcel> findByAggregatorAndAggregatorTrackingId(String aggregator, String aggregatorTrackingId);

  Optional<Parcel> findFirstByNormalizedTrackingNumberOrderByUpdatedAtDesc(String normalizedTrackingNumber);

  @Query("""
      select p
      from Parcel p
      where p.status in :statuses
        and (p.nextTrackingRefreshAt is null or p.nextTrackingRefreshAt <= :now)
      order by p.nextTrackingRefreshAt asc, p.id asc
      """)
  List<Parcel> findDueForTrackingRefresh(
      @Param("statuses") List<ParcelStatus> statuses,
      @Param("now") OffsetDateTime now,
      Pageable pageable
  );
}
