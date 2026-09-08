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
    // Issued documents are isolated from application APIs; retain only for the accounting purpose.
    jdbc.update("""
        INSERT INTO accounting_archives(retain_until, document)
        SELECT accounting_retention_end(COALESCE(document_date, created_at::date), ?, ?),
               to_jsonb(i) - 'user_id'
        FROM admin_invoices i WHERE user_id = ? AND status <> 'draft'
        """, user.getFiscalYearEndMonth(), user.getFiscalYearEndDay(), userId);
    jdbc.update("""
        INSERT INTO accounting_archives(retain_until, document)
        SELECT accounting_retention_end(date_vente, ?, ?),
          jsonb_build_object('kind', 'sale', 'date_vente', date_vente, 'nom_item', nom_item,
            'prix_retail', prix_retail, 'prix_resell', prix_resell, 'date_achat', date_achat)
        FROM tableauventes WHERE user_id = ? AND date_vente IS NOT NULL
        """, user.getFiscalYearEndMonth(), user.getFiscalYearEndDay(), userId);
    jdbc.update("INSERT INTO storage_cleanup_queue(user_id) VALUES (?) ON CONFLICT DO NOTHING", userId);
    // Foreign-key cascades cover mail tokens, parcels, notifications, layouts, stock and drafts.
    jdbc.update("DELETE FROM users WHERE id = ?", userId);
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCommit() { storage.deleteUserFiles(userId); }
    });
  }
}
