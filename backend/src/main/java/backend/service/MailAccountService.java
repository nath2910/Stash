package backend.service;

import backend.dto.MailAccountResponse;
import backend.dto.MailScanResponse;
import backend.dto.TrackingConnectResponse;
import backend.entity.MailAccount;
import backend.entity.User;
import backend.repository.MailAccountRepository;
import backend.service.GmailMailScannerService.MailScanSummary;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MailAccountService {

  private final MailAccountRepository mailAccountRepository;
  private final GmailOAuthService gmailOAuthService;
  private final GmailMailScannerService gmailMailScannerService;

  public MailAccountService(
      MailAccountRepository mailAccountRepository,
      GmailOAuthService gmailOAuthService,
      GmailMailScannerService gmailMailScannerService
  ) {
    this.mailAccountRepository = mailAccountRepository;
    this.gmailOAuthService = gmailOAuthService;
    this.gmailMailScannerService = gmailMailScannerService;
  }

  @Transactional(readOnly = true)
  public List<MailAccountResponse> listForUser(Long userId) {
    return mailAccountRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
        .map(MailAccountResponse::fromEntity)
        .toList();
  }

  public TrackingConnectResponse connectGmail(User user, String emailHint) {
    return gmailOAuthService.buildConnectResponse(user, emailHint);
  }

  public MailAccount completeGmailCallback(String code, String state) {
    return gmailOAuthService.handleCallback(code, state);
  }

  public URI gmailCallbackRedirect(boolean success) {
    return gmailOAuthService.buildFrontendRedirect(success);
  }

  @Transactional
  public void deleteForUser(Long userId, Long accountId) {
    MailAccount account = mailAccountRepository.findByIdAndUser_Id(accountId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte mail introuvable"));
    mailAccountRepository.delete(account);
  }

  public MailScanResponse scanNow(Long userId, Long accountId) {
    MailAccount account = mailAccountRepository.findByIdAndUser_Id(accountId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte mail introuvable"));
    MailScanSummary summary = gmailMailScannerService.scanAccountDetailed(account.getId());
    MailAccount updated = mailAccountRepository.findByIdAndUser_Id(accountId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte mail introuvable"));
    return new MailScanResponse(
        MailAccountResponse.fromEntity(updated),
        summary.scannedMessages(),
        summary.deliveryMessages(),
        summary.importedCount(),
        summary.reviewCount(),
        summary.rejectedCount(),
        summary.duplicateCount(),
        scanMessage(summary),
        summary.candidatesToReview()
    );
  }

  private String scanMessage(MailScanSummary summary) {
    if (summary.scannedMessages() == 0) {
      return "Aucun nouvel email de livraison a analyser.";
    }
    if (summary.importedCount() > 0 || summary.reviewCount() > 0) {
      return summary.importedCount() + " suivi(s) fiable(s) importe(s), "
          + summary.reviewCount() + " candidat(s) a verifier.";
    }
    if (summary.deliveryMessages() == 0) {
      return "Aucun email de livraison fiable n'a ete trouve.";
    }
    return "Aucun numero de suivi fiable n'a ete trouve. Certains numeros ressemblaient plutot a des commandes, factures ou references internes.";
  }
}
