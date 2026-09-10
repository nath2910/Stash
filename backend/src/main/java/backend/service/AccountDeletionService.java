package backend.service;

import backend.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountDeletionService {
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
    // If Stripe is unavailable, keep the account so an invisible recurring charge cannot remain.
    billing.cancelForAccountDeletion(user);
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
}
