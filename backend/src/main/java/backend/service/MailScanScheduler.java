package backend.service;

import backend.entity.MailAccount;
import backend.entity.MailAccountStatus;
import backend.repository.MailAccountRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MailScanScheduler {

  private static final Logger log = LoggerFactory.getLogger(MailScanScheduler.class);

  private final MailAccountRepository mailAccountRepository;
  private final GmailMailScannerService gmailMailScannerService;
  private final int batchSize;

  public MailScanScheduler(
      MailAccountRepository mailAccountRepository,
      GmailMailScannerService gmailMailScannerService,
      @Value("${app.delivery.scan-batch-size:25}") int batchSize
  ) {
    this.mailAccountRepository = mailAccountRepository;
    this.gmailMailScannerService = gmailMailScannerService;
    this.batchSize = batchSize;
  }

  @Scheduled(fixedDelayString = "${app.delivery.scan-fixed-delay-ms:300000}")
  public void scanDueMailAccounts() {
    List<MailAccount> dueAccounts = mailAccountRepository.findPrimaryDueForScan(
        MailAccountStatus.ACTIVE,
        OffsetDateTime.now(ZoneOffset.UTC),
        PageRequest.of(0, Math.max(1, batchSize))
    );

    for (MailAccount account : dueAccounts) {
      try {
        int detected = gmailMailScannerService.scanAccount(account.getId());
        if (detected > 0) {
          log.info("Delivery mail scan detected {} tracking candidate(s) for account {}", detected, account.getId());
        }
      } catch (Exception ex) {
        log.warn("Delivery mail scan skipped account {} after error", account.getId());
      }
    }
  }
}
