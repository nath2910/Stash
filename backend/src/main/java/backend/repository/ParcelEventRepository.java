package backend.repository;

import backend.entity.ParcelEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelEventRepository extends JpaRepository<ParcelEvent, Long> {

  boolean existsByParcel_IdAndEventHash(Long parcelId, String eventHash);

  List<ParcelEvent> findByParcel_IdOrderByEventTimeDesc(Long parcelId);
}
