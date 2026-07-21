package backend.service;

import backend.dto.MailAccountResponse;
import backend.dto.MailScanBatchResponse;
import backend.dto.MailScanParcelResponse;
import backend.dto.MailScanResponse;
import backend.dto.TrackingConnectResponse;
import backend.entity.MailAccount;
import backend.entity.MailAccountStatus;
import backend.entity.User;
import backend.repository.MailAccountRepository;
import backend.service.GmailMailScannerService.MailScanSummary;
import java.net.URI;
import java.util.ArrayList;
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
    return mailAccountRepository.findTopByUser_IdOrderByCreatedAtDesc(userId)
        .map(MailAccountResponse::fromEntity)
        .map(List::of)
        .orElseGet(List::of);
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
    return buildScanReport(account, userId);
  }

  public MailScanBatchResponse scanAll(Long userId) {
    List<MailAccount> accounts = mailAccountRepository.findTopByUser_IdOrderByCreatedAtDesc(userId)
        .map(List::of)
        .orElseGet(List::of);
    if (accounts.isEmpty()) {
      return new MailScanBatchResponse(
          0,
          0,
          0,
          0,
          0,
          0,
          0,
          "Connectez un compte Gmail pour scanner vos emails Colissimo.",
          List.of(),
          List.of(),
          List.of(),
          List.of()
      );
    }

    int scannedAccounts = 0;
    int scannedMessages = 0;
    int deliveryMessages = 0;
    int importedCount = 0;
    int reviewCount = 0;
    int rejectedCount = 0;
    int duplicateCount = 0;
    List<MailScanResponse> accountReports = new ArrayList<>();
    List<backend.dto.TrackingCandidateResponse> candidatesToReview = new ArrayList<>();
    List<MailScanParcelResponse> importedParcels = new ArrayList<>();
    List<MailScanParcelResponse> duplicateParcels = new ArrayList<>();

    for (MailAccount account : accounts) {
      MailScanResponse report = buildScanReport(account, userId);
      accountReports.add(report);
      if (report.success()) {
        scannedAccounts += 1;
      }
      scannedMessages += report.scannedMessages();
      deliveryMessages += report.deliveryMessages();
      importedCount += report.importedCount();
      reviewCount += report.reviewCount();
      rejectedCount += report.rejectedCount();
      duplicateCount += report.duplicateCount();
      mergeCandidates(candidatesToReview, report.candidatesToReview());
      mergeParcels(importedParcels, report.importedParcels());
      mergeParcels(duplicateParcels, report.duplicateParcels());
    }

    MailScanSummary totalSummary = new MailScanSummary(
        scannedMessages,
        deliveryMessages,
        importedCount,
        reviewCount,
        rejectedCount,
        duplicateCount,
        List.copyOf(candidatesToReview),
        List.copyOf(importedParcels),
        List.copyOf(duplicateParcels)
    );
    int failedAccounts = Math.max(0, accountReports.size() - scannedAccounts);
    String summaryMessage = scanMessage(totalSummary);
    if (scannedAccounts == 0 && failedAccounts > 0) {
      summaryMessage = "Aucun compte Gmail n'a pu etre scanne. Reliez ou rescanner les comptes en erreur.";
    } else if (failedAccounts > 0) {
      summaryMessage = summaryMessage + " " + failedAccounts + " compte(s) en erreur.";
    }
    return new MailScanBatchResponse(
        scannedAccounts,
        scannedMessages,
        deliveryMessages,
        importedCount,
        reviewCount,
        rejectedCount,
        duplicateCount,
        summaryMessage,
        List.copyOf(accountReports),
        List.copyOf(candidatesToReview),
        List.copyOf(importedParcels),
        List.copyOf(duplicateParcels)
    );
  }

  private MailScanResponse buildScanReport(MailAccount account, Long userId) {
    MailAccountResponse refreshedAccount = MailAccountResponse.fromEntity(account);
    try {
      MailScanSummary summary = gmailMailScannerService.scanAccountDetailed(account.getId());
      MailAccount updated = mailAccountRepository.findByIdAndUser_Id(account.getId(), userId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte mail introuvable"));
      refreshedAccount = MailAccountResponse.fromEntity(updated);
      return new MailScanResponse(
          refreshedAccount,
          summary.scannedMessages(),
          summary.deliveryMessages(),
          summary.importedCount(),
          summary.reviewCount(),
          summary.rejectedCount(),
          summary.duplicateCount(),
          scanMessage(summary),
          summary.candidatesToReview(),
          summary.importedParcels(),
          summary.duplicateParcels(),
          true,
          null
      );
    } catch (Exception ex) {
      MailAccount latest = mailAccountRepository.findByIdAndUser_Id(account.getId(), userId).orElse(account);
      refreshedAccount = MailAccountResponse.fromEntity(latest);
      return new MailScanResponse(
          refreshedAccount,
          0,
          0,
          0,
          0,
          0,
          0,
          scanFailureMessage(latest),
          List.of(),
          List.of(),
          List.of(),
          false,
          ex.getMessage()
      );
    }
  }

  private String scanMessage(MailScanSummary summary) {
    if (summary.scannedMessages() == 0) {
      return "Aucun nouvel email Colissimo a analyser.";
    }
    if (summary.importedCount() > 0 || summary.reviewCount() > 0) {
      StringBuilder message = new StringBuilder();
      message.append(summary.importedCount()).append(" suivi(s) Colissimo importe(s)");
      if (summary.duplicateCount() > 0) {
        message.append(", ").append(summary.duplicateCount()).append(" deja present(s)");
      }
      if (summary.reviewCount() > 0) {
        message.append(", ").append(summary.reviewCount()).append(" a verifier");
      }
      message.append(".");
      return message.toString();
    }
    if (summary.deliveryMessages() == 0) {
      return "Aucun email Colissimo pertinent n'a ete trouve.";
    }
    return "Aucun numero de suivi Colissimo fiable n'a ete trouve.";
  }

  private String scanFailureMessage(MailAccount account) {
    if (account != null && account.getStatus() == MailAccountStatus.REVOKED) {
      return "Acces Gmail revoque. Reliez le compte pour relancer le scan.";
    }
    return "Scan Gmail indisponible sur ce compte pour le moment.";
  }

  private void mergeParcels(List<MailScanParcelResponse> target, List<MailScanParcelResponse> source) {
    if (source == null) {
      return;
    }
    for (MailScanParcelResponse parcel : source) {
      if (parcel == null || parcel.id() == null) {
        continue;
      }
      boolean alreadyPresent = target.stream().anyMatch(item -> parcel.id().equals(item.id()));
      if (!alreadyPresent) {
        target.add(parcel);
      }
    }
  }

  private void mergeCandidates(
      List<backend.dto.TrackingCandidateResponse> target,
      List<backend.dto.TrackingCandidateResponse> source
  ) {
    if (source == null) {
      return;
    }
    for (backend.dto.TrackingCandidateResponse candidate : source) {
      if (candidate == null || candidate.id() == null) {
        continue;
      }
      boolean alreadyPresent = target.stream().anyMatch(item -> candidate.id().equals(item.id()));
      if (!alreadyPresent) {
        target.add(candidate);
      }
    }
  }
}
