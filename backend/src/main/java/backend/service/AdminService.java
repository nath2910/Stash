package backend.service;

import backend.entity.AdminInvoice;
import backend.entity.AdminState;
import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.AdminInvoiceRepository;
import backend.repository.AdminStateRepository;
import backend.repository.SnkVenteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {

  private static final List<String> PROOF_KEYS = List.of(
      "proof",
      "proofUrl",
      "factureUrl",
      "invoiceUrl",
      "receiptUrl",
      "attachmentUrl",
      "hasAttachment"
  );

  private final AdminStateRepository adminStateRepository;
  private final AdminInvoiceRepository adminInvoiceRepository;
  private final SnkVenteRepository snkVenteRepository;

  public AdminService(
      AdminStateRepository adminStateRepository,
      AdminInvoiceRepository adminInvoiceRepository,
      SnkVenteRepository snkVenteRepository
  ) {
    this.adminStateRepository = adminStateRepository;
    this.adminInvoiceRepository = adminInvoiceRepository;
    this.snkVenteRepository = snkVenteRepository;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> dashboard(Long userId, LocalDate from, LocalDate to) {
    List<SnkVente> items = snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId);
    LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
    LocalDate end = to != null ? to : LocalDate.now();
    if (start.isAfter(end)) {
      LocalDate tmp = start;
      start = end;
      end = tmp;
    }

    BigDecimal periodRevenue = BigDecimal.ZERO;
    BigDecimal periodCost = BigDecimal.ZERO;
    BigDecimal stockValue = BigDecimal.ZERO;
    long soldCount = 0;
    long purchasedCount = 0;
    long inventoryCount = 0;
    long missingProofCount = 0;

    for (SnkVente item : items) {
      boolean sold = isSold(item);
      if (sold) soldCount += 1;
      if (!sold) {
        inventoryCount += 1;
        stockValue = stockValue.add(nz(item.getPrixRetail()));
      }
      if (!hasProof(item)) missingProofCount += 1;

      if (dateInRange(item.getDateAchat(), start, end)) {
        purchasedCount += 1;
      }

      if (sold && dateInRange(item.getDateVente(), start, end)) {
        periodRevenue = periodRevenue.add(nz(item.getPrixResell()));
        periodCost = periodCost.add(nz(item.getPrixRetail()));
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("from", start);
    result.put("to", end);
    result.put("totalItems", items.size());
    result.put("soldCount", soldCount);
    result.put("purchasedCount", purchasedCount);
    result.put("inventoryCount", inventoryCount);
    result.put("missingProofCount", missingProofCount);
    result.put("periodRevenue", periodRevenue);
    result.put("periodCost", periodCost);
    result.put("periodGrossProfit", periodRevenue.subtract(periodCost));
    result.put("stockValue", stockValue);
    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> state(Long userId) {
    return adminStateRepository.findByUser_Id(userId)
        .map(AdminState::getPayload)
        .map(this::copyMap)
        .orElseGet(LinkedHashMap::new);
  }

  @Transactional
  public Map<String, Object> saveState(User user, Map<String, Object> payload) {
    AdminState state = adminStateRepository.findByUser_Id(user.getId())
        .orElseGet(() -> {
          AdminState created = new AdminState();
          created.setUser(user);
          return created;
        });

    state.setPayload(copyMap(payload));
    return copyMap(adminStateRepository.save(state).getPayload());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> settings(Long userId) {
    Map<String, Object> payload = state(userId);
    Object settings = payload.get("settings");
    if (settings instanceof Map<?, ?> map) {
      return copyMap(map);
    }
    return new LinkedHashMap<>();
  }

  @Transactional
  public Map<String, Object> saveSettings(User user, Map<String, Object> settings) {
    Map<String, Object> payload = state(user.getId());
    payload.put("settings", copyMap(settings));
    saveState(user, payload);
    return settings(user.getId());
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> invoices(Long userId, int limit) {
    int safeLimit = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 300));
    return adminInvoiceRepository
        .findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, safeLimit))
        .stream()
        .map(this::toDocument)
        .toList();
  }

  @Transactional
  public Map<String, Object> createInvoice(User user, Map<String, Object> payload) {
    Map<String, Object> document = copyMap(payload);
    String number = text(document.get("number"));
    if (number.isBlank()) {
      number = "ADM-" + LocalDate.now().getYear() + "-" + System.currentTimeMillis();
      document.put("number", number);
    }

    AdminInvoice invoice = new AdminInvoice();
    invoice.setUser(user);
    invoice.setDocumentNumber(number);
    invoice.setDocumentType(defaultText(document.get("type"), "invoice"));
    invoice.setStatus(defaultText(document.get("status"), "draft"));
    invoice.setDocumentDate(localDate(document.get("date")));
    invoice.setCustomerName(text(document.get("customerName")));
    invoice.setTotalTtc(decimal(document.get("totalTtc")));
    invoice.setPayload(document);

    return toDocument(adminInvoiceRepository.save(invoice));
  }

  @Transactional
  public Map<String, Object> markInvoicePaid(Long userId, Long invoiceId) {
    AdminInvoice invoice = findInvoice(userId, invoiceId);
    invoice.setStatus("paid");
    invoice.getPayload().put("status", "paid");
    return toDocument(adminInvoiceRepository.save(invoice));
  }

  @Transactional
  public Map<String, Object> cancelInvoice(Long userId, Long invoiceId, String reason) {
    AdminInvoice invoice = findInvoice(userId, invoiceId);
    invoice.setStatus("cancelled");
    invoice.getPayload().put("status", "cancelled");
    if (reason != null && !reason.isBlank()) {
      invoice.getPayload().put("cancelReason", reason);
    }
    return toDocument(adminInvoiceRepository.save(invoice));
  }

  @Transactional(readOnly = true)
  public String exportCsv(Long userId, LocalDate from, LocalDate to) {
    List<SnkVente> items = snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId);
    List<List<Object>> rows = new ArrayList<>();
    rows.add(List.of("date", "type", "libelle", "categorie", "montant_ttc", "cout", "profit", "statut"));

    for (SnkVente item : items) {
      boolean sold = isSold(item);
      if (dateInRange(item.getDateVente(), from, to) && sold) {
        BigDecimal revenue = nz(item.getPrixResell());
        BigDecimal cost = nz(item.getPrixRetail());
        rows.add(List.of(
            item.getDateVente(),
            "vente",
            safeText(item.getNomItem()),
            safeText(item.getCategorie()),
            revenue,
            cost,
            revenue.subtract(cost),
            "vendu"
        ));
      }

      if (dateInRange(item.getDateAchat(), from, to)) {
        rows.add(List.of(
            item.getDateAchat(),
            "achat",
            safeText(item.getNomItem()),
            safeText(item.getCategorie()),
            nz(item.getPrixRetail()),
            nz(item.getPrixRetail()),
            "",
            sold ? "vendu" : "stock"
        ));
      }
    }

    StringBuilder csv = new StringBuilder("\uFEFF");
    for (int i = 0; i < rows.size(); i += 1) {
      if (i > 0) csv.append('\n');
      csv.append(rows.get(i).stream().map(this::csvCell).reduce((a, b) -> a + ";" + b).orElse(""));
    }
    return csv.toString();
  }

  private AdminInvoice findInvoice(Long userId, Long invoiceId) {
    return adminInvoiceRepository.findByIdAndUser_Id(invoiceId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document admin introuvable"));
  }

  private Map<String, Object> toDocument(AdminInvoice invoice) {
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

  private boolean isSold(SnkVente item) {
    return item.getDateVente() != null || nz(item.getPrixResell()).compareTo(BigDecimal.ZERO) > 0;
  }

  private boolean hasProof(SnkVente item) {
    try {
      if (item.getAttachments() != null && !item.getAttachments().isEmpty()) return true;
    } catch (RuntimeException ignored) {
      // Metadata still gives a reliable proof fallback.
    }

    Map<String, Object> metadata = item.getMetadata();
    if (metadata == null || metadata.isEmpty()) return false;
    for (String key : PROOF_KEYS) {
      Object value = metadata.get(key);
      if (value instanceof Boolean bool && bool) return true;
      if (value != null && !String.valueOf(value).isBlank()) return true;
    }
    return false;
  }

  private boolean dateInRange(LocalDate date, LocalDate from, LocalDate to) {
    if (date == null) return false;
    if (from != null && date.isBefore(from)) return false;
    return to == null || !date.isAfter(to);
  }

  private BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal decimal(Object value) {
    if (value == null) return BigDecimal.ZERO;
    if (value instanceof BigDecimal decimal) return decimal;
    try {
      return new BigDecimal(String.valueOf(value).replace(',', '.'));
    } catch (NumberFormatException error) {
      return BigDecimal.ZERO;
    }
  }

  private LocalDate localDate(Object value) {
    if (value instanceof LocalDate date) return date;
    String text = text(value);
    if (text.isBlank()) return null;
    try {
      return LocalDate.parse(text.substring(0, Math.min(10, text.length())));
    } catch (RuntimeException error) {
      return null;
    }
  }

  private String defaultText(Object value, String fallback) {
    String text = text(value);
    return text.isBlank() ? fallback : text;
  }

  private String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private String safeText(Object value) {
    String text = text(value);
    return text.isBlank() ? "Non renseigne" : text;
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

  private String csvCell(Object value) {
    String text = value instanceof BigDecimal decimal
        ? decimal.toPlainString()
        : String.valueOf(value == null ? "" : value);
    return "\"" + text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
  }
}
