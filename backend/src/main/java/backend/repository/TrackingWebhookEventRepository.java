package backend.repository;

import backend.entity.TrackingWebhookEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackingWebhookEventRepository extends JpaRepository<TrackingWebhookEvent, Long> {

  Optional<TrackingWebhookEvent> findByAggregatorAndExternalEventId(String aggregator, String externalEventId);
}
