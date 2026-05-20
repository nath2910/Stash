package backend.repository;

import backend.entity.SeenMailMessage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeenMailMessageRepository extends JpaRepository<SeenMailMessage, Long> {

  boolean existsByMailAccount_IdAndProviderMessageId(Long mailAccountId, String providerMessageId);

  Optional<SeenMailMessage> findByMailAccount_IdAndProviderMessageId(Long mailAccountId, String providerMessageId);
}
