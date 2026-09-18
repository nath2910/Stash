package backend.service;

import backend.entity.User;
import backend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AccountDeletionServiceTest {

  @AfterEach
  void cleanupSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void deletionDoesNotArchiveUserInventoryOrDocuments() throws Exception {
    UserRepository users = Mockito.mock(UserRepository.class);
    BillingService billing = Mockito.mock(BillingService.class);
    JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
    FileStorageService storage = Mockito.mock(FileStorageService.class);
    User user = new User();
    Mockito.when(users.lockById(42L)).thenReturn(Optional.of(user));
    Mockito.when(storage.deleteUserFiles(42L)).thenReturn(true);
    TransactionSynchronizationManager.initSynchronization();

    new AccountDeletionService(users, billing, jdbc, storage).delete(42L);

    Mockito.verify(billing).deleteCustomerForAccountDeletion(user);
    Mockito.verify(jdbc).update(
        "INSERT INTO storage_cleanup_queue(user_id) VALUES (?) ON CONFLICT DO NOTHING",
        42L
    );
    Mockito.verify(jdbc).update("DELETE FROM users WHERE id = ?", 42L);
    Mockito.verify(jdbc, Mockito.never()).update(
        ArgumentMatchers.contains("accounting_archives"),
        ArgumentMatchers.<Object[]>any()
    );

    Assertions.assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
    TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
    Mockito.verify(storage).deleteUserFiles(42L);
    Mockito.verify(jdbc).update("DELETE FROM storage_cleanup_queue WHERE user_id = ?", 42L);
  }

  @Test
  void failedFileCleanupRemainsQueuedForRetry() throws Exception {
    UserRepository users = Mockito.mock(UserRepository.class);
    BillingService billing = Mockito.mock(BillingService.class);
    JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
    FileStorageService storage = Mockito.mock(FileStorageService.class);
    User user = new User();
    Mockito.when(users.lockById(42L)).thenReturn(Optional.of(user));
    Mockito.when(storage.deleteUserFiles(42L)).thenReturn(false);
    TransactionSynchronizationManager.initSynchronization();

    new AccountDeletionService(users, billing, jdbc, storage).delete(42L);
    TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();

    Mockito.verify(jdbc, Mockito.never()).update(
        "DELETE FROM storage_cleanup_queue WHERE user_id = ?",
        42L
    );
  }

  @Test
  void stripeDeletionFailureKeepsAllLocalData() throws Exception {
    UserRepository users = Mockito.mock(UserRepository.class);
    BillingService billing = Mockito.mock(BillingService.class);
    JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
    FileStorageService storage = Mockito.mock(FileStorageService.class);
    User user = new User();
    Mockito.when(users.lockById(42L)).thenReturn(Optional.of(user));
    Mockito.doThrow(new IllegalStateException("Stripe unavailable"))
        .when(billing).deleteCustomerForAccountDeletion(user);

    Assertions.assertThrows(IllegalStateException.class,
        () -> new AccountDeletionService(users, billing, jdbc, storage).delete(42L));

    Mockito.verify(jdbc, Mockito.never()).update(Mockito.anyString(), Mockito.<Object[]>any());
    Mockito.verifyNoInteractions(storage);
  }

  @Test
  void queuedFileCleanupIsRetriedUntilItSucceeds() {
    UserRepository users = Mockito.mock(UserRepository.class);
    BillingService billing = Mockito.mock(BillingService.class);
    JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
    FileStorageService storage = Mockito.mock(FileStorageService.class);
    Mockito.when(jdbc.query(
        Mockito.eq("SELECT user_id FROM storage_cleanup_queue ORDER BY requested_at ASC LIMIT 100"),
        Mockito.any(org.springframework.jdbc.core.RowMapper.class)
    )).thenReturn(List.of(42L));
    Mockito.when(storage.deleteUserFiles(42L)).thenReturn(true);

    new AccountDeletionService(users, billing, jdbc, storage).retryStorageCleanup();

    Mockito.verify(storage).deleteUserFiles(42L);
    Mockito.verify(jdbc).update("DELETE FROM storage_cleanup_queue WHERE user_id = ?", 42L);
  }
}
