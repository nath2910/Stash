package backend.repository;

import backend.entity.AdminState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminStateRepository extends JpaRepository<AdminState, Long> {
  Optional<AdminState> findByUser_Id(Long userId);
}
