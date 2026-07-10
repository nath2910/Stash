package backend.service;

import backend.dto.AdministrativeAlertResponse;
import backend.dto.AdministrativeDeclarationRecordRequest;
import backend.dto.AdministrativeDocumentDescriptorResponse;
import backend.dto.AdministrativeDocumentRecordResponse;
import backend.dto.AdministrativeDocumentRequest;
import backend.dto.AdministrativeInvoiceBatchResponse;
import backend.dto.AdministrativeObligationResponse;
import backend.dto.AdministrativePeriodBucketResponse;
import backend.dto.AdministrativeProfileRequest;
import backend.dto.AdministrativeProfileResponse;
import backend.dto.AdministrativeQualityCheckResponse;
import backend.dto.AdministrativeSaleLineResponse;
import backend.dto.AdministrativeSourceResponse;
import backend.dto.AdministrativeSummaryResponse;
import backend.entity.AdminInvoice;
import backend.entity.AdminState;
import backend.entity.BusinessActivityType;
import backend.entity.BusinessRegime;
import backend.entity.DeclarationFrequency;
import backend.entity.LegalProfileType;
import backend.entity.SnkVente;
import backend.entity.TaxCategory;
import backend.entity.User;
import backend.entity.VatStatus;
import backend.repository.AdminInvoiceRepository;
import backend.repository.AdminStateRepository;
import backend.repository.SnkVenteRepository;
import backend.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdministrativeService {

  public static final String LAST_SOURCE_VERIFICATION = "2026-07-03";
  private static final String DEFAULT_ACTIVITY = "Achat-revente de marchandises";
  private static final String DEFAULT_FISCAL_REGIME = "Micro-BIC achat-revente";
  private static final String DEFAULT_URSSAF_CATEGORY = "Vente de marchandises";
  private static final String ADMINISTRATIVE_RECORDS_KEY = "administrativeRecords";
  private static final BigDecimal MICRO_FISCAL_GOODS_THRESHOLD_2026 = new BigDecimal("203100");
  private static final BigDecimal VAT_FRANCHISE_GOODS_BASE_THRESHOLD_2026 = new BigDecimal("85000");
  private static final BigDecimal VAT_FRANCHISE_GOODS_TOLERANCE_THRESHOLD_2026 = new BigDecimal("93500");
  private static final BigDecimal WARNING_THRESHOLD_RATIO = new BigDecimal("0.85");
  private static final String REVENUE_RULE =
      "CA a declarer = total brut encaisse hors TVA. Les achats, frais, commissions et livraisons ne sont pas deduits.";
  private static final List<String> PROOF_KEYS = List.of(
      "proof",
      "proofUrl",
      "factureUrl",
      "invoiceUrl",
      "receiptUrl",
      "attachmentUrl",
      "hasAttachment"
  );
  private static final Set<String> LEGAL_STATUSES = Set.of(
      "none",
      "personal",
      "micro",
      "ei_real",
      "eurl",
      "sarl",
      "sasu",
      "sas",
      "other"
  );
  private static final Set<String> ACTIVITY_TYPES = Set.of(
      "goods_resale",
      "services",
      "mixed",
      "second_hand",
      "consignment",
      "ecommerce",
      "physical_shop",
      "marketplaces",
      "events"
  );
  private static final Set<String> VAT_REGIMES = Set.of(
      "unknown",
      "franchise_base",
      "real_simplified",
      "real_normal",
      "margin_scheme_possible"
  );
  private static final Set<String> DECLARATION_TYPES = Set.of(
      "urssaf_monthly",
      "urssaf_quarterly",
      "vat_monthly",
      "vat_quarterly",
      "vat_annual",
      "annual_closing",
      "cfe",
      "accountant_export"
  );

  private final UserRepository userRepository;
  private final SnkVenteRepository snkVenteRepository;
  private final AdministrativePdfService administrativePdfService;
  private final AdminStateRepository adminStateRepository;
  private final AdminInvoiceRepository adminInvoiceRepository;

  public AdministrativeService(
      UserRepository userRepository,
      SnkVenteRepository snkVenteRepository,
      AdministrativePdfService administrativePdfService,
      AdminStateRepository adminStateRepository,
      AdminInvoiceRepository adminInvoiceRepository
  ) {
    this.userRepository = userRepository;
    this.snkVenteRepository = snkVenteRepository;
    this.administrativePdfService = administrativePdfService;
    this.adminStateRepository = adminStateRepository;
    this.adminInvoiceRepository = adminInvoiceRepository;
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
    String legalStatus = parseLegalStatus(firstNonBlank(request.legalStatus(), request.profileType()));
    LegalProfileType profileType = profileTypeForLegalStatus(legalStatus);
    DeclarationFrequency declarationFrequency = parseDeclarationFrequency(request.declarationFrequency());
    List<String> activities = normalizeValues(request.activities(), ACTIVITY_TYPES, "activites");
    if (activities.isEmpty()) {
      activities = defaultActivities(legalStatus);
    }
    String vatRegime = normalizeVatRegime(request.vatRegime(), request.vatFranchise(), legalStatus);
    List<String> declarations = normalizeValues(request.declarations(), DECLARATION_TYPES, "declarations");
    if (declarations.isEmpty()) {
      declarations = defaultDeclarations(legalStatus, declarationFrequency);
    }

    user.setLegalProfileType(profileType);
    user.setAdministrativeLegalStatus(legalStatus);
    user.setAdministrativeActivities(joinValues(activities));
    user.setAdministrativeVatRegime(vatRegime);
    user.setAdministrativeDeclarations(joinValues(declarations));
    user.setAdministrativeDisplayName(trimToNull(request.displayName()));
    user.setAdministrativeAddress(trimToNull(request.address()));
    user.setBusinessName(trimToNull(request.businessName()));
    user.setOwnerName(trimToNull(request.ownerName()));
    user.setLegalForm(defaultText(request.legalForm(), defaultLegalForm(legalStatus)));
    user.setFiscalYearEndMonth(normalizeMonth(request.fiscalYearEndMonth()));
    user.setFiscalYearEndDay(normalizeDay(request.fiscalYearEndDay()));
    user.setDeclarationPeriodicity(firstNonBlank(
        normalizeDeclarationPeriodicity(request.declarationPeriodicity()),
        declarationPeriodicityForFrequency(declarationFrequency)
    ));
    user.setUrssafCategory(defaultText(request.urssafCategory(), isMicro(legalStatus) ? DEFAULT_URSSAF_CATEGORY : null));
    user.setDefaultVatRate(normalizeVatRate(request.defaultVatRate()));
    user.setAdministrativeNotes(trimToNull(request.notes()));
    user.setAdministrativeVerificationStatus(defaultText(request.verificationStatus(), "A_VERIFIER"));
    user.setAdminUsesOnlinePlatforms(Boolean.TRUE.equals(request.usesOnlinePlatforms()));
    user.setAdminBuysForResale(Boolean.TRUE.equals(request.buysForResale()));

    if (request.siret() != null) {
      String siret = normalizeSiret(request.siret());
      user.setSiret(siret);
      user.setSiren(siret == null ? null : siret.substring(0, 9));
    }

    if (isMicro(legalStatus)) {
      user.setTradeName(trimToNull(request.tradeName()));
      user.setMainActivity(defaultText(request.mainActivity(), DEFAULT_ACTIVITY));
      user.setFiscalRegime(defaultText(request.fiscalRegime(), DEFAULT_FISCAL_REGIME));
      user.setTaxCategory(TaxCategory.BIC);
      user.setBusinessRegime(BusinessRegime.MICRO_ENTREPRISE);
      user.setBusinessActivityType(BusinessActivityType.ACHAT_REVENTE);
      user.setVatStatus(vatStatusForRegime(vatRegime));
      user.setDeclarationFrequency(declarationFrequency);
      user.setWithholdingTaxOption(parseTriState(request.withholdingTaxOption()));
      user.setVatFranchise(parseTriState(request.vatFranchise()));
      user.setActivityStartDate(request.activityStartDate());
    } else if (isBusinessLegalStatus(legalStatus)) {
      user.setTradeName(trimToNull(request.tradeName()));
      user.setMainActivity(trimToNull(request.mainActivity()));
      user.setFiscalRegime(trimToNull(request.fiscalRegime()));
      user.setTaxCategory(null);
      user.setBusinessRegime(null);
      user.setBusinessActivityType(null);
      user.setVatStatus(vatStatusForRegime(vatRegime));
      user.setDeclarationFrequency(declarationFrequency);
      user.setWithholdingTaxOption(null);
      user.setVatFranchise(parseTriState(request.vatFranchise()));
      user.setActivityStartDate(request.activityStartDate());
    } else {
      user.setTradeName(trimToNull(request.tradeName()));
      user.setMainActivity(trimToNull(request.mainActivity()));
      user.setFiscalRegime(null);
      user.setTaxCategory(null);
      user.setBusinessRegime(null);
      user.setBusinessActivityType(null);
      user.setVatStatus(null);
      user.setDeclarationFrequency(null);
      user.setWithholdingTaxOption(null);
      user.setVatFranchise(null);
      user.setActivityStartDate(request.activityStartDate());
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
    return buildSummary(
        userId,
        user,
        start,
        end,
        snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId),
        invoicesForUser(userId)
    );
  }

  @Transactional(readOnly = true)
  public AdministrativeSummaryResponse yearSummary(Long userId, int year) {
    User user = findUser(userId);
    LocalDate start = LocalDate.of(year, 1, 1);
    LocalDate end = LocalDate.of(year, 12, 31);
    return buildSummary(
        userId,
        user,
        start,
        end,
        snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId),
        invoicesForUser(userId)
    );
  }

  @Transactional(readOnly = true)
  public List<AdministrativeDocumentDescriptorResponse> documents(Long userId) {
    AdministrativeProfileResponse profile = getProfile(userId);
    boolean micro = isMicro(profile.legalStatus());
    boolean company = isCompanyLegalStatus(normalizeLegalStatus(profile.legalStatus()))
        || "ei_real".equals(normalizeLegalStatus(profile.legalStatus()));
    List<String> activities = profile.activities() == null ? List.of() : profile.activities();
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
        micro ? "Registre des recettes" : company ? "Registre des ventes" : "Recapitulatif des ventes",
        "Liste chronologique des ventes et montants encaisses connus dans l'application.",
        "/administrative/documents/receipts-register",
        "PDF",
        micro ? "MICRO_ENTREPRISE" : company ? "SOCIETE_REEL" : "PARTICULIER"
    ));

    if (micro || company) {
      docs.add(new AdministrativeDocumentDescriptorResponse(
          "purchases-register",
          company ? "Registre achats" : "Registre des achats",
          "Recapitulatif des achats rattaches a l'activite d'achat-revente.",
          "/administrative/documents/purchases-register",
          "PDF",
          company ? "SOCIETE_REEL" : "MICRO_ENTREPRISE"
      ));
    }

    if ((micro || company) && activities.contains("second_hand")) {
      docs.add(new AdministrativeDocumentDescriptorResponse(
          "second-hand-register",
          "Registre des objets d'occasion",
          "Registre preparatoire des objets d'occasion detectes dans les donnees MyStash.",
          "/administrative/documents/second-hand-register",
          "PDF",
          company ? "SOCIETE_REEL" : "MICRO_ENTREPRISE"
      ));
    }

    if (micro) {
      docs.add(new AdministrativeDocumentDescriptorResponse(
          "fiscal-summary",
          "Recap annuel fiscal",
          "Aide preparatoire pour la declaration annuelle.",
          "/administrative/documents/fiscal-summary",
          "PDF",
          "MICRO_ENTREPRISE"
      ));
      docs.add(new AdministrativeDocumentDescriptorResponse(
          "annual-folder",
          "Dossier annuel complet",
          "Dossier exportable regroupant CA annuel, ventes, achats, registres et controles manquants.",
          "/administrative/documents/annual-folder",
          "PDF",
          "MICRO_ENTREPRISE"
      ));
    } else if (company) {
      docs.add(new AdministrativeDocumentDescriptorResponse(
          "fiscal-summary",
          "Dossier comptable preparatoire",
          "Recapitulatif annuel preparatoire pour resultat, cloture et comptable.",
          "/administrative/documents/fiscal-summary",
          "PDF",
          "SOCIETE_REEL"
      ));
      docs.add(new AdministrativeDocumentDescriptorResponse(
          "annual-folder",
          "Dossier annuel complet",
          "Dossier exportable pour comptable ou archivage interne avec ventes, achats, controles et recap fiscal.",
          "/administrative/documents/annual-folder",
          "PDF",
          "SOCIETE_REEL"
      ));
    }
    return docs;
  }

  @Transactional(readOnly = true)
  public List<AdministrativeDocumentRecordResponse> documentRecords(Long userId) {
    return readAdministrativeRecords(userId);
  }

  @Transactional
  public GeneratedAdministrativeDocument generateDocument(
      Long userId,
      AdministrativeDocumentRequest request,
      String documentType
  ) {
    String kind = normalizeDocumentType(documentType);
    User user = findUser(userId);
    int year = request != null && request.year() != null ? request.year() : LocalDate.now().getYear();
    LocalDate start = request != null ? request.periodStart() : null;
    LocalDate end = request != null ? request.periodEnd() : null;
    if ("fiscal-summary".equals(kind) || "annual-folder".equals(kind)) {
      start = LocalDate.of(year, 1, 1);
      end = LocalDate.of(year, 12, 31);
    }
    start = start != null ? start : LocalDate.now().withDayOfMonth(1);
    end = end != null ? end : LocalDate.now();
    if (start.isAfter(end)) {
      LocalDate tmp = start;
      start = end;
      end = tmp;
    }
    AdministrativeSummaryResponse summary = buildSummary(
        userId,
        user,
        start,
        end,
        snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId),
        invoicesForUser(userId)
    );
    byte[] content = administrativePdfService.generate(kind, summary);
    String filename = documentFilename(kind, summary);
    saveAdministrativeRecord(user, documentRecord(kind, summary, filename, content.length));
    return new GeneratedAdministrativeDocument(content, filename);
  }

  @Transactional
  public AdministrativeDocumentRecordResponse markDeclarationDone(
      Long userId,
      AdministrativeDeclarationRecordRequest request
  ) {
    if (request == null || request.periodStart() == null || request.periodEnd() == null) {
      throw badRequest("Periode de declaration manquante");
    }
    User user = findUser(userId);
    LocalDate start = request.periodStart();
    LocalDate end = request.periodEnd();
    if (start.isAfter(end)) {
      LocalDate tmp = start;
      start = end;
      end = tmp;
    }
    Map<String, Object> record = new LinkedHashMap<>();
    record.put("id", recordId("urssaf-declaration", start, end));
    record.put("profileId", defaultText(request.profileId(), deriveLegalStatus(user)));
    record.put("type", "urssaf-declaration");
    record.put("title", defaultText(request.title(), "Declaration URSSAF marquee comme faite"));
    record.put("periodStart", start.toString());
    record.put("periodEnd", end.toString());
    record.put("status", "done");
    record.put("amount", nz(request.amount()));
    record.put("mimeType", "application/json");
    AdministrativeDocumentRecordResponse saved = saveAdministrativeRecord(user, record);
    return saved;
  }

  @Transactional
  public AdministrativeInvoiceBatchResponse generateMissingInvoices(Long userId, AdministrativeDocumentRequest request) {
    User user = findUser(userId);
    LocalDate start = request != null && request.periodStart() != null
        ? request.periodStart()
        : LocalDate.now().withDayOfMonth(1);
    LocalDate end = request != null && request.periodEnd() != null ? request.periodEnd() : LocalDate.now();
    if (start.isAfter(end)) {
      LocalDate tmp = start;
      start = end;
      end = tmp;
    }

    List<AdminInvoice> existingInvoices = invoicesForUser(userId);
    Set<String> invoicedSaleIds = relatedSaleIds(existingInvoices);
    int sequence = nextInvoiceSequence(existingInvoices, end.getYear());
    List<Map<String, Object>> generated = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    LocalDate finalStart = start;
    LocalDate finalEnd = end;

    List<SnkVente> sales = snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId)
        .stream()
        .filter(item -> item.getId() != null)
        .filter(item -> item.getDateVente() != null && item.getPrixResell() != null)
        .filter(item -> dateInRange(item.getDateVente(), finalStart, finalEnd))
        .filter(item -> !invoicedSaleIds.contains(String.valueOf(item.getId())))
        .sorted(Comparator.comparing(SnkVente::getDateVente).thenComparing(SnkVente::getId))
        .toList();

    for (SnkVente sale : sales) {
      AdminInvoice invoice = new AdminInvoice();
      String number = "MS-" + end.getYear() + "-" + String.format("%04d", sequence++);
      BigDecimal amount = nz(sale.getPrixResell());
      Map<String, Object> payload = invoicePayload(user, sale, number, amount);

      invoice.setUser(user);
      invoice.setDocumentNumber(number);
      invoice.setDocumentType("invoice");
      invoice.setStatus("draft");
      invoice.setDocumentDate(sale.getDateVente());
      invoice.setCustomerName(textValue(payload.get("customerName")));
      invoice.setTotalTtc(amount);
      invoice.setPayload(payload);

      generated.add(toInvoiceDocument(adminInvoiceRepository.save(invoice)));
      total = total.add(amount);
    }

    AdministrativeDocumentRecordResponse record = null;
    if (!generated.isEmpty()) {
      Map<String, Object> recordPayload = new LinkedHashMap<>();
      recordPayload.put("id", recordId("invoice-batch", start, end));
      recordPayload.put("profileId", deriveLegalStatus(user));
      recordPayload.put("type", "invoice-batch");
      recordPayload.put("title", generated.size() + " facture(s) generee(s)");
      recordPayload.put("periodStart", start.toString());
      recordPayload.put("periodEnd", end.toString());
      recordPayload.put("status", "generated");
      recordPayload.put("amount", total);
      recordPayload.put("fileSize", (long) generated.size());
      recordPayload.put("mimeType", "application/json");
      record = saveAdministrativeRecord(user, recordPayload);
    }

    return new AdministrativeInvoiceBatchResponse(generated.size(), generated, record);
  }

  private String documentFilename(String kind, AdministrativeSummaryResponse summary) {
    return switch (kind) {
      case "urssaf-summary" -> "fiche-saisie-urssaf-" + summary.periodStart() + "-" + summary.periodEnd() + ".pdf";
      case "fiscal-summary" -> "recap-annuel-fiscal-" + summary.year() + ".pdf";
      case "receipts-register" -> "registre-recettes-" + summary.periodStart() + "-" + summary.periodEnd() + ".pdf";
      case "purchases-register" -> "registre-achats-" + summary.periodStart() + "-" + summary.periodEnd() + ".pdf";
      case "second-hand-register" -> "registre-objets-occasion-" + summary.periodStart() + "-" + summary.periodEnd() + ".pdf";
      case "annual-folder" -> "dossier-annuel-complet-" + summary.year() + ".pdf";
      default -> kind + "-" + summary.periodStart() + "-" + summary.periodEnd() + ".pdf";
    };
  }

  private Map<String, Object> documentRecord(
      String kind,
      AdministrativeSummaryResponse summary,
      String filename,
      int fileSize
  ) {
    Map<String, Object> record = new LinkedHashMap<>();
    record.put("id", recordId(kind, summary.periodStart(), summary.periodEnd()));
    record.put("profileId", summary.profile().legalStatus());
    record.put("type", kind);
    record.put("title", documentTitle(kind));
    record.put("periodStart", summary.periodStart().toString());
    record.put("periodEnd", summary.periodEnd().toString());
    record.put("status", "generated");
    record.put("filePath", filename);
    record.put("fileSize", (long) fileSize);
    record.put("mimeType", "application/pdf");
    record.put("amount", "urssaf-summary".equals(kind) ? summary.periodRevenue() : null);
    return record;
  }

  private String documentTitle(String kind) {
    return switch (kind) {
      case "urssaf-summary" -> "Fiche de saisie URSSAF generee";
      case "fiscal-summary" -> "Dossier annuel genere";
      case "receipts-register" -> "Livre des recettes genere";
      case "purchases-register" -> "Registre des achats genere";
      case "second-hand-register" -> "Registre des objets d'occasion genere";
      case "annual-folder" -> "Dossier annuel complet genere";
      default -> "Document administratif genere";
    };
  }

  private String recordId(String type, LocalDate start, LocalDate end) {
    return type + ":" + start + ":" + end;
  }

  private List<AdministrativeDocumentRecordResponse> readAdministrativeRecords(Long userId) {
    findUser(userId);
    return adminStateRepository.findByUser_Id(userId)
        .map(AdminState::getPayload)
        .map(payload -> recordMaps(payload.get(ADMINISTRATIVE_RECORDS_KEY)))
        .orElseGet(List::of)
        .stream()
        .map(this::toDocumentRecord)
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(
            AdministrativeDocumentRecordResponse::updatedAt,
            Comparator.nullsLast(Comparator.reverseOrder())
        ))
        .toList();
  }

  private AdministrativeDocumentRecordResponse saveAdministrativeRecord(User user, Map<String, Object> nextRecord) {
    AdminState state = adminStateRepository.findByUser_Id(user.getId())
        .orElseGet(() -> {
          AdminState created = new AdminState();
          created.setUser(user);
          return created;
        });

    Map<String, Object> payload = copyMap(state.getPayload());
    List<Map<String, Object>> records = new ArrayList<>(recordMaps(payload.get(ADMINISTRATIVE_RECORDS_KEY)));
    String id = textValue(nextRecord.get("id"));
    OffsetDateTime now = OffsetDateTime.now();
    nextRecord.put("userId", user.getId());
    List<Map<String, Object>> keptRecords = new ArrayList<>();
    for (Map<String, Object> record : records) {
      if (id.equals(textValue(record.get("id")))) {
        if (trimToNull(textValue(nextRecord.get("createdAt"))) == null) {
          nextRecord.put("createdAt", textValue(record.get("createdAt")));
        }
      } else {
        keptRecords.add(record);
      }
    }
    records = keptRecords;

    if (trimToNull(textValue(nextRecord.get("createdAt"))) == null) {
      nextRecord.put("createdAt", now.toString());
    }
    nextRecord.put("updatedAt", now.toString());
    records.add(0, copyMap(nextRecord));

    if (records.size() > 80) {
      records = new ArrayList<>(records.subList(0, 80));
    }

    payload.put(ADMINISTRATIVE_RECORDS_KEY, records);
    state.setPayload(payload);
    adminStateRepository.save(state);
    return toDocumentRecord(nextRecord);
  }

  private List<Map<String, Object>> recordMaps(Object value) {
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    List<Map<String, Object>> records = new ArrayList<>();
    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        records.add(copyMap(map));
      }
    }
    return records;
  }

  private AdministrativeDocumentRecordResponse toDocumentRecord(Map<?, ?> source) {
    if (source == null || textValue(source.get("id")).isBlank()) {
      return null;
    }
    return new AdministrativeDocumentRecordResponse(
        textValue(source.get("id")),
        longValue(source.get("userId")),
        textValue(source.get("profileId")),
        textValue(source.get("type")),
        textValue(source.get("title")),
        dateValue(source.get("periodStart")),
        dateValue(source.get("periodEnd")),
        defaultText(textValue(source.get("status")), "generated"),
        textValue(source.get("filePath")),
        longValue(source.get("fileSize")),
        textValue(source.get("mimeType")),
        textValue(source.get("checksum")),
        decimalValue(source.get("amount")),
        dateTimeValue(source.get("createdAt")),
        dateTimeValue(source.get("updatedAt"))
    );
  }

  private Map<String, Object> copyMap(Map<?, ?> source) {
    Map<String, Object> copy = new LinkedHashMap<>();
    if (source == null) return copy;
    for (Map.Entry<?, ?> entry : source.entrySet()) {
      if (entry.getKey() == null) continue;
      copy.put(String.valueOf(entry.getKey()), entry.getValue());
    }
    return copy;
  }

  private String textValue(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private LocalDate dateValue(Object value) {
    if (value instanceof LocalDate date) return date;
    String text = textValue(value);
    if (text.isBlank()) return null;
    try {
      return LocalDate.parse(text.substring(0, Math.min(10, text.length())));
    } catch (RuntimeException error) {
      return null;
    }
  }

  private OffsetDateTime dateTimeValue(Object value) {
    if (value instanceof OffsetDateTime dateTime) return dateTime;
    String text = textValue(value);
    if (text.isBlank()) return null;
    try {
      return OffsetDateTime.parse(text);
    } catch (RuntimeException error) {
      return null;
    }
  }

  private Long longValue(Object value) {
    if (value instanceof Number number) return number.longValue();
    String text = textValue(value);
    if (text.isBlank()) return null;
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException error) {
      return null;
    }
  }

  private BigDecimal decimalValue(Object value) {
    if (value instanceof BigDecimal decimal) return decimal;
    if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
    String text = textValue(value);
    if (text.isBlank()) return null;
    try {
      return new BigDecimal(text.replace(',', '.'));
    } catch (NumberFormatException error) {
      return null;
    }
  }

  private AdministrativeSummaryResponse buildSummary(
      Long userId,
      User user,
      LocalDate start,
      LocalDate end,
      List<SnkVente> items,
      List<AdminInvoice> invoices
  ) {
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
    long missingPurchaseAmountCount = 0;
    long missingProofCount = 0;
    long periodMismatchCount = 0;
    long shortResaleCount = 0;
    Map<YearMonth, Long> saleMonths = new LinkedHashMap<>();
    Set<String> invoicedSaleIds = relatedSaleIds(invoices);
    long missingInvoiceCount = 0;

    for (SnkVente item : items) {
      if (item.getPrixResell() != null && item.getDateVente() == null) {
        missingSaleDateCount += 1;
      }
      if (dateInRange(item.getDateVente(), start, end) && item.getPrixResell() == null) {
        missingSaleAmountCount += 1;
      }
      if (item.getDateAchat() != null && item.getDateVente() != null && item.getDateAchat().isAfter(item.getDateVente())) {
        periodMismatchCount += 1;
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
          if (item.getPrixRetail() == null || nz(item.getPrixRetail()).compareTo(BigDecimal.ZERO) <= 0) {
            missingPurchaseAmountCount += 1;
          }
          if (!hasProof(item)) {
            missingProofCount += 1;
          }
          if (item.getId() != null && !invoicedSaleIds.contains(String.valueOf(item.getId()))) {
            missingInvoiceCount += 1;
          }
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
        missingPurchaseAmountCount,
        missingProofCount,
        periodMismatchCount,
        missingInvoiceCount,
        annualRevenue,
        diagnostic
    );
    List<AdministrativeQualityCheckResponse> qualityChecks = buildQualityChecks(
        profile,
        sales.size(),
        missingSaleDateCount,
        missingSaleAmountCount,
        missingPurchaseAmountCount,
        missingProofCount,
        periodMismatchCount,
        missingInvoiceCount,
        annualRevenue
    );
    List<AdministrativeObligationResponse> obligations = buildObligations(
        profile,
        sales.size(),
        purchases.size(),
        periodRevenue,
        periodPurchaseTotal
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
        missingPurchaseAmountCount,
        missingProofCount,
        periodMismatchCount,
        missingInvoiceCount,
        purchases.size(),
        periodPurchaseTotal,
        REVENUE_RULE,
        diagnostic.level(),
        diagnostic.label(),
        diagnostic.message(),
        buildBreakdown(start, end, profile.declarationFrequency(), sales),
        sales,
        purchases,
        obligations,
        alerts,
        qualityChecks,
        sources()
    );
  }

  private List<AdministrativeObligationResponse> buildObligations(
      AdministrativeProfileResponse profile,
      long periodSaleCount,
      long periodPurchaseCount,
      BigDecimal periodRevenue,
      BigDecimal periodPurchaseTotal
  ) {
    List<AdministrativeObligationResponse> obligations = new ArrayList<>();
    String legalStatus = normalizeLegalStatus(profile.legalStatus());
    List<String> activities = profile.activities() == null ? List.of() : profile.activities();
    List<String> declarations = profile.declarations() == null ? List.of() : profile.declarations();
    String vatRegime = normalizeVatRegime(profile.vatRegime(), null, legalStatus);
    boolean hasSales = periodSaleCount > 0 || nz(periodRevenue).compareTo(BigDecimal.ZERO) > 0;
    boolean hasPurchases = periodPurchaseCount > 0 || nz(periodPurchaseTotal).compareTo(BigDecimal.ZERO) > 0;
    boolean resaleActivity = profile.buysForResale()
        || activities.contains("goods_resale")
        || activities.contains("mixed")
        || activities.contains("ecommerce")
        || activities.contains("marketplaces");

    if ("none".equals(legalStatus) || "personal".equals(legalStatus)) {
      obligations.add(obligation(
          "personal-inventory-export",
          "exports",
          "Exports personnels",
          "Centralisez stock, ventes estimees et exports personnels. Ces donnees ne constituent pas une declaration officielle.",
          hasSales ? "to_verify" : "pending",
          "info",
          "Verifier les donnees",
          "receipts-register"
      ));
      if (resaleActivity || hasSales) {
        obligations.add(obligation(
            "personal-bic-2042",
            "declarations",
            "2042-C-PRO BIC a verifier",
            "Si les biens sont achetes ou fabriques pour etre revendus, les recettes relevent des BIC sur la declaration annuelle.",
            "to_verify",
            "warning",
            "Verifier BIC",
            null
        ));
      }
      if (profile.usesOnlinePlatforms()) {
        obligations.add(obligation(
            "personal-platform-report",
            "declarations",
            "Releves plateformes",
            "Verifier les montants transmis par les plateformes avant la declaration annuelle.",
            "to_verify",
            "info",
            "Comparer les montants",
            null
        ));
      }
      obligations.add(obligation(
          "personal-disclaimer",
          "vigilance",
          "Cadre personnel a verifier",
          "Stash fournit des donnees indicatives et ne remplace pas une verification juridique, fiscale ou comptable.",
          "to_verify",
          "warning",
          "Lire les alertes",
          null
      ));
      return obligations;
    }

    if (isMicro(legalStatus)) {
      boolean hasKnownUrssafPeriod = declarations.contains("urssaf_monthly")
          || declarations.contains("urssaf_quarterly")
          || profile.declarationFrequency() == DeclarationFrequency.MONTHLY
          || profile.declarationFrequency() == DeclarationFrequency.QUARTERLY;
      obligations.add(obligation(
          "micro-urssaf",
          "declarations",
          "Declaration URSSAF a preparer",
          "Preparez le chiffre d'affaires encaisse, la rubrique, la periode et le SIRET avant saisie sur le portail officiel.",
          profile.siret() == null || profile.siret().isBlank() || !hasKnownUrssafPeriod ? "incomplete" : "to_verify",
          profile.siret() == null || profile.siret().isBlank() ? "warning" : "info",
          "Preparer la declaration",
          "urssaf-summary"
      ));
      obligations.add(obligation(
          "micro-income-tax-2042",
          "declarations",
          "2042-C-PRO annuelle",
          "Reporter le chiffre d'affaires annuel dans la declaration de revenus, meme en cas de versement liberatoire.",
          "to_verify",
          "info",
          "Declarer annuel",
          null
      ));
      obligations.add(obligation(
          "micro-cfe",
          "declarations",
          "CFE",
          "Cotisation fonciere des entreprises a suivre dans l'espace professionnel impots.gouv.fr, sauf exoneration applicable.",
          "pending",
          "info",
          "Verifier CFE",
          null
      ));
      obligations.add(obligation(
          "micro-invoicing",
          "pre_compta",
          "Factures et notes",
          "Conserver les factures, notes et justificatifs rattaches aux ventes et achats.",
          "to_verify",
          "info",
          "Controler pieces",
          null
      ));
      obligations.add(obligation(
          "micro-e-invoicing",
          "pre_compta",
          "Facturation electronique",
          "Anticiper reception des factures electroniques puis emission et e-reporting selon le calendrier applicable.",
          "pending",
          "info",
          "Anticiper",
          null
      ));
      obligations.add(obligation(
          "micro-receipts-register",
          "registres",
          "Livre des recettes",
          "Export preparatoire des recettes connues sur la periode. A verifier avant archivage ou transmission.",
          hasSales ? "to_verify" : "pending",
          "info",
          "Exporter le registre",
          "receipts-register"
      ));
      if (activities.contains("goods_resale") || activities.contains("mixed") || activities.contains("ecommerce")) {
        obligations.add(obligation(
            "micro-purchases-register",
            "registres",
            "Registre des achats",
            "Registre preparatoire utile pour une activite de vente de marchandises ou achat-revente.",
            hasPurchases ? "to_verify" : "pending",
            "info",
            "Exporter les achats",
          "purchases-register"
        ));
      }
      if ("franchise_base".equals(vatRegime)) {
        obligations.add(obligation(
            "micro-vat-franchise",
            "tva",
            "Franchise TVA",
            "Surveiller les seuils de franchise TVA et la mention obligatoire sur les factures.",
            "to_verify",
            "info",
            "Verifier seuils",
            null
        ));
      } else if (!"unknown".equals(vatRegime)) {
        obligations.add(obligation(
            "micro-vat-active",
            "tva",
            "TVA a preparer",
            "Preparez TVA collectee et deductible si vos donnees permettent de les suivre. Les montants restent indicatifs.",
            "to_verify",
            "warning",
            "Preparer la TVA",
            null
        ));
      }
    }

    if (activities.contains("second_hand")) {
      obligations.add(obligation(
          "second-hand-register",
          "registres",
          "Registre occasion a verifier",
          "Preparez un registre des objets mobiliers ou registre occasion si votre activite le necessite.",
          "to_verify",
          "warning",
          "Verifier le registre",
          "second-hand-register"
      ));
    }

    if ("ei_real".equals(legalStatus)) {
      obligations.add(obligation(
          "real-bic-2031",
          "declarations",
          "Resultat BIC 2031",
          "Declaration de resultat BIC et liasse fiscale selon le regime reel applicable.",
          "to_verify",
          "warning",
          "Preparer 2031",
          null
      ));
      obligations.add(obligation(
          "real-pre-accounting",
          "pre_compta",
          "Preparation pre-comptable",
          "Regroupez CA, achats, stock initial/final, variation de stock, marge et charges disponibles avant cloture.",
          "to_verify",
          "warning",
          "Preparer le dossier",
          "fiscal-summary"
      ));
      obligations.add(obligation(
          "real-accounts",
          "pre_compta",
          "Comptes annuels",
          "Bilan, compte de resultat, inventaire et pieces comptables a conserver.",
          "to_verify",
          "info",
          "Preparer cloture",
          null
      ));
      obligations.add(obligation(
          "real-vat",
          "tva",
          "TVA",
          "Declaration TVA selon le regime reel simplifie ou normal.",
          "to_verify",
          "warning",
          "Preparer TVA",
          null
      ));
      obligations.add(obligation(
          "real-cfe",
          "declarations",
          "CFE",
          "Cotisation fonciere des entreprises a verifier dans l'espace professionnel impots.gouv.fr.",
          "pending",
          "info",
          "Verifier CFE",
          null
      ));
      obligations.add(obligation(
          "real-invoicing",
          "pre_compta",
          "Facturation",
          "Factures de vente et pieces justificatives a conserver.",
          "to_verify",
          "info",
          "Controler pieces",
          null
      ));
      obligations.add(obligation(
          "real-e-invoicing",
          "pre_compta",
          "Facturation electronique",
          "Anticiper reception des factures electroniques puis emission et e-reporting selon le calendrier applicable.",
          "pending",
          "info",
          "Anticiper",
          null
      ));
    }

    if (isCompanyLegalStatus(legalStatus)) {
      obligations.add(obligation(
          "company-result-tax",
          "declarations",
          "Resultat fiscal",
          "Liasse fiscale et resultat IS ou IR selon le regime de la societe.",
          "to_verify",
          "warning",
          "Preparer resultat",
          null
      ));
      obligations.add(obligation(
          "company-accounting-package",
          "pre_compta",
          "Dossier comptable",
          "Preparez ventes, achats, stock, TVA, exports et justificatifs pour votre declaration ou votre comptable.",
          "to_verify",
          "warning",
          "Preparer les exports",
          "fiscal-summary"
      ));
      obligations.add(obligation(
          "company-accounts-deposit",
          "pre_compta",
          "Depot des comptes",
          "Comptes annuels, PV d'assemblee et depot au greffe apres cloture.",
          "to_verify",
          "warning",
          "Preparer depot",
          null
      ));
      obligations.add(obligation(
          "company-cfe",
          "declarations",
          "CFE",
          "Cotisation fonciere des entreprises a verifier dans l'espace professionnel impots.gouv.fr.",
          "pending",
          "info",
          "Verifier CFE",
          null
      ));
      obligations.add(obligation(
          "company-invoicing",
          "pre_compta",
          "Facturation",
          "Factures de vente, TVA le cas echeant, justificatifs et mentions obligatoires.",
          "to_verify",
          "info",
          "Controler pieces",
          null
      ));
      obligations.add(obligation(
          "company-e-invoicing",
          "pre_compta",
          "Facturation electronique",
          "Anticiper reception des factures electroniques puis emission et e-reporting selon le calendrier applicable.",
          "pending",
          "info",
          "Anticiper",
          null
      ));
      if (!"franchise_base".equals(vatRegime)) {
        obligations.add(obligation(
            "company-vat",
            "tva",
            "TVA a verifier",
            "Bloc de preparation TVA. Les montants sont indicatifs tant que les donnees de TVA ne sont pas completes.",
            "to_verify",
            "warning",
            "Verifier la TVA",
            null
        ));
      }
    }

    obligations.add(obligation(
        "legal-disclaimer",
        "vigilance",
        "Verification avant declaration",
        "Les montants et documents generes par Stash sont fournis a titre d'aide a la preparation.",
        "to_verify",
        "info",
        "Verifier avant declaration",
        null
    ));
    return obligations;
  }

  private AdministrativeObligationResponse obligation(
      String id,
      String category,
      String title,
      String description,
      String status,
      String severity,
      String actionLabel,
      String documentType
  ) {
    return new AdministrativeObligationResponse(
        id,
        category,
        title,
        description,
        status,
        severity,
        actionLabel,
        documentType
    );
  }

  private List<AdministrativeAlertResponse> buildAlerts(
      AdministrativeProfileResponse profile,
      long periodSaleCount,
      long missingSaleDateCount,
      long missingSaleAmountCount,
      long missingPurchaseAmountCount,
      long missingProofCount,
      long periodMismatchCount,
      long missingInvoiceCount,
      BigDecimal annualRevenue,
      IndividualDiagnostic diagnostic
  ) {
    List<AdministrativeAlertResponse> alerts = new ArrayList<>();
    if (profile.legalStatus() == null) {
      alerts.add(new AdministrativeAlertResponse(
          "danger",
          "Profil non configure",
          "Renseignez le profil revendeur dans Mon compte avant d'utiliser cette page."
      ));
    } else if (isMicro(profile.legalStatus())) {
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
          isMicro(profile.legalStatus())
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
    if (missingPurchaseAmountCount > 0) {
      alerts.add(new AdministrativeAlertResponse(
          "warning",
          "Prix d'achat manquant",
          missingPurchaseAmountCount + " vente(s) de la periode n'ont pas de prix d'achat exploitable."
      ));
    }
    if (missingProofCount > 0) {
      alerts.add(new AdministrativeAlertResponse(
          "warning",
          "Justificatif manquant",
          missingProofCount + " vente(s) de la periode n'ont pas de justificatif detectable par l'application."
      ));
    }
    if (periodMismatchCount > 0) {
      alerts.add(new AdministrativeAlertResponse(
          "warning",
          "Dates incoherentes",
          periodMismatchCount + " ligne(s) ont une date d'achat posterieure a la date de vente."
      ));
    }
    if (missingInvoiceCount > 0) {
      alerts.add(new AdministrativeAlertResponse(
          "warning",
          "Factures a generer",
          missingInvoiceCount + " vente(s) de la periode n'ont pas de facture rattachee."
      ));
    }
    if (isNearThreshold(annualRevenue, VAT_FRANCHISE_GOODS_BASE_THRESHOLD_2026)) {
      alerts.add(new AdministrativeAlertResponse(
          thresholdSeverity(annualRevenue, VAT_FRANCHISE_GOODS_BASE_THRESHOLD_2026),
          "CA annuel proche d'un seuil TVA",
          "CA annuel connu: " + moneyText(annualRevenue) + ". Seuil de base TVA achat-revente surveille: "
              + moneyText(VAT_FRANCHISE_GOODS_BASE_THRESHOLD_2026) + "."
      ));
    }
    if (isNearThreshold(annualRevenue, MICRO_FISCAL_GOODS_THRESHOLD_2026)) {
      alerts.add(new AdministrativeAlertResponse(
          thresholdSeverity(annualRevenue, MICRO_FISCAL_GOODS_THRESHOLD_2026),
          "CA annuel proche du seuil micro-fiscal",
          "CA annuel connu: " + moneyText(annualRevenue) + ". Seuil micro-fiscal achat-revente surveille: "
              + moneyText(MICRO_FISCAL_GOODS_THRESHOLD_2026) + "."
      ));
    }
    if ("personal".equals(profile.legalStatus()) && !"ok".equals(diagnostic.level())) {
      alerts.add(new AdministrativeAlertResponse(
          "warning",
          "Situation a verifier",
          diagnostic.message()
      ));
    }
    return alerts;
  }

  private List<AdministrativeQualityCheckResponse> buildQualityChecks(
      AdministrativeProfileResponse profile,
      long periodSaleCount,
      long missingSaleDateCount,
      long missingSaleAmountCount,
      long missingPurchaseAmountCount,
      long missingProofCount,
      long periodMismatchCount,
      long missingInvoiceCount,
      BigDecimal annualRevenue
  ) {
    List<AdministrativeQualityCheckResponse> checks = new ArrayList<>();
    if (missingSaleDateCount > 0) {
      checks.add(qualityCheck(
          "missing-cash-in-date",
          "danger",
          "Ventes sans date d'encaissement",
          missingSaleDateCount + " vente(s) ont un prix de vente mais pas de date d'encaissement exploitable.",
          missingSaleDateCount,
          "incomplete",
          "Corriger les ventes",
          "sales"
      ));
    }
    if (missingSaleAmountCount > 0) {
      checks.add(qualityCheck(
          "missing-sale-amount",
          "danger",
          "Ventes sans prix de vente",
          missingSaleAmountCount + " vente(s) vendue(s) n'ont pas de prix de vente exploitable.",
          missingSaleAmountCount,
          "incomplete",
          "Corriger les ventes",
          "sales"
      ));
    }
    if (missingPurchaseAmountCount > 0) {
      checks.add(qualityCheck(
          "missing-purchase",
          "warning",
          "Prix d'achat manquant",
          missingPurchaseAmountCount + " vente(s) de la periode ont un prix d'achat a 0 EUR ou manquant.",
          missingPurchaseAmountCount,
          "to_verify",
          "Corriger les ventes",
          "sales"
      ));
    }
    if (missingProofCount > 0) {
      checks.add(qualityCheck(
          "missing-proof",
          "warning",
          "Justificatifs manquants",
          missingProofCount + " vente(s) de la periode n'ont pas de justificatif detectable.",
          missingProofCount,
          "to_verify",
          "Corriger les ventes",
          "sales"
      ));
    }
    if (periodMismatchCount > 0) {
      checks.add(qualityCheck(
          "period-mismatch",
          "warning",
          "Incoherences de dates",
          periodMismatchCount + " ligne(s) ont une date d'achat posterieure a la date de vente.",
          periodMismatchCount,
          "to_verify",
          "Corriger les ventes",
          "sales"
      ));
    }
    if (missingInvoiceCount > 0 && isBusinessLegalStatus(profile.legalStatus())) {
      checks.add(qualityCheck(
          "missing-invoices",
          "warning",
          "Factures manquantes",
          missingInvoiceCount + " facture(s) peuvent etre generee(s) depuis les ventes de la periode.",
          missingInvoiceCount,
          "ready",
          "Generer les factures",
          "invoices"
      ));
    }
    if (periodSaleCount == 0) {
      checks.add(qualityCheck(
          "no-sales",
          "info",
          "Periode sans vente exploitable",
          isMicro(profile.legalStatus())
              ? "Le montant prepare pour cette periode est 0 EUR."
              : "Aucune vente exploitable n'est retenue sur cette periode.",
          0,
          "pending",
          "Verifier la periode",
          "period"
      ));
    }
    if (isMicro(profile.legalStatus()) || isCompanyLegalStatus(profile.legalStatus())) {
      addThresholdCheck(checks, annualRevenue, VAT_FRANCHISE_GOODS_BASE_THRESHOLD_2026, "threshold-vat-base",
          "CA annuel proche du seuil TVA", "Verifier seuils TVA");
      addThresholdCheck(checks, annualRevenue, MICRO_FISCAL_GOODS_THRESHOLD_2026, "threshold-micro-fiscal",
          "CA annuel proche du seuil micro-fiscal", "Verifier seuil micro");
    }
    return checks;
  }

  private void addThresholdCheck(
      List<AdministrativeQualityCheckResponse> checks,
      BigDecimal annualRevenue,
      BigDecimal threshold,
      String id,
      String title,
      String actionLabel
  ) {
    if (!isNearThreshold(annualRevenue, threshold)) {
      return;
    }
    String severity = thresholdSeverity(annualRevenue, threshold);
    checks.add(qualityCheck(
        id,
        severity,
        title,
        "CA annuel connu: " + moneyText(annualRevenue) + " / seuil surveille: " + moneyText(threshold) + ".",
        1,
        "warning".equals(severity) ? "to_verify" : "incomplete",
        actionLabel,
        "profile"
    ));
  }

  private AdministrativeQualityCheckResponse qualityCheck(
      String id,
      String severity,
      String title,
      String message,
      long count,
      String status,
      String actionLabel,
      String target
  ) {
    return new AdministrativeQualityCheckResponse(id, severity, title, message, count, status, actionLabel, target);
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

  private List<AdminInvoice> invoicesForUser(Long userId) {
    return adminInvoiceRepository.findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1000));
  }

  private Set<String> relatedSaleIds(List<AdminInvoice> invoices) {
    LinkedHashSet<String> saleIds = new LinkedHashSet<>();
    if (invoices == null) {
      return saleIds;
    }
    for (AdminInvoice invoice : invoices) {
      Map<String, Object> payload = invoice.getPayload();
      for (String key : List.of("relatedSaleId", "saleId", "venteId")) {
        String value = textValue(payload == null ? null : payload.get(key));
        if (!value.isBlank()) {
          saleIds.add(value);
        }
      }
    }
    return saleIds;
  }

  private int nextInvoiceSequence(List<AdminInvoice> invoices, int year) {
    int max = 0;
    String prefix = "MS-" + year + "-";
    if (invoices != null) {
      for (AdminInvoice invoice : invoices) {
        max = Math.max(max, invoiceSequence(invoice.getDocumentNumber(), prefix));
        Map<String, Object> payload = invoice.getPayload();
        if (payload != null) {
          max = Math.max(max, invoiceSequence(textValue(payload.get("number")), prefix));
        }
      }
    }
    return max + 1;
  }

  private int invoiceSequence(String number, String prefix) {
    String text = trimToNull(number);
    if (text == null || !text.startsWith(prefix)) {
      return 0;
    }
    try {
      return Integer.parseInt(text.substring(prefix.length()));
    } catch (NumberFormatException error) {
      return 0;
    }
  }

  private Map<String, Object> invoicePayload(User user, SnkVente sale, String number, BigDecimal amount) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("number", number);
    payload.put("type", "invoice");
    payload.put("status", "draft");
    payload.put("date", sale.getDateVente() == null ? LocalDate.now().toString() : sale.getDateVente().toString());
    payload.put("customerName", customerName(sale));
    payload.put("totalTtc", amount);
    payload.put("relatedSaleId", sale.getId());
    payload.put("saleReference", "SNK-" + sale.getId());
    payload.put("vatMention", vatMention(user));
    payload.put("generatedBy", "administrative-assistant");
    payload.put("lines", List.of(Map.of(
        "label", defaultText(sale.getNomItem(), "Vente"),
        "quantity", 1,
        "unitPrice", amount,
        "total", amount
    )));
    return payload;
  }

  private String customerName(SnkVente sale) {
    String payment = paymentMethod(sale.getMetadata());
    return "Non renseigne".equals(payment) ? "Client non renseigne" : payment;
  }

  private String vatMention(User user) {
    String legalStatus = deriveLegalStatus(user);
    String vatRegime = normalizeVatRegime(user.getAdministrativeVatRegime(), user.getVatFranchise(), legalStatus);
    if ("franchise_base".equals(vatRegime)) {
      return "TVA non applicable - article 293 B du CGI";
    }
    return "TVA selon le regime configure - a verifier";
  }

  private Map<String, Object> toInvoiceDocument(AdminInvoice invoice) {
    Map<String, Object> document = copyMap(invoice.getPayload());
    document.putIfAbsent("id", invoice.getId());
    document.put("backendId", invoice.getId());
    document.put("number", invoice.getDocumentNumber());
    document.put("type", invoice.getDocumentType());
    document.put("status", invoice.getStatus());
    document.put("date", invoice.getDocumentDate());
    document.put("customerName", invoice.getCustomerName());
    document.put("totalTtc", invoice.getTotalTtc());
    document.put("createdAt", invoice.getCreatedAt());
    document.put("updatedAt", invoice.getUpdatedAt());
    return document;
  }

  private boolean hasProof(SnkVente item) {
    try {
      if (item.getAttachments() != null && !item.getAttachments().isEmpty()) {
        return true;
      }
    } catch (RuntimeException ignored) {
      // Metadata is still checked below.
    }

    Map<String, Object> metadata = item.getMetadata();
    if (metadata == null || metadata.isEmpty()) {
      return false;
    }
    for (String key : PROOF_KEYS) {
      Object value = metadata.get(key);
      if (value instanceof Boolean bool && bool) {
        return true;
      }
      if (value != null && !String.valueOf(value).isBlank()) {
        return true;
      }
    }
    return false;
  }

  private AdministrativeProfileResponse toProfile(User user) {
    String legalStatus = deriveLegalStatus(user);
    LegalProfileType type = profileTypeForLegalStatus(legalStatus);
    List<String> activities = parseStoredValues(user.getAdministrativeActivities(), ACTIVITY_TYPES);
    if (activities.isEmpty()) {
      activities = defaultActivities(legalStatus);
    }
    String vatRegime = normalizeVatRegime(user.getAdministrativeVatRegime(), user.getVatFranchise(), legalStatus);
    List<String> declarations = parseStoredValues(user.getAdministrativeDeclarations(), DECLARATION_TYPES);
    if (declarations.isEmpty()) {
      declarations = defaultDeclarations(legalStatus, user.getDeclarationFrequency());
    }
    String businessName = firstNonBlank(
        user.getBusinessName(),
        user.getTradeName(),
        user.getAdministrativeDisplayName(),
        fullName(user),
        user.getEmail()
    );
    String ownerName = firstNonBlank(user.getOwnerName(), fullName(user), user.getEmail());
    String legalForm = firstNonBlank(user.getLegalForm(), defaultLegalForm(legalStatus));
    String declarationPeriodicity = firstNonBlank(
        normalizeDeclarationPeriodicity(user.getDeclarationPeriodicity()),
        declarationPeriodicityForFrequency(user.getDeclarationFrequency())
    );
    List<String> missing = missingFields(user);
    return new AdministrativeProfileResponse(
        type,
        legalStatus,
        activities,
        vatRegime,
        declarations,
        user.getSiret(),
        user.getSiren(),
        businessName,
        ownerName,
        firstNonBlank(user.getAdministrativeDisplayName(), fullName(user), user.getEmail()),
        user.getTradeName(),
        user.getAdministrativeAddress(),
        legalForm,
        firstNonBlank(user.getMainActivity(), isMicro(legalStatus) ? DEFAULT_ACTIVITY : null),
        firstNonBlank(user.getFiscalRegime(), isMicro(legalStatus) ? DEFAULT_FISCAL_REGIME : null),
        user.getDeclarationFrequency(),
        declarationPeriodicity,
        defaultText(user.getWithholdingTaxOption(), isMicro(legalStatus) ? "UNKNOWN" : null),
        defaultText(user.getVatFranchise(), isMicro(legalStatus) ? "UNKNOWN" : null),
        user.getActivityStartDate(),
        defaultFiscalMonth(user.getFiscalYearEndMonth()),
        defaultFiscalDay(user.getFiscalYearEndDay()),
        firstNonBlank(user.getUrssafCategory(), isMicro(legalStatus) ? DEFAULT_URSSAF_CATEGORY : null),
        user.getDefaultVatRate(),
        user.getAdministrativeNotes(),
        defaultText(user.getAdministrativeVerificationStatus(), isMicro(legalStatus) ? "A_VERIFIER" : null),
        user.isAdminUsesOnlinePlatforms(),
        user.isAdminBuysForResale(),
        legalStatus != null && missing.isEmpty(),
        user.getLegalProfileUpdatedAt(),
        missing
    );
  }

  private List<String> missingFields(User user) {
    String legalStatus = deriveLegalStatus(user);
    List<String> missing = new ArrayList<>();
    if (legalStatus == null) {
      missing.add("legalStatus");
      return missing;
    }
    if (isMicro(legalStatus)) {
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
    if (!"personal".equals(profile.legalStatus())) {
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
            "Service Public Entreprendre - regime fiscal micro-entreprise",
            "https://entreprendre.service-public.gouv.fr/vosdroits/F23267",
            LAST_SOURCE_VERIFICATION
        ),
        new AdministrativeSourceResponse(
            "Service Public Entreprendre - franchise en base de TVA",
            "https://entreprendre.service-public.gouv.fr/vosdroits/F21746",
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

  private String parseLegalStatus(String value) {
    String normalized = normalizeLegalStatus(value);
    if (normalized == null) {
      return "none";
    }
    if (!LEGAL_STATUSES.contains(normalized)) {
      throw badRequest("Situation juridique invalide");
    }
    return normalized;
  }

  private String deriveLegalStatus(User user) {
    String stored = normalizeLegalStatus(user.getAdministrativeLegalStatus());
    if (stored != null && LEGAL_STATUSES.contains(stored)) {
      return stored;
    }
    LegalProfileType type = user.getLegalProfileType();
    if (type == null) {
      return "none";
    }
    return switch (type) {
      case NONE -> "none";
      case PERSONAL, PARTICULIER, INDIVIDUAL_UNDER_5K_MONTH -> "personal";
      case MICRO_ENTREPRISE, MICRO_ENTREPRISE_UNDER_200K_YEAR -> "micro";
      case EI_REAL -> "ei_real";
      case EURL -> "eurl";
      case SARL -> "sarl";
      case SASU -> "sasu";
      case SAS -> "sas";
      case OTHER -> "other";
    };
  }

  private String normalizeLegalStatus(String value) {
    String key = normalizeChoice(value);
    if (key == null) {
      return null;
    }
    return switch (key) {
      case "no_status", "pas_de_statut", "none", "sans_statut" -> "none";
      case "personal", "particulier", "individual", "individual_under_5k_month" -> "personal";
      case "micro", "micro_entreprise", "micro_entreprise_under_200k_year", "auto_entrepreneur" -> "micro";
      case "ei", "ei_real", "entreprise_individuelle_reel", "entreprise_individuelle_au_reel" -> "ei_real";
      case "eurl" -> "eurl";
      case "sarl" -> "sarl";
      case "sasu" -> "sasu";
      case "sas" -> "sas";
      case "other", "autre" -> "other";
      default -> key;
    };
  }

  private LegalProfileType profileTypeForLegalStatus(String legalStatus) {
    return switch (parseLegalStatus(legalStatus)) {
      case "none" -> LegalProfileType.NONE;
      case "personal" -> LegalProfileType.PARTICULIER;
      case "micro" -> LegalProfileType.MICRO_ENTREPRISE;
      case "ei_real" -> LegalProfileType.EI_REAL;
      case "eurl" -> LegalProfileType.EURL;
      case "sarl" -> LegalProfileType.SARL;
      case "sasu" -> LegalProfileType.SASU;
      case "sas" -> LegalProfileType.SAS;
      default -> LegalProfileType.OTHER;
    };
  }

  private boolean isMicro(String legalStatus) {
    return "micro".equals(normalizeLegalStatus(legalStatus));
  }

  private boolean isBusinessLegalStatus(String legalStatus) {
    String normalized = normalizeLegalStatus(legalStatus);
    return "micro".equals(normalized)
        || "ei_real".equals(normalized)
        || isCompanyLegalStatus(normalized);
  }

  private boolean isCompanyLegalStatus(String legalStatus) {
    String normalized = normalizeLegalStatus(legalStatus);
    return "eurl".equals(normalized)
        || "sarl".equals(normalized)
        || "sasu".equals(normalized)
        || "sas".equals(normalized)
        || "other".equals(normalized);
  }

  private List<String> normalizeValues(List<String> values, Set<String> allowedValues, String fieldName) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String value : values) {
      String key = normalizeChoice(value);
      if (key == null) {
        continue;
      }
      if (!allowedValues.contains(key)) {
        throw badRequest("Valeur invalide pour " + fieldName);
      }
      normalized.add(key);
    }
    return List.copyOf(normalized);
  }

  private List<String> parseStoredValues(String value, Set<String> allowedValues) {
    String text = trimToNull(value);
    if (text == null) {
      return List.of();
    }
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String part : text.split(",")) {
      String key = normalizeChoice(part);
      if (key != null && allowedValues.contains(key)) {
        normalized.add(key);
      }
    }
    return List.copyOf(normalized);
  }

  private String joinValues(List<String> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    return String.join(",", values);
  }

  private List<String> defaultActivities(String legalStatus) {
    String normalized = normalizeLegalStatus(legalStatus);
    if ("micro".equals(normalized)) {
      return List.of("goods_resale");
    }
    if ("none".equals(normalized) || "personal".equals(normalized)) {
      return List.of("second_hand");
    }
    return List.of("goods_resale", "ecommerce");
  }

  private List<String> defaultDeclarations(String legalStatus, DeclarationFrequency frequency) {
    String normalized = normalizeLegalStatus(legalStatus);
    if ("micro".equals(normalized)) {
      List<String> values = new ArrayList<>();
      values.add(frequency == DeclarationFrequency.MONTHLY ? "urssaf_monthly" : "urssaf_quarterly");
      values.add("cfe");
      return values;
    }
    if ("ei_real".equals(normalized) || isCompanyLegalStatus(normalized)) {
      return List.of("vat_annual", "annual_closing", "accountant_export");
    }
    return List.of();
  }

  private String normalizeVatRegime(String value, String vatFranchise, String legalStatus) {
    String key = normalizeChoice(value);
    if (key != null) {
      if (!VAT_REGIMES.contains(key)) {
        throw badRequest("Regime TVA invalide");
      }
      return key;
    }
    if ("YES".equalsIgnoreCase(defaultText(vatFranchise, ""))) {
      return "franchise_base";
    }
    if ("NO".equalsIgnoreCase(defaultText(vatFranchise, "")) && isBusinessLegalStatus(legalStatus)) {
      return "unknown";
    }
    return "unknown";
  }

  private VatStatus vatStatusForRegime(String vatRegime) {
    String normalized = normalizeChoice(vatRegime);
    if ("franchise_base".equals(normalized)) {
      return VatStatus.NO;
    }
    if ("real_simplified".equals(normalized)
        || "real_normal".equals(normalized)
        || "margin_scheme_possible".equals(normalized)) {
      return VatStatus.YES;
    }
    return VatStatus.UNKNOWN;
  }

  private String normalizeDeclarationPeriodicity(String value) {
    String key = normalizeChoice(value);
    if (key == null) {
      return null;
    }
    return DECLARATION_TYPES.contains(key) ? key : null;
  }

  private String declarationPeriodicityForFrequency(DeclarationFrequency frequency) {
    if (frequency == DeclarationFrequency.MONTHLY) {
      return "urssaf_monthly";
    }
    if (frequency == DeclarationFrequency.QUARTERLY) {
      return "urssaf_quarterly";
    }
    return null;
  }

  private String defaultLegalForm(String legalStatus) {
    return switch (parseLegalStatus(legalStatus)) {
      case "personal" -> "Particulier";
      case "micro" -> "Micro-entreprise";
      case "ei_real" -> "Entreprise individuelle au reel";
      case "eurl" -> "EURL";
      case "sarl" -> "SARL";
      case "sasu" -> "SASU";
      case "sas" -> "SAS";
      case "other" -> "Autre";
      default -> "Suivi personnel";
    };
  }

  private String normalizeSiret(String value) {
    String siret = trimToNull(value);
    if (siret == null) {
      return null;
    }
    if (!siret.matches("\\d{14}")) {
      throw badRequest("Le SIRET doit contenir exactement 14 chiffres");
    }
    return siret;
  }

  private Integer normalizeMonth(Integer value) {
    if (value == null) {
      return 12;
    }
    if (value < 1 || value > 12) {
      throw badRequest("Le mois de cloture doit etre compris entre 1 et 12");
    }
    return value;
  }

  private Integer normalizeDay(Integer value) {
    if (value == null) {
      return 31;
    }
    if (value < 1 || value > 31) {
      throw badRequest("Le jour de cloture doit etre compris entre 1 et 31");
    }
    return value;
  }

  private Integer defaultFiscalMonth(Integer value) {
    return value == null ? 12 : value;
  }

  private Integer defaultFiscalDay(Integer value) {
    return value == null ? 31 : value;
  }

  private BigDecimal normalizeVatRate(BigDecimal value) {
    if (value == null) {
      return null;
    }
    if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
      throw badRequest("Le taux de TVA par defaut doit etre compris entre 0 et 100");
    }
    return value;
  }

  private String normalizeChoice(String value) {
    String text = trimToNull(value);
    if (text == null) {
      return null;
    }
    return text.toLowerCase(Locale.ROOT)
        .replace('-', '_')
        .replace(' ', '_');
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
      case "urssaf-summary", "fiscal-summary", "receipts-register", "purchases-register",
          "second-hand-register", "annual-folder" -> text;
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

  private boolean isNearThreshold(BigDecimal value, BigDecimal threshold) {
    if (value == null || threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }
    return value.compareTo(threshold.multiply(WARNING_THRESHOLD_RATIO)) >= 0;
  }

  private String thresholdSeverity(BigDecimal value, BigDecimal threshold) {
    return nz(value).compareTo(threshold) >= 0 ? "danger" : "warning";
  }

  private String moneyText(BigDecimal value) {
    return nz(value).setScale(2, RoundingMode.HALF_UP).toPlainString() + " EUR";
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
