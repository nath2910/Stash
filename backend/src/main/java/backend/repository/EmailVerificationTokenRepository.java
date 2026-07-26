package backend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import backend.entity.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
  @EntityGraph(attributePaths = "user")
  Optional<EmailVerificationToken> findByToken(String token);

  @Transactional
  void deleteByUserIdAndUsedAtIsNull(Long userId);

  @Transactional
  void deleteByUserId(Long userId);

  @Transactional
  void deleteByExpiresAtBefore(Instant cutoff);

  @Transactional
  void deleteByUsedAtIsNotNull();
}
