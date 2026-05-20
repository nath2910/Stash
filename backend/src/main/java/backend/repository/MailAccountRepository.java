package backend.repository;

import backend.entity.MailAccount;
import backend.entity.MailAccountStatus;
import backend.entity.MailProvider;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

  List<MailAccount> findByUser_IdOrderByCreatedAtDesc(Long userId);

  Optional<MailAccount> findByIdAndUser_Id(Long id, Long userId);

  @Query("""
      select m
      from MailAccount m
      join fetch m.user
      where m.id = :id
      """)
  Optional<MailAccount> findByIdWithUser(@Param("id") Long id);

  Optional<MailAccount> findByProviderAndProviderAccountId(MailProvider provider, String providerAccountId);

  Optional<MailAccount> findByUser_IdAndProviderAndEmailAddress(Long userId, MailProvider provider, String emailAddress);

  @Query("""
      select m
      from MailAccount m
      where m.status = :status
        and m.nextScanAt <= :now
      order by m.nextScanAt asc, m.id asc
      """)
  List<MailAccount> findDueForScan(
      @Param("status") MailAccountStatus status,
      @Param("now") OffsetDateTime now,
      Pageable pageable
  );
}
