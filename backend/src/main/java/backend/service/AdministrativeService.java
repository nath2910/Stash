package backend.service;

import backend.dto.AdministrativeAlertResponse;
import backend.dto.AdministrativeDocumentDescriptorResponse;
import backend.dto.AdministrativeDocumentRequest;
import backend.dto.AdministrativePeriodBucketResponse;
import backend.dto.AdministrativeProfileRequest;
import backend.dto.AdministrativeProfileResponse;
import backend.dto.AdministrativeSaleLineResponse;
import backend.dto.AdministrativeSourceResponse;
import backend.dto.AdministrativeSummaryResponse;
import backend.entity.BusinessActivityType;
import backend.entity.BusinessRegime;
import backend.entity.DeclarationFrequency;
import backend.entity.LegalProfileType;
import backend.entity.SnkVente;
import backend.entity.TaxCategory;
import backend.entity.User;
import backend.entity.VatStatus;
import backend.repository.SnkVenteRepository;
import backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdministrativeService {

  public static final String LAST_SOURCE_VERIFICATION = "2026-06-13";
  private static final String DEFAULT_ACTIVITY = "Achat-revente de marchandises";
  private static final String DEFAULT_FISCAL_REGIME = "Micro-BIC achat-revente";
  private static final String REVENUE_RULE =
      "CA a declarer = total brut encaisse hors TVA. Les achats, frais, commissions et livraisons ne sont pas deduits.";

  private final UserRepository userRepository;
  private final SnkVenteRepository snkVenteRepository;
  private final AdministrativePdfService administrativePdfService;

  public AdministrativeService(
      UserRepository userRepository,
      SnkVenteRepository snkVenteRepository,
      AdministrativePdfService administrativePdfService
  ) {
    this.userRepository = userRepository;
    this.snkVenteRepository = snkVenteRepository;
    this.administrativePdfService = administrativePdfService;
  }

  @Transactional(readOnly = true)
  public AdministrativeProfileResponse getProfile(Long userId) {
    return toProfile(findUser(userId));
  }

  @Transactional
  public AdministrativeProfileResponse updateProfile(Long userId, AdministrativeProfileRequest request) {
    if (request == null) {
      throw badRequest("Profil administratif manquant");
    }

    User user = findUser(userId);
    LegalProfileType profileType = parseProfileType(request.profileType());
    user.setLegalProfileType(profileType);
    user.setAdministrativeDisplayName(trimToNull(request.displayName()));
    user.setAdministrativeAddress(trimToNull(request.address()));
    user.setAdministrativeNotes(trimToNull(request.notes()));
    user.setAdministrativeVerificationStatus(defaultText(request.verificationStatus(), "A_VERIFIER"));
    user.setAdminUsesOnlinePlatforms(Boolean.TRUE.equals(request.usesOnlinePlatforms()));
    user.setAdminBuysForResale(Boolean.TRUE.equals(request.buysForResale()));

    if (isMicro(profileType)) {
      String siret = trimToNull(request.siret());
      if (siret != null && !siret.matches("\\d{14}")) {
        throw badRequest("Le SIRET doit contenir exactement 14 chiffres");
      }
      user.setSiret(siret);
      user.setSiren(siret == null ? null : siret.substring(0, 9));
      user.setTradeName(trimToNull(request.tradeName()));
      user.setMainActivity(defaultText(request.mainActivity(), DEFAULT_ACTIVITY));
      user.setFiscalRegime(defaultText(request.fiscalRegime(), DEFAULT_FISCAL_REGIME));
      user.setTaxCategory(TaxCategory.BIC);
      user.setBusinessRegime(BusinessRegime.MICRO_ENTREPRISE);
      user.setBusinessActivityType(BusinessActivityType.ACHAT_REVENTE);
      user.setVatStatus(VatStatus.UNKNOWN);
      user.setDeclarationFrequency(parseDeclarationFrequency(request.declarationFrequency()));
      user.setWithholdingTaxOption(parseTriState(request.withholdingTaxOption()));
      user.setVatFranchise(parseTriState(request.vatFranchise()));
      user.setActivityStartDate(request.activityStartDate());
    } else {
      user.setSiret(null);
      user.setSiren(null);
      user.setTradeName(null);
      user.setMainActivity(null);
      user.setFiscalRegime(null);
      user.setTaxCategory(null);
      user.setBusinessRegime(null);
      user.setBusinessActivityType(null);
      user.setVatStatus(null);
      user.setDeclarationFrequency(null);
      user.setWithholdingTaxOption(null);
      user.setVatFranchise(null);
      user.setActivityStartDate(null);
    }

    user.setLegalProfileUpdatedAt(OffsetDateTime.now());
    user.setLegalProfileCompleted(missingFields(user).isEmpty());
    return toProfile(userRepository.save(user));
  }

  @Transactional(readOnly = true)
  public AdministrativeSummaryResponse summary(Long userId, LocalDate periodStart, LocalDate periodEnd) {
    User user = findUser(userId);
    LocalDate start = periodStart != null ? periodStart : LocalDate.now().withDayOfMonth(1);
    LocalDate end = periodEnd != null ? periodEnd : LocalDate.now();
    if (start.isAfter(end)) {
      LocalDate tmp = start;
      start = end;
      end = tmp;
    }
    return buildSummary(user, start, end, snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId));
  }

  @Transactional(readOnly = true)
  public AdministrativeSummaryResponse yearSummary(Long userId, int year) {
    User user = findUser(userId);
    LocalDate start = LocalDate.of(year, 1, 1);
    LocalDate end = LocalDate.of(year, 12, 31);
    return buildSummary(user, start, end, snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId));
  }

  @Transactional(readOnly = true)
  public List<AdministrativeDocumentDescriptorResponse> documents(Long userId) {
    AdministrativeProfileResponse profile = getProfile(userId);
    boolean micro = isMicro(profile.profileType());
    List<AdministrativeDocumentDescriptorResponse> docs = new ArrayList<>();
    if (micro) {
      docs.add(new AdministrativeDocumentDescriptorResponse(
          "urssaf-summary",
          "Fiche de saisie URSSAF",
          "Montant brut encaisse a recopier sur autoentrepreneur.urssaf.fr.",
          "/administrative/documents/urssaf-summary",
          "PDF",
          "MICRO_ENTREPRISE"
      ));
    }

    docs.add(new AdministrativeDocumentDescriptorResponse(
        "receipts-register",
        micro ? "Registre des recettes" : "Recapitulatif des ventes",
        "Liste chronologique des ventes et montants encaisses connus dans l'application.",
        "/administrative/documents/receipts-register",
        "PDF",
        micro ? "MICRO_ENTREPRISE" : "PARTICULIER"
    ));

    if (micro) {
      docs.add(new AdministrativeDocumentDescriptorResponse(
          "purchases-register",
          "Registre des achats",
          "Recapitulatif des achats rattaches a l'activite d'achat-revente.",
          "/administrative/documents/purchases-register",
          "PDF",
          "MICRO_ENTREPRISE"
      ));
      docs.add(new AdministrativeDocumentDescriptorResponse(
          "fiscal-summary",
          "Recap annuel fiscal",
          "Aide preparatoire pour la declaration annuelle.",
          "/administrative/documents/fiscal-summary",
          "PDF",
          "MICRO_ENTREPRISE"
      ));
    }
    return docs;
  }

  @Transactional(readOnly = true)
  public GeneratedAdministrativeDocument generateDocument(
      Long userId,
      AdministrativeDocumentRequest request,
      String documentType
  ) {
    String kind = normalizeDocumentType(documentType);
    int year = request != null && request.year() != null ? request.year() : LocalDate.now().getYear();
    LocalDate start = request != null ? request.periodStart() : null;
    LocalDate end = request != null ? request.periodEnd() : null;
    if ("fiscal-summary".equals(kind)) {
      start = LocalDate.of(year, 1, 1);
      end = LocalDate.of(year, 12, 31);
    }
    AdministrativeSummaryResponse summary = summary(userId, start, end);
    byte[] content = administrativePdfService.generate(kind, summary);
    String filename = documentFilename(kind, summary);
    return new GeneratedAdministrativeDocument(content, filename);
  }

  private String documentFilename(String kind, AdministrativeSummaryResponse summary) {
    return switch (kind) {
      case "urssaf-summary" -> "fiche-saisie-urssaf-" + summary.periodStart() + "-" + summary.periodEnd() + ".pdf";
      case "fiscal-summary" -> "recap-annuel-fiscal-" + summary.year() + ".pdf";
      case "receipts-register" -> "registre-recettes-" + summary.periodStart() + "-" + summary.periodEnd() + ".pdf";
      case "purchases-register" -> "registre-achats-" + summary.periodStart() + "-" + summary.periodEnd() + ".pdf";
      default -> kind + "-" + summary.periodStart() + "-" + summary.periodEnd() + ".pdf";
    };
  }

  private AdministrativeSummaryResponse buildSummary(User user, LocalDate start, LocalDate end, List<SnkVente> items) {
    AdministrativeProfileResponse profile = toProfile(user);
    int year = start.getYear();
    LocalDate yearStart = LocalDate.of(year, 1, 1);
    LocalDate yearEnd = LocalDate.of(year, 12, 31);

    List<AdministrativeSaleLineResponse> sales = new ArrayList<>();
    List<AdministrativeSaleLineResponse> purchases = new ArrayList<>();
    BigDecimal periodRevenue = BigDecimal.ZERO;
    BigDecimal annualRevenue = BigDecimal.ZERO;
    BigDecimal periodPurchaseTotal = BigDecimal.ZERO;
    long annualSaleCount = 0;
    long missingSaleDateCount = 0;
    long missingSaleAmountCount = 0;
    long shortResaleCount = 0;
    Map<YearMonth, Long> saleMonths = new LinkedHashMap<>();

    for (SnkVente item : items) {
      if (item.getPrixResell() != null && item.getDateVente() == null) {
        missingSaleDateCount += 1;
      }
      if (item.getDateVente() != null && item.getPrixResell() == null) {
        missingSaleAmountCount += 1;
      }

      if (item.getDateVente() != null && item.getPrixResell() != null) {
        if (dateInRange(item.getDateVente(), yearStart, yearEnd)) {
          annualRevenue = annualRevenue.add(nz(item.getPrixResell()));
          annualSaleCount += 1;
          saleMonths.merge(YearMonth.from(item.getDateVente()), 1L, Long::sum);
        }
        if (dateInRange(item.getDateVente(), start, end)) {
          periodRevenue = periodRevenue.add(nz(item.getPrixResell()));
          sales.add(toSaleLine(item));
        }
        if (item.getDateAchat() != null && !item.getDateAchat().isAfter(item.getDateVente())
            && !item.getDateAchat().plusDays(90).isBefore(item.getDateVente())) {
          shortResaleCount += 1;
        }
      }

      if (dateInRange(item.getDateAchat(), start, end)) {
        periodPurchaseTotal = periodPurchaseTotal.add(nz(item.getPrixRetail()));
        purchases.add(toPurchaseLine(item));
      }
    }

    sales.sort(Comparator.comparing(AdministrativeSaleLineResponse::saleDate).thenComparing(AdministrativeSaleLineResponse::id));
    purchases.sort(Comparator.comparing(AdministrativeSaleLineResponse::saleDate, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(AdministrativeSaleLineResponse::id));

    long incompleteSaleCount = missingSaleDateCount + missingSaleAmountCount;
    IndividualDiagnostic diagnostic = individualDiagnostic(
        profile,
        annualSaleCount,
        saleMonths.size(),
        shortResaleCount
    );

    List<AdministrativeAlertResponse> alerts = buildAlerts(
        profile,
        sales.size(),
        missingSaleDateCount,
        missingSaleAmountCount,
        diagnostic
    );

    return new AdministrativeSummaryResponse(
        start,
        end,
        year,
        profile,
        periodRevenue,
        annualRevenue,
        sales.size(),
        annualSaleCount,
        incompleteSaleCount,
        missingSaleDateCount,
        missingSaleAmountCount,
        purchases.size(),
        periodPurchaseTotal,
        REVENUE_RULE,
        diagnostic.level(),
        diagnostic.label(),
        diagnostic.message(),
        buildBreakdown(start, end, profile.declarationFrequency(), sales),
        sales,
        purchases,
        alerts,
        sources()
    );
  }

  private List<AdministrativeAlertResponse> buildAlerts(
      AdministrativeProfileResponse profile,
      long periodSaleCount,
      long missingSaleDateCount,
      long missingSaleAmountCount,
      IndividualDiagnostic diagnostic
  ) {
    List<AdministrativeAlertResponse> alerts = new ArrayList<>();
    if (profile.profileType() == null) {
      alerts.add(new AdministrativeAlertResponse(
          "danger",
          "Profil non configure",
          "Renseignez le profil revendeur dans Mon compte avant d'utiliser cette page."
      ));
    } else if (isMicro(profile.profileType())) {
      boolean hasSpecificProfileAlert = false;
      if (trimToNull(profile.siret()) == null) {
        hasSpecificProfileAlert = true;
        alerts.add(new AdministrativeAlertResponse(
            "warning",
            "SIRET manquant",
            "Completez le SIRET dans Mon compte avant de recopier un montant sur l'URSSAF."
        ));
      }
      if (profile.declarationFrequency() == null || profile.declarationFrequency() == DeclarationFrequency.UNKNOWN) {
        hasSpecificProfileAlert = true;
        alerts.add(new AdministrativeAlertResponse(
            "warning",
            "Periodicite URSSAF inconnue",
            "Renseignez une periodicite mensuelle ou trimestrielle dans Mon compte."
        ));
      }
      if (!profile.completed() && !hasSpecificProfileAlert) {
        alerts.add(new AdministrativeAlertResponse(
            "warning",
            "Profil micro-entreprise incomplet",
            "Completez les informations manquantes dans Mon compte."
        ));
      }
    } else if (!profile.completed()) {
      alerts.add(new AdministrativeAlertResponse(
          "warning",
          "Profil incomplet",
          "Completez le profil revendeur dans Mon compte."
      ));
    }
    if (periodSaleCount == 0) {
      alerts.add(new AdministrativeAlertResponse(
          "info",
          "Periode sans vente",
          isMicro(profile.profileType())
              ? "Le montant a declarer pour cette periode est 0 EUR."
              : "Aucune vente exploitable n'est retenue sur cette periode."
      ));
    }
    if (missingSaleAmountCount > 0) {
      alerts.add(new AdministrativeAlertResponse(
          "warning",
          "Vente sans prix de vente",
          missingSaleAmountCount + " vente(s) vendue(s) n'ont pas de prix de vente exploitable."
      ));
    }
    if (missingSaleDateCount > 0) {
      alerts.add(new AdministrativeAlertResponse(
          "warning",
          "Vente sans date de vente",
          missingSaleDateCount + " vente(s) ont un prix de vente mais pas de date de vente."
      ));
    }
    if (profile.profileType() == LegalProfileType.PARTICULIER && !"ok".equals(diagnostic.level())) {
      alerts.add(new AdministrativeAlertResponse(
          "warning",
          "Situation a verifier",
          diagnostic.message()
      ));
    }
    return alerts;
  }

  private List<AdministrativePeriodBucketResponse> buildBreakdown(
      LocalDate start,
      LocalDate end,
      DeclarationFrequency frequency,
      List<AdministrativeSaleLineResponse> sales
  ) {
    List<AdministrativePeriodBucketResponse> buckets = new ArrayList<>();
    boolean quarterly = frequency == DeclarationFrequency.QUARTERLY;
    LocalDate cursor = quarterly ? quarterStart(start) : YearMonth.from(start).atDay(1);
    while (!cursor.isAfter(end)) {
      LocalDate bucketStart = cursor.isBefore(start) ? start : cursor;
      LocalDate rawEnd = quarterly ? cursor.plusMonths(3).minusDays(1) : YearMonth.from(cursor).atEndOfMonth();
      LocalDate bucketEnd = rawEnd.isAfter(end) ? end : rawEnd;
      BigDecimal revenue = BigDecimal.ZERO;
      long count = 0;
      for (AdministrativeSaleLineResponse sale : sales) {
        if (dateInRange(sale.saleDate(), bucketStart, bucketEnd)) {
          revenue = revenue.add(nz(sale.saleAmount()));
          count += 1;
        }
      }
      buckets.add(new AdministrativePeriodBucketResponse(
          quarterly ? quarterLabel(cursor) : YearMonth.from(cursor).toString(),
          bucketStart,
          bucketEnd,
          revenue,
          count
      ));
      cursor = quarterly ? cursor.plusMonths(3) : cursor.plusMonths(1);
    }
    return buckets;
  }

  private AdministrativeSaleLineResponse toSaleLine(SnkVente item) {
    return new AdministrativeSaleLineResponse(
        item.getId(),
        "SNK-" + item.getId(),
        defaultText(item.getNomItem(), "Item sans nom"),
        item.getDateVente(),
        item.getDateVente(),
        nz(item.getPrixResell()),
        nz(item.getPrixRetail()),
        defaultText(item.getCategorie(), "Non renseigne"),
        defaultText(item.getType(), "OTHER"),
        paymentMethod(item.getMetadata())
    );
  }

  private AdministrativeSaleLineResponse toPurchaseLine(SnkVente item) {
    return new AdministrativeSaleLineResponse(
        item.getId(),
        "SNK-" + item.getId(),
        defaultText(item.getNomItem(), "Item sans nom"),
        item.getDateAchat(),
        null,
        BigDecimal.ZERO,
        nz(item.getPrixRetail()),
        defaultText(item.getCategorie(), "Non renseigne"),
        defaultText(item.getType(), "OTHER"),
        paymentMethod(item.getMetadata())
    );
  }

  private String paymentMethod(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return "Non renseigne";
    }
    for (String key : List.of("paymentMethod", "payment", "moyenPaiement", "paiement", "platform")) {
      Object value = metadata.get(key);
      if (value != null && !String.valueOf(value).isBlank()) {
        return String.valueOf(value).trim();
      }
    }
    return "Non renseigne";
  }

  private AdministrativeProfileResponse toProfile(User user) {
    List<String> missing = missingFields(user);
    LegalProfileType type = normalizeType(user.getLegalProfileType());
    return new AdministrativeProfileResponse(
        type,
        user.getSiret(),
        user.getSiren(),
        firstNonBlank(user.getAdministrativeDisplayName(), fullName(user), user.getEmail()),
        user.getTradeName(),
        user.getAdministrativeAddress(),
        firstNonBlank(user.getMainActivity(), isMicro(type) ? DEFAULT_ACTIVITY : null),
        firstNonBlank(user.getFiscalRegime(), isMicro(type) ? DEFAULT_FISCAL_REGIME : null),
        user.getDeclarationFrequency(),
        defaultText(user.getWithholdingTaxOption(), isMicro(type) ? "UNKNOWN" : null),
        defaultText(user.getVatFranchise(), isMicro(type) ? "UNKNOWN" : null),
        user.getActivityStartDate(),
        user.getAdministrativeNotes(),
        defaultText(user.getAdministrativeVerificationStatus(), isMicro(type) ? "A_VERIFIER" : null),
        user.isAdminUsesOnlinePlatforms(),
        user.isAdminBuysForResale(),
        type != null && missing.isEmpty(),
        user.getLegalProfileUpdatedAt(),
        missing
    );
  }

  private List<String> missingFields(User user) {
    LegalProfileType type = normalizeType(user.getLegalProfileType());
    List<String> missing = new ArrayList<>();
    if (type == null) {
      missing.add("profileType");
      return missing;
    }
    if (isMicro(type)) {
      if (trimToNull(user.getSiret()) == null || !user.getSiret().matches("\\d{14}")) {
        missing.add("siret");
      }
      if (user.getDeclarationFrequency() == null || user.getDeclarationFrequency() == DeclarationFrequency.UNKNOWN) {
        missing.add("declarationFrequency");
      }
    }
    return missing;
  }

  private IndividualDiagnostic individualDiagnostic(
      AdministrativeProfileResponse profile,
      long annualSaleCount,
      int activeSaleMonths,
      long shortResaleCount
  ) {
    if (profile.profileType() != LegalProfileType.PARTICULIER) {
      return new IndividualDiagnostic("none", "Non applicable", "Diagnostic reserve au profil particulier.");
    }

    int signals = 0;
    if (profile.buysForResale()) signals += 3;
    if (profile.usesOnlinePlatforms()) signals += 1;
    if (annualSaleCount >= 20) signals += 1;
    if (activeSaleMonths >= 4) signals += 1;
    if (shortResaleCount >= 3) signals += 1;

    if (signals >= 3) {
      return new IndividualDiagnostic(
          "danger",
          "Semble proche d'une activite professionnelle",
          "Plusieurs signaux apparaissent dans les donnees. Ce n'est pas une decision juridique, mais il faut verifier le cadre applicable."
      );
    }
    if (signals >= 1) {
      return new IndividualDiagnostic(
          "warning",
          "A verifier",
          "Les donnees montrent au moins un signal de vigilance. Consultez les sources officielles avant de conclure."
      );
    }
    return new IndividualDiagnostic(
        "ok",
        "Probablement occasionnel",
        "Les ventes enregistrees ressemblent a un suivi ponctuel, sous reserve des cas particuliers imposables."
    );
  }

  private List<AdministrativeSourceResponse> sources() {
    return List.of(
        new AdministrativeSourceResponse(
            "Service Public Entreprendre - regime micro-social",
            "https://entreprendre.service-public.gouv.fr/vosdroits/F37353",
            LAST_SOURCE_VERIFICATION
        ),
        new AdministrativeSourceResponse(
            "Service Public Entreprendre - obligations comptables micro-entrepreneur",
            "https://entreprendre.service-public.gouv.fr/vosdroits/F23266",
            LAST_SOURCE_VERIFICATION
        ),
        new AdministrativeSourceResponse(
            "Service Public Entreprendre - quand declarer son chiffre d'affaires",
            "https://entreprendre.service-public.gouv.fr/vosdroits/F23257",
            LAST_SOURCE_VERIFICATION
        ),
        new AdministrativeSourceResponse(
            "Autoentrepreneur Urssaf - declarer et payer mes cotisations",
            "https://www.autoentrepreneur.urssaf.fr/portail/accueil/une-question/toutes-les-fiches-pratiques/declarer-et-payer-mes-cotisation.html",
            LAST_SOURCE_VERIFICATION
        )
    );
  }

  private User findUser(Long userId) {
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
    }
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
  }

  private LegalProfileType parseProfileType(String value) {
    String text = trimToNull(value);
    if (text == null) {
      throw badRequest("Le type de profil administratif est obligatoire");
    }
    String normalized = text.toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "PARTICULIER", "INDIVIDUAL_UNDER_5K_MONTH", "INDIVIDUAL" -> LegalProfileType.PARTICULIER;
      case "MICRO_ENTREPRISE", "MICRO_ENTREPRISE_UNDER_200K_YEAR", "MICRO", "AUTO_ENTREPRENEUR" ->
          LegalProfileType.MICRO_ENTREPRISE;
      default -> throw badRequest("Type de profil administratif invalide");
    };
  }

  private LegalProfileType normalizeType(LegalProfileType type) {
    if (type == LegalProfileType.INDIVIDUAL_UNDER_5K_MONTH) {
      return LegalProfileType.PARTICULIER;
    }
    if (type == LegalProfileType.MICRO_ENTREPRISE_UNDER_200K_YEAR) {
      return LegalProfileType.MICRO_ENTREPRISE;
    }
    return type;
  }

  private boolean isMicro(LegalProfileType type) {
    return normalizeType(type) == LegalProfileType.MICRO_ENTREPRISE;
  }

  private DeclarationFrequency parseDeclarationFrequency(String value) {
    String text = trimToNull(value);
    if (text == null) {
      return DeclarationFrequency.UNKNOWN;
    }
    return switch (text.toUpperCase(Locale.ROOT)) {
      case "MONTHLY", "MENSUELLE", "MENSUEL" -> DeclarationFrequency.MONTHLY;
      case "QUARTERLY", "TRIMESTRIELLE", "TRIMESTRIEL" -> DeclarationFrequency.QUARTERLY;
      case "UNKNOWN", "INCONNU" -> DeclarationFrequency.UNKNOWN;
      default -> throw badRequest("Periodicite de declaration invalide");
    };
  }

  private String parseTriState(String value) {
    String text = trimToNull(value);
    if (text == null) {
      return "UNKNOWN";
    }
    return switch (text.toUpperCase(Locale.ROOT)) {
      case "YES", "OUI", "TRUE" -> "YES";
      case "NO", "NON", "FALSE" -> "NO";
      case "UNKNOWN", "INCONNU", "A_VERIFIER" -> "UNKNOWN";
      default -> throw badRequest("Valeur oui/non/inconnu invalide");
    };
  }

  private String normalizeDocumentType(String value) {
    String text = defaultText(value, "");
    return switch (text) {
      case "urssaf-summary", "fiscal-summary", "receipts-register", "purchases-register" -> text;
      default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document administratif inconnu");
    };
  }

  private LocalDate quarterStart(LocalDate date) {
    int quarterMonth = ((date.getMonthValue() - 1) / 3) * 3 + 1;
    return LocalDate.of(date.getYear(), quarterMonth, 1);
  }

  private String quarterLabel(LocalDate date) {
    int quarter = ((date.getMonthValue() - 1) / 3) + 1;
    return "T" + quarter + " " + date.getYear();
  }

  private boolean dateInRange(LocalDate date, LocalDate from, LocalDate to) {
    if (date == null) return false;
    if (from != null && date.isBefore(from)) return false;
    return to == null || !date.isAfter(to);
  }

  private BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private String firstNonBlank(String... values) {
    if (values == null) return null;
    for (String value : values) {
      String text = trimToNull(value);
      if (text != null) return text;
    }
    return null;
  }

  private String fullName(User user) {
    return (defaultText(user.getFirstName(), "") + " " + defaultText(user.getLastName(), "")).trim();
  }

  private String defaultText(String value, String fallback) {
    String text = trimToNull(value);
    return text == null ? fallback : text;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public record GeneratedAdministrativeDocument(byte[] content, String filename) {}

  private record IndividualDiagnostic(String level, String label, String message) {}
}
