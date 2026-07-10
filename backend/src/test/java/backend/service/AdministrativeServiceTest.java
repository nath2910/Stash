package backend.service;

import backend.dto.AdministrativeDeclarationRecordRequest;
import backend.dto.AdministrativeDocumentRequest;
import backend.dto.AdministrativeDocumentDescriptorResponse;
import backend.dto.AdministrativeDocumentRecordResponse;
import backend.dto.AdministrativeInvoiceBatchResponse;
import backend.dto.AdministrativeProfileRequest;
import backend.dto.AdministrativeProfileResponse;
import backend.dto.AdministrativeSummaryResponse;
import backend.entity.AdminInvoice;
import backend.entity.DeclarationFrequency;
import backend.entity.LegalProfileType;
import backend.entity.AdminState;
import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.AdminInvoiceRepository;
import backend.repository.AdminStateRepository;
import backend.repository.SnkVenteRepository;
import backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AdministrativeServiceTest {

  private UserRepository userRepository;
  private SnkVenteRepository snkVenteRepository;
  private AdminStateRepository adminStateRepository;
  private AdminInvoiceRepository adminInvoiceRepository;
  private AdministrativeService service;
  private User user;

  @BeforeEach
  void setup() {
    userRepository = Mockito.mock(UserRepository.class);
    snkVenteRepository = Mockito.mock(SnkVenteRepository.class);
    adminStateRepository = Mockito.mock(AdminStateRepository.class);
    adminInvoiceRepository = Mockito.mock(AdminInvoiceRepository.class);
    service = new AdministrativeService(
        userRepository,
        snkVenteRepository,
        new AdministrativePdfService(),
        adminStateRepository,
        adminInvoiceRepository
    );

    user = new User();
    user.setEmail("test@example.com");
    user.setFirstName("Ada");
    user.setLastName("Lovelace");
    user.setLegalProfileType(LegalProfileType.MICRO_ENTREPRISE);
    user.setSiret("12345678901234");
    user.setSiren("123456789");
    user.setAdministrativeDisplayName("Ada Lovelace");
    user.setAdministrativeAddress("1 rue Test");
    user.setDeclarationFrequency(DeclarationFrequency.MONTHLY);
    user.setLegalProfileCompleted(true);

    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Mockito.when(adminStateRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
    Mockito.when(adminStateRepository.save(Mockito.any(AdminState.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Mockito.when(adminInvoiceRepository.findByUser_IdOrderByCreatedAtDesc(Mockito.eq(1L), Mockito.any()))
        .thenReturn(List.of());
    Mockito.when(adminInvoiceRepository.save(Mockito.any(AdminInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void periodRevenueUsesGrossSaleAmountWithoutDeductingCosts() {
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of(
        sale(1, "A", "2026-01-05", "2026-01-20", "80.00", "100.00"),
        sale(2, "B", "2026-01-08", "2026-01-22", "150.00", "200.00"),
        sale(3, "Outside", "2026-02-01", "2026-02-02", "10.00", "999.00")
    ));

    AdministrativeSummaryResponse result = service.summary(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31)
    );

    Assertions.assertEquals(new BigDecimal("300.00"), result.periodRevenue());
    Assertions.assertEquals(2L, result.periodSaleCount());
    Assertions.assertTrue(result.revenueRule().contains("ne sont pas deduits"));
  }

  @Test
  void annualSummaryUsesOnlyRequestedUserRepositoryQuery() {
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of(
        sale(1, "A", "2026-01-01", "2026-01-02", "10.00", "50.00"),
        sale(2, "B", "2026-12-01", "2026-12-02", "20.00", "70.00"),
        sale(3, "Old", "2025-12-01", "2025-12-02", "20.00", "500.00")
    ));

    AdministrativeSummaryResponse result = service.yearSummary(1L, 2026);

    Assertions.assertEquals(new BigDecimal("120.00"), result.annualRevenue());
    Assertions.assertEquals(2L, result.annualSaleCount());
    Mockito.verify(snkVenteRepository).findByUser_IdOrderByDateAchatDesc(1L);
  }

  @Test
  void updateMicroProfileDerivesSirenAndMarksComplete() {
    AdministrativeProfileResponse result = service.updateProfile(1L, new AdministrativeProfileRequest(
        "MICRO_ENTREPRISE",
        "micro",
        List.of("goods_resale", "ecommerce"),
        "franchise_base",
        List.of("urssaf_quarterly", "cfe"),
        "98765432100011",
        "Ada Shop",
        "Ada Lovelace",
        "Ada Shop",
        "Ada Sneakers",
        "2 rue Commerce",
        "Micro-entreprise",
        null,
        null,
        "QUARTERLY",
        "urssaf_quarterly",
        "NO",
        "YES",
        LocalDate.of(2026, 1, 10),
        12,
        31,
        "Vente de marchandises",
        null,
        "",
        "A_VERIFIER",
        true,
        true
    ));

    Assertions.assertEquals(LegalProfileType.MICRO_ENTREPRISE, result.profileType());
    Assertions.assertEquals("micro", result.legalStatus());
    Assertions.assertEquals(List.of("goods_resale", "ecommerce"), result.activities());
    Assertions.assertEquals("987654321", result.siren());
    Assertions.assertEquals(DeclarationFrequency.QUARTERLY, result.declarationFrequency());
    Assertions.assertTrue(result.completed());
  }

  @Test
  void microDocumentsUsePreparatoryNames() {
    List<AdministrativeDocumentDescriptorResponse> documents = service.documents(1L);

    Assertions.assertEquals(
        List.of(
            "Fiche de saisie URSSAF",
            "Registre des recettes",
            "Registre des achats",
            "Recap annuel fiscal",
            "Dossier annuel complet"
        ),
        documents.stream().map(AdministrativeDocumentDescriptorResponse::title).toList()
    );
    Assertions.assertTrue(documents.stream().noneMatch(document ->
        document.title().toLowerCase().contains("officiel")
    ));
  }

  @Test
  void microAlertsIdentifyUsefulMissingInformation() {
    user.setSiret(null);
    user.setSiren(null);
    user.setDeclarationFrequency(DeclarationFrequency.UNKNOWN);
    user.setLegalProfileCompleted(false);
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of());

    AdministrativeSummaryResponse result = service.summary(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31)
    );

    List<String> alertTitles = result.alerts().stream().map(alert -> alert.title()).toList();
    Assertions.assertTrue(alertTitles.contains("SIRET manquant"));
    Assertions.assertTrue(alertTitles.contains("Periodicite URSSAF inconnue"));
    Assertions.assertTrue(alertTitles.contains("Periode sans vente"));
  }

  @Test
  void legacyMicroProfileMapsToModularAdministrativeProfile() {
    AdministrativeProfileResponse result = service.getProfile(1L);

    Assertions.assertEquals("micro", result.legalStatus());
    Assertions.assertEquals(List.of("goods_resale"), result.activities());
    Assertions.assertTrue(result.declarations().contains("urssaf_monthly"));
    Assertions.assertEquals("Vente de marchandises", result.urssafCategory());
  }

  @Test
  void summaryIncludesMicroObligations() {
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of(
        sale(1, "A", "2026-01-05", "2026-01-20", "80.00", "100.00")
    ));

    AdministrativeSummaryResponse result = service.summary(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31)
    );

    List<String> obligationIds = result.obligations().stream()
        .map(obligation -> obligation.id())
        .toList();
    Assertions.assertTrue(obligationIds.contains("micro-urssaf"));
    Assertions.assertTrue(obligationIds.contains("micro-income-tax-2042"));
    Assertions.assertTrue(obligationIds.contains("micro-cfe"));
    Assertions.assertTrue(obligationIds.contains("micro-invoicing"));
    Assertions.assertTrue(obligationIds.contains("micro-receipts-register"));
    Assertions.assertTrue(obligationIds.contains("micro-purchases-register"));
  }

  @Test
  void companyProfileDoesNotExposeMicroUrssafObligation() {
    user.setLegalProfileType(LegalProfileType.SASU);
    user.setAdministrativeLegalStatus("sasu");
    user.setAdministrativeActivities("goods_resale,ecommerce");
    user.setAdministrativeVatRegime("real_simplified");
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of());

    AdministrativeSummaryResponse result = service.summary(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31)
    );

    List<String> obligationIds = result.obligations().stream()
        .map(obligation -> obligation.id())
        .toList();
    Assertions.assertFalse(obligationIds.contains("micro-urssaf"));
    Assertions.assertTrue(obligationIds.contains("company-result-tax"));
    Assertions.assertTrue(obligationIds.contains("company-accounting-package"));
    Assertions.assertTrue(obligationIds.contains("company-accounts-deposit"));
    Assertions.assertTrue(obligationIds.contains("company-cfe"));
  }

  @Test
  void companyDocumentsExposeAccountingRegistersWithoutUrssaf() {
    user.setLegalProfileType(LegalProfileType.EURL);
    user.setAdministrativeLegalStatus("eurl");
    user.setAdministrativeActivities("goods_resale");

    List<AdministrativeDocumentDescriptorResponse> documents = service.documents(1L);
    List<String> documentIds = documents.stream()
        .map(AdministrativeDocumentDescriptorResponse::id)
        .toList();

    Assertions.assertFalse(documentIds.contains("urssaf-summary"));
    Assertions.assertTrue(documentIds.contains("receipts-register"));
    Assertions.assertTrue(documentIds.contains("purchases-register"));
    Assertions.assertTrue(documentIds.contains("fiscal-summary"));
    Assertions.assertTrue(documents.stream().allMatch(document -> !"MICRO_ENTREPRISE".equals(document.primaryProfile())));
  }

  @Test
  void individualProfileKeepsDiagnosticIndicative() {
    user.setLegalProfileType(LegalProfileType.PARTICULIER);
    user.setAdminBuysForResale(true);
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of(
        sale(1, "A", "2026-01-01", "2026-01-20", "100.00", "150.00"),
        sale(2, "B", "2026-01-02", "2026-02-01", "100.00", "160.00"),
        sale(3, "C", "2026-01-03", "2026-02-15", "100.00", "170.00")
    ));

    AdministrativeSummaryResponse result = service.summary(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31)
    );

    Assertions.assertEquals("danger", result.individualDiagnosticLevel());
    Assertions.assertTrue(result.individualDiagnosticMessage().contains("pas une decision juridique"));
  }

  @Test
  void reportsMissingSaleDatesAndAmounts() {
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of(
        sale(1, "No date", "2026-01-01", null, "10.00", "100.00"),
        sale(2, "No amount", "2026-01-01", "2026-01-10", "10.00", null)
    ));

    AdministrativeSummaryResponse result = service.summary(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31)
    );

    Assertions.assertEquals(2L, result.incompleteSaleCount());
    Assertions.assertEquals(1L, result.missingSaleDateCount());
    Assertions.assertEquals(1L, result.missingSaleAmountCount());
  }

  @Test
  void supportsMonthlyAndQuarterlyBuckets() {
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of(
        sale(1, "Jan", "2026-01-01", "2026-01-15", "10.00", "100.00"),
        sale(2, "Mar", "2026-03-01", "2026-03-15", "20.00", "200.00")
    ));

    AdministrativeSummaryResponse monthly = service.summary(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 3, 31)
    );

    user.setDeclarationFrequency(DeclarationFrequency.QUARTERLY);
    AdministrativeSummaryResponse quarterly = service.summary(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 3, 31)
    );

    Assertions.assertEquals(3, monthly.periodBreakdown().size());
    Assertions.assertEquals(1, quarterly.periodBreakdown().size());
    Assertions.assertEquals(new BigDecimal("300.00"), quarterly.periodBreakdown().get(0).revenue());
  }

  @Test
  void generatesPdfDocument() {
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of(
        sale(1, "A", "2026-01-01", "2026-01-15", "10.00", "100.00")
    ));

    AdministrativeService.GeneratedAdministrativeDocument document = service.generateDocument(
        1L,
        new AdministrativeDocumentRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 2026),
        "urssaf-summary"
    );

    Assertions.assertTrue(new String(document.content(), 0, 8).startsWith("%PDF-1."));
    Assertions.assertTrue(document.filename().endsWith(".pdf"));
  }

  @Test
  void markDeclarationDoneCreatesAdministrativeRecord() {
    AdministrativeDocumentRecordResponse record = service.markDeclarationDone(
        1L,
        new AdministrativeDeclarationRecordRequest(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            new BigDecimal("300.00"),
            "Declaration URSSAF janvier faite",
            "micro"
        )
    );

    Assertions.assertEquals("urssaf-declaration", record.type());
    Assertions.assertEquals("done", record.status());
    Assertions.assertEquals(new BigDecimal("300.00"), record.amount());
    Mockito.verify(adminStateRepository).save(Mockito.any(AdminState.class));
  }

  @Test
  void generateMissingInvoicesCreatesDraftsAndArchivesBatch() {
    Mockito.when(snkVenteRepository.findByUser_IdOrderByDateAchatDesc(1L)).thenReturn(List.of(
        sale(1, "A", "2026-01-01", "2026-01-15", "10.00", "100.00"),
        sale(2, "B", "2026-01-02", "2026-01-20", "20.00", "150.00")
    ));

    AdministrativeInvoiceBatchResponse result = service.generateMissingInvoices(
        1L,
        new AdministrativeDocumentRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 2026)
    );

    Assertions.assertEquals(2, result.generatedCount());
    Assertions.assertEquals("MS-2026-0001", result.invoices().get(0).get("number"));
    Assertions.assertEquals(1, result.invoices().get(0).get("relatedSaleId"));
    Assertions.assertEquals("draft", result.invoices().get(0).get("status"));
    Assertions.assertNotNull(result.record());
    Assertions.assertEquals("invoice-batch", result.record().type());
    Mockito.verify(adminInvoiceRepository, Mockito.times(2)).save(Mockito.any(AdminInvoice.class));
    Mockito.verify(adminStateRepository).save(Mockito.any(AdminState.class));
  }

  private SnkVente sale(
      int id,
      String name,
      String purchaseDate,
      String saleDate,
      String purchaseAmount,
      String saleAmount
  ) {
    SnkVente item = new SnkVente();
    item.setId(id);
    item.setNomItem(name);
    item.setDateAchat(purchaseDate == null ? null : LocalDate.parse(purchaseDate));
    item.setDateVente(saleDate == null ? null : LocalDate.parse(saleDate));
    item.setPrixRetail(purchaseAmount == null ? null : new BigDecimal(purchaseAmount));
    item.setPrixResell(saleAmount == null ? null : new BigDecimal(saleAmount));
    item.setCategorie("Sneakers");
    item.setType("SNEAKER");
    return item;
  }
}
