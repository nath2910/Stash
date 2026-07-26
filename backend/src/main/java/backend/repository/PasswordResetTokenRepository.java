package backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import backend.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  @EntityGraph(attributePaths = "user")
  Optional<PasswordResetToken> findByToken(String token);

  void deleteByUser_Id(Long userId);
}
