package backend.repository;

import backend.entity.Attachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
  List<Attachment> findByVente_IdAndUser_Id(Integer venteId, Long userId);
  Optional<Attachment> findByIdAndVente_IdAndUser_Id(Long id, Integer venteId, Long userId);
}
