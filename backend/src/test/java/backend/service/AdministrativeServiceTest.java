package backend.service;

import backend.dto.AdministrativeDocumentRequest;
import backend.dto.AdministrativeDocumentDescriptorResponse;
import backend.dto.AdministrativeProfileRequest;
import backend.dto.AdministrativeProfileResponse;
import backend.dto.AdministrativeSummaryResponse;
import backend.entity.DeclarationFrequency;
import backend.entity.LegalProfileType;
import backend.entity.SnkVente;
import backend.entity.User;
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
  private AdministrativeService service;
  private User user;

  @BeforeEach
  void setup() {
    userRepository = Mockito.mock(UserRepository.class);
    snkVenteRepository = Mockito.mock(SnkVenteRepository.class);
    service = new AdministrativeService(userRepository, snkVenteRepository, new AdministrativePdfService());

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
        "98765432100011",
        "Ada Shop",
        "Ada Sneakers",
        "2 rue Commerce",
        null,
        null,
        "QUARTERLY",
        "NO",
        "YES",
        LocalDate.of(2026, 1, 10),
        "",
        "A_VERIFIER",
        true,
        true
    ));

    Assertions.assertEquals(LegalProfileType.MICRO_ENTREPRISE, result.profileType());
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
            "Recap annuel fiscal"
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
