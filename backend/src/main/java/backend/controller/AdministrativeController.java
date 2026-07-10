package backend.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import backend.dto.AdministrativeDeclarationRecordRequest;
import backend.dto.AdministrativeDocumentDescriptorResponse;
import backend.dto.AdministrativeDocumentRecordResponse;
import backend.dto.AdministrativeDocumentRequest;
import backend.dto.AdministrativeInvoiceBatchResponse;
import backend.dto.AdministrativeProfileRequest;
import backend.dto.AdministrativeProfileResponse;
import backend.dto.AdministrativeSummaryResponse;
import backend.entity.User;
import backend.service.AdministrativeService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "administrative")
public class AdministrativeController {

  private final AdministrativeService administrativeService;

  public AdministrativeController(AdministrativeService administrativeService) {
    this.administrativeService = administrativeService;
  }

  @GetMapping(path = "profile", produces = APPLICATION_JSON_VALUE)
  public AdministrativeProfileResponse profile(@AuthenticationPrincipal User currentUser) {
    return administrativeService.getProfile(currentUser.getId());
  }

  @PutMapping(path = "profile", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public AdministrativeProfileResponse saveProfile(
      @AuthenticationPrincipal User currentUser,
      @RequestBody AdministrativeProfileRequest request
  ) {
    return administrativeService.updateProfile(currentUser.getId(), request);
  }

  @GetMapping(path = "summary", produces = APPLICATION_JSON_VALUE)
  public AdministrativeSummaryResponse summary(
      @AuthenticationPrincipal User currentUser,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
  ) {
    return administrativeService.summary(currentUser.getId(), periodStart, periodEnd);
  }

  @GetMapping(path = "year-summary", produces = APPLICATION_JSON_VALUE)
  public AdministrativeSummaryResponse yearSummary(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) Integer year
  ) {
    int selectedYear = year == null ? LocalDate.now().getYear() : year;
    return administrativeService.yearSummary(currentUser.getId(), selectedYear);
  }

  @GetMapping(path = "documents", produces = APPLICATION_JSON_VALUE)
  public List<AdministrativeDocumentDescriptorResponse> documents(@AuthenticationPrincipal User currentUser) {
    return administrativeService.documents(currentUser.getId());
  }

  @GetMapping(path = "document-records", produces = APPLICATION_JSON_VALUE)
  public List<AdministrativeDocumentRecordResponse> documentRecords(@AuthenticationPrincipal User currentUser) {
    return administrativeService.documentRecords(currentUser.getId());
  }

  @PostMapping(path = "declarations/mark-done", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public AdministrativeDocumentRecordResponse markDeclarationDone(
      @AuthenticationPrincipal User currentUser,
      @RequestBody AdministrativeDeclarationRecordRequest request
  ) {
    return administrativeService.markDeclarationDone(currentUser.getId(), request);
  }

  @PostMapping(path = "documents/urssaf-summary", consumes = APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> urssafSummary(
      @AuthenticationPrincipal User currentUser,
      @RequestBody(required = false) AdministrativeDocumentRequest request
  ) {
    return pdf(administrativeService.generateDocument(currentUser.getId(), request, "urssaf-summary"));
  }

  @PostMapping(path = "documents/fiscal-summary", consumes = APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> fiscalSummary(
      @AuthenticationPrincipal User currentUser,
      @RequestBody(required = false) AdministrativeDocumentRequest request
  ) {
    return pdf(administrativeService.generateDocument(currentUser.getId(), request, "fiscal-summary"));
  }

  @PostMapping(path = "documents/receipts-register", consumes = APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> receiptsRegister(
      @AuthenticationPrincipal User currentUser,
      @RequestBody(required = false) AdministrativeDocumentRequest request
  ) {
    return pdf(administrativeService.generateDocument(currentUser.getId(), request, "receipts-register"));
  }

  @PostMapping(path = "documents/purchases-register", consumes = APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> purchasesRegister(
      @AuthenticationPrincipal User currentUser,
      @RequestBody(required = false) AdministrativeDocumentRequest request
  ) {
    return pdf(administrativeService.generateDocument(currentUser.getId(), request, "purchases-register"));
  }

  @PostMapping(path = "documents/second-hand-register", consumes = APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> secondHandRegister(
      @AuthenticationPrincipal User currentUser,
      @RequestBody(required = false) AdministrativeDocumentRequest request
  ) {
    return pdf(administrativeService.generateDocument(currentUser.getId(), request, "second-hand-register"));
  }

  @PostMapping(path = "documents/annual-folder", consumes = APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> annualFolder(
      @AuthenticationPrincipal User currentUser,
      @RequestBody(required = false) AdministrativeDocumentRequest request
  ) {
    return pdf(administrativeService.generateDocument(currentUser.getId(), request, "annual-folder"));
  }

  @PostMapping(path = "invoices/generate-missing", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public AdministrativeInvoiceBatchResponse generateMissingInvoices(
      @AuthenticationPrincipal User currentUser,
      @RequestBody(required = false) AdministrativeDocumentRequest request
  ) {
    return administrativeService.generateMissingInvoices(currentUser.getId(), request);
  }

  private ResponseEntity<byte[]> pdf(AdministrativeService.GeneratedAdministrativeDocument document) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(document.filename()).build().toString()
        )
        .contentLength(document.content().length)
        .body(document.content());
  }
}
