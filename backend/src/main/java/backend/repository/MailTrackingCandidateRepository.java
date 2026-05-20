package backend.repository;

import backend.entity.MailTrackingCandidate;
import backend.entity.TrackingCandidateStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailTrackingCandidateRepository extends JpaRepository<MailTrackingCandidate, Long> {

  List<MailTrackingCandidate> findByUser_IdAndStatusOrderByReceivedAtDescCreatedAtDesc(
      Long userId,
      TrackingCandidateStatus status
  );

  List<MailTrackingCandidate> findByUser_IdAndStatusInOrderByReceivedAtDescCreatedAtDesc(
      Long userId,
      Collection<TrackingCandidateStatus> statuses
  );

  Optional<MailTrackingCandidate> findByIdAndUser_Id(Long id, Long userId);

  Optional<MailTrackingCandidate> findByUser_IdAndDedupeKey(Long userId, String dedupeKey);
}
