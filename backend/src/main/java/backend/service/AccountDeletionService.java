package backend.service;

import backend.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountDeletionService {
  private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);
  private final UserRepository users;
  private final BillingService billing;
  private final JdbcTemplate jdbc;
  private final FileStorageService storage;

  public AccountDeletionService(UserRepository users, BillingService billing, JdbcTemplate jdbc, FileStorageService storage) {
    this.users = users;
    this.billing = billing;
    this.jdbc = jdbc;
    this.storage = storage;
  }

  @Transactional(rollbackFor = Exception.class)
  public void delete(Long userId) throws Exception {
    var user = users.lockById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    // Do not remove local data until Stripe confirms both the subscription cancellation and the
    // removal of saved payment details. A provider failure rolls this transaction back.
    billing.deleteCustomerForAccountDeletion(user);
    jdbc.update("INSERT INTO storage_cleanup_queue(user_id) VALUES (?) ON CONFLICT DO NOTHING", userId);
    // Foreign-key cascades cover documents, mail tokens, parcels, notifications, layouts and stock.
    jdbc.update("DELETE FROM users WHERE id = ?", userId);
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        if (storage.deleteUserFiles(userId)) {
          jdbc.update("DELETE FROM storage_cleanup_queue WHERE user_id = ?", userId);
        }
      }
    });
  }

  /**
   * Physical storage cannot participate in the database transaction. Retry queued removals until
   * the user directory is gone, so a transient storage failure never becomes retained data.
   */
  @Scheduled(fixedDelayString = "${app.account-deletion.storage-cleanup-delay-ms:300000}")
  @Transactional(rollbackFor = Exception.class)
  public void retryStorageCleanup() {
    List<Long> userIds = jdbc.query(
        "SELECT user_id FROM storage_cleanup_queue ORDER BY requested_at ASC LIMIT 100",
        (resultSet, rowNumber) -> resultSet.getLong("user_id")
    );
    for (Long userId : userIds) {
      try {
        if (storage.deleteUserFiles(userId)) {
          jdbc.update("DELETE FROM storage_cleanup_queue WHERE user_id = ?", userId);
        }
      } catch (RuntimeException ex) {
        // Continue with other accounts; this id remains queued for the next pass.
        log.error("Account file cleanup retry failed for user {}", userId, ex);
      }
    }
  }
}
