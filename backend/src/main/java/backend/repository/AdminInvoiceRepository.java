package backend.repository;

import backend.entity.AdminInvoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminInvoiceRepository extends JpaRepository<AdminInvoice, Long> {
  List<AdminInvoice> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  Optional<AdminInvoice> findByIdAndUser_Id(Long id, Long userId);
}
