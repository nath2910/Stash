package backend.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import backend.dto.SnkVenteCreateDto;
import backend.dto.SnkVenteChildViewDto;
import backend.dto.SnkVenteGroupViewDto;
import backend.dto.SnkVenteImportDto;
import backend.dto.TopVenteProjection;
import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.SnkVenteRepository;
import backend.repository.SnkVenteRepository.BrandCount;
import backend.repository.UserRepository;

@Service
public class snkVenteService {

  private static final int MAX_IMPORT_ITEMS = 500;
  private static final String DEFAULT_ITEM_TYPE = "SNEAKER";
  private static final int MAX_TYPE_LENGTH = 80;
  private static final Pattern SAFE_METADATA_KEY = Pattern.compile("^[A-Za-z0-9_.-]{1,60}$");
  private static final Map<String, String> ITEM_TYPE_ALIASES = Map.ofEntries(
      Map.entry("SNEAKERS", "SNEAKER"),
      Map.entry("SHOE", "SNEAKER"),
      Map.entry("SHOES", "SNEAKER"),
      Map.entry("CHAUSSURE", "SNEAKER"),
      Map.entry("CHAUSSURES", "SNEAKER"),
      Map.entry("VETEMENT", "CLOTHING"),
      Map.entry("VETEMENTS", "CLOTHING"),
      Map.entry("CLOTHES", "CLOTHING"),
      Map.entry("ACCESSOIRE", "ACCESSORY"),
      Map.entry("ACCESSOIRES", "ACCESSORY"),
      Map.entry("ACCESSORIES", "ACCESSORY"),
      Map.entry("MONTRE", "WATCH"),
      Map.entry("MONTRES", "WATCH"),
      Map.entry("WATCHES", "WATCH"),
      Map.entry("ELECTRONIQUE", "ELECTRONICS"),
      Map.entry("ELECTRONICS", "ELECTRONICS"),
      Map.entry("ELECTRONIC", "ELECTRONICS"),
      Map.entry("COLLECTION", "COLLECTIBLE"),
      Map.entry("COLLECTIBLES", "COLLECTIBLE"),
      Map.entry("MAISON", "HOME"),
      Map.entry("MOBILIER", "HOME"),
      Map.entry("FURNITURE", "HOME"),
      Map.entry("POKEMON", "POKEMON_CARD"),
      Map.entry("POKEMON_CARDS", "POKEMON_CARD"),
      Map.entry("CARTE_POKEMON", "POKEMON_CARD"),
      Map.entry("CARTES_POKEMON", "POKEMON_CARD"),
      Map.entry("TICKETS", "TICKET"),
      Map.entry("AUTRE", "OTHER"),
      Map.entry("AUTRES", "OTHER")
  );

  private final SnkVenteRepository snkVenteRepository;

  private final StatsCacheEvictionService statsCacheEviction;
  private final UserRepository userRepository;

  public snkVenteService(SnkVenteRepository snkVenteRepository, UserRepository userRepository,
      StatsCacheEvictionService statsCacheEviction) {
    this.snkVenteRepository = snkVenteRepository;
    this.statsCacheEviction = statsCacheEviction;
    this.userRepository = userRepository;
  }

  private User getUserOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
  }

  public SnkVente creer(Long userId, SnkVenteCreateDto dto) {
    return creerPlusieurs(userId, dto).get(0);
  }

  @Transactional
  public List<SnkVente> creerPlusieurs(Long userId, SnkVenteCreateDto dto) {
    User user = getUserOrThrow(userId);
    int quantity = safeQuantity(dto.quantity());
    List<SnkVente> out = new ArrayList<>();
    if (quantity <= 1) {
      out.add(snkVenteRepository.save(buildEntity(user, dto)));
      statsCacheEviction.evictUser(userId);
      return out;
    }
    if (shouldCreateGroup(dto, quantity)) {
      SnkVente parent = snkVenteRepository.save(createGroupedParent(user, dto, quantity));
      out.add(parent);
      out.addAll(parent.getChildren());
      statsCacheEviction.evictUser(userId);
      return out;
    }
    List<SnkVente> entities = java.util.stream.IntStream.range(0, quantity)
        .mapToObj(i -> buildEntity(user, dto))
        .collect(Collectors.toList());
    out.addAll(snkVenteRepository.saveAll(entities));
    statsCacheEviction.evictUser(userId);
    return out;
  }

  private SnkVente buildEntity(User user, SnkVenteCreateDto dto) {
    SnkVente v = new SnkVente();
    v.setUser(user);
    applyFields(v, dto);
    return v;
  }

  private SnkVente createGroupedParent(User user, SnkVenteCreateDto dto, int quantity) {
    SnkVente parent = new SnkVente();
    parent.setUser(user);
    applyFields(parent, dto);
    parent.setGroupParent(true);
    parent.setUnitIndex(null);
    parent.setParent(null);

    List<SnkVente> children = java.util.stream.IntStream.range(0, quantity)
        .mapToObj(index -> buildChildEntity(user, parent, dto, index + 1))
        .collect(Collectors.toCollection(ArrayList::new));
    parent.setChildren(children);
    return parent;
  }

  private SnkVente buildChildEntity(User user, SnkVente parent, SnkVenteCreateDto dto, int unitIndex) {
    SnkVente child = buildEntity(user, dto);
    child.setParent(parent);
    child.setGroupParent(false);
    child.setUnitIndex(unitIndex);
    return child;
  }

  private int safeQuantity(Integer quantity) {
    if (quantity == null) return 1;
    return Math.min(50, Math.max(1, quantity));
  }

  private boolean shouldCreateGroup(SnkVenteCreateDto dto, int quantity) {
    return quantity > 1 && Boolean.TRUE.equals(dto.grouped());
  }

  @Transactional(readOnly = true)
  public List<SnkVente> rechercherParUser(Long userId) {
    return rechercherParUser(userId, null);
  }

  @Transactional(readOnly = true)
  public List<SnkVente> rechercherParUser(Long userId, Integer limit) {
    if (limit != null && limit > 0) {
      int safe = Math.min(Math.max(limit, 1), 2000);
      return snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId, PageRequest.of(0, safe));
    }
    return snkVenteRepository.findByUser_IdOrderByDateAchatDesc(userId);
  }

  @Transactional(readOnly = true)
  public List<SnkVenteGroupViewDto> rechercherGroupesParUser(Long userId, Integer limit) {
    List<SnkVente> rows = snkVenteRepository.findAllByUser_IdOrderByDateAchatDesc(userId);
    List<SnkVenteGroupViewDto> groups = buildGroupedViews(rows);
    if (limit != null && limit > 0 && limit < groups.size()) {
      return groups.subList(0, limit);
    }
    return groups;
  }

  @Transactional(readOnly = true)
  public SnkVente lire(Long userId, Integer id) {
    return snkVenteRepository.findById(id)
        .filter(v -> v.getUser() != null && userId.equals(v.getUser().getId()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vente introuvable"));
  }

  @Transactional(readOnly = true)
  public List<SnkVenteChildViewDto> lireChildren(Long userId, Integer id) {
    SnkVente parent = lire(userId, id);
    if (!parent.isGroupParent()) {
      return List.of();
    }
    return snkVenteRepository.findByParent_IdOrderByUnitIndexAscIdAsc(id).stream()
        .filter(v -> v.getUser() != null && userId.equals(v.getUser().getId()))
        .map(SnkVenteChildViewDto::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SnkVente> getDernieresVentesParUser(Long userId, int limit) {
    int safeLimit = Math.min(Math.max(limit, 1), 50);
    return snkVenteRepository.findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, safeLimit));
  }

  @Transactional(readOnly = true)
  public BigDecimal totalBenef(Long userId) {
    return snkVenteRepository.totalBenef(userId);
  }

  @Transactional(readOnly = true)
  public BigDecimal totalBenefYear(Long userId, int year) {
    return snkVenteRepository.totalBenefYear(userId, year);
  }

  @Transactional(readOnly = true)
  public BigDecimal sumPrixResell(Long userId) {
    return snkVenteRepository.sumPrixResell(userId);
  }

  @Transactional(readOnly = true)
  public List<BrandCount> graphMarque(Long userId) {
    return snkVenteRepository.graphMarque(userId);
  }

  @Transactional
  public void deleteVente(Long userId, Integer id) {
    SnkVente existing = snkVenteRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vente introuvable"));

    if (existing.getUser() == null || !userId.equals(existing.getUser().getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces interdit");
    }

    Integer parentIdToCheck = existing.getParentId();
    snkVenteRepository.delete(existing);
    cleanupEmptyGroupParent(userId, parentIdToCheck);
    statsCacheEviction.evictUser(userId);
  }

  @Transactional
  public int deleteBulk(Long userId, List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return 0;

    List<Integer> uniqueIds = ids.stream()
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (uniqueIds.isEmpty()) return 0;

    List<SnkVente> matches = snkVenteRepository.findByUser_IdAndIdIn(userId, uniqueIds);
    if (matches.isEmpty()) return 0;

    java.util.Set<Integer> selectedParentIds = matches.stream()
        .filter(SnkVente::isGroupParent)
        .map(SnkVente::getId)
        .collect(Collectors.toSet());

    java.util.Set<Integer> parentIdsToCheck = matches.stream()
        .map(SnkVente::getParentId)
        .filter(Objects::nonNull)
        .filter(parentId -> !selectedParentIds.contains(parentId))
        .collect(Collectors.toSet());

    matches.stream()
        .filter(vente -> vente.getParentId() == null || !selectedParentIds.contains(vente.getParentId()))
        .sorted(Comparator.comparing((SnkVente vente) -> vente.getParentId() == null))
        .forEach(snkVenteRepository::delete);

    parentIdsToCheck.forEach(parentId -> cleanupEmptyGroupParent(userId, parentId));
    statsCacheEviction.evictUser(userId);
    return matches.size();
  }

  @Transactional
  public SnkVenteGroupViewDto regrouperSelection(Long userId, List<Integer> ids) {
    List<Integer> uniqueIds = ids == null
        ? List.of()
        : ids.stream().filter(Objects::nonNull).distinct().toList();
    if (uniqueIds.size() < 2 || uniqueIds.size() > 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selection invalide pour le regroupement");
    }

    List<SnkVente> matches = snkVenteRepository.findByUser_IdAndIdIn(userId, uniqueIds);
    if (matches.size() != uniqueIds.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Certaines lignes sont introuvables");
    }
    if (matches.stream().anyMatch(vente -> vente.isGroupParent() || vente.getParentId() != null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisis uniquement des lignes simples hors groupe");
    }

    String expectedType = normalizeItemType(matches.get(0).getType());
    String expectedCategory = normalizeCategoryKey(matches.get(0).getCategorie(), expectedType);
    boolean sameShape = matches.stream().allMatch(vente ->
        expectedType.equals(normalizeItemType(vente.getType()))
            && expectedCategory.equals(
                normalizeCategoryKey(vente.getCategorie(), normalizeItemType(vente.getType()))));
    if (!sameShape) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Les lignes doivent partager le meme type et la meme sous-categorie"
      );
    }

    List<SnkVente> ordered = matches.stream()
        .sorted(Comparator
            .comparing(SnkVente::getDateAchat, Comparator.nullsLast(LocalDate::compareTo))
            .thenComparing(SnkVente::getId))
        .collect(Collectors.toCollection(ArrayList::new));

    User user = getUserOrThrow(userId);
    SnkVente parent = new SnkVente();
    parent.setUser(user);
    parent.setGroupParent(true);
    parent.setParent(null);
    parent.setUnitIndex(null);
    parent.setType(expectedType);
    parent.setCategorie(normalizeGroupParentCategory(ordered.get(0).getCategorie(), expectedType));
    parent.setNomItem(defaultGroupName(ordered, expectedType));
    parent.setDescription(null);
    parent.setDateAchat(resolveParentDateAchat(ordered));
    parent.setDateVente(null);
    parent.setPrixRetail(null);
    parent.setPrixResell(null);
    parent.setMetadata(new HashMap<>());
    parent = snkVenteRepository.save(parent);

    List<SnkVente> children = new ArrayList<>();
    for (int index = 0; index < ordered.size(); index += 1) {
      SnkVente child = ordered.get(index);
      child.setParent(parent);
      child.setGroupParent(false);
      child.setUnitIndex(index + 1);
      children.add(child);
    }
    parent.setChildren(children);
    snkVenteRepository.saveAll(children);
    statsCacheEviction.evictUser(userId);
    return toGroupView(parent, children);
  }

  private void cleanupEmptyGroupParent(Long userId, Integer parentId) {
    if (parentId == null) return;
    if (snkVenteRepository.countByUser_IdAndParent_Id(userId, parentId) > 0) return;

    snkVenteRepository.findById(parentId)
        .filter(parent -> parent.getUser() != null && userId.equals(parent.getUser().getId()))
        .filter(SnkVente::isGroupParent)
        .ifPresent(snkVenteRepository::delete);
  }

  public List<TopVenteProjection> getTop3VentesAnneeCourante(Long userId) {
    int currentYear = Year.now().getValue();
    return snkVenteRepository.topVentesYear(userId, currentYear).stream().limit(3).toList();
  }

  @Transactional
  public SnkVente updateVente(Long userId, Integer id, SnkVente payload) {
    SnkVente existing = snkVenteRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vente introuvable"));

    if (existing.getUser() == null || !userId.equals(existing.getUser().getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces interdit");
    }

    if (existing.isGroupParent()) {
      return updateGroupParent(userId, existing, payload);
    }

    applyFields(existing, payload);
    SnkVente saved = snkVenteRepository.save(existing);
    syncGroupParentAggregates(userId, saved.getParentId());
    statsCacheEviction.evictUser(userId);
    return saved;
  }

  @Transactional(readOnly = true)
  public List<SnkVenteRepository.LabelCount> topCategories(Long userId) {
    return snkVenteRepository.topCategories(userId, PageRequest.of(0, 10));
  }

  @Transactional(readOnly = true)
  public List<SnkVenteRepository.LabelCount> topItemsByCategorie(Long userId, String categorie) {
    return snkVenteRepository.topItemsByCategorie(userId, categorie, PageRequest.of(0, 10));
  }

  public List<SnkVente> get7DernieresVentesParUser(Long userId) {
    return getDernieresVentesParUser(userId, 7);
  }

  @Transactional
  public int importBulk(Long userId, List<SnkVenteImportDto> items) {
    User user = getUserOrThrow(userId);
    if (items == null || items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun item fourni");
    }

    if (items.size() > MAX_IMPORT_ITEMS) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Import trop volumineux (max " + MAX_IMPORT_ITEMS + " lignes)"
      );
    }

    List<SnkVente> entities = items.stream()
        .filter(Objects::nonNull)
        .map(this::trimDto)
        .filter(dto -> dto.getNomItem() != null && !dto.getNomItem().isEmpty())
        .map(dto -> {
          SnkVente v = new SnkVente();
          v.setUser(user);
          applyFields(v, dto);
          v.setNomItem(dto.getNomItem());
          return v;
        })
        .collect(Collectors.toList());

    if (entities.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune ligne valide dans le fichier");
    }

    snkVenteRepository.saveAll(entities);
    statsCacheEviction.evictUser(userId);
    return entities.size();
  }

  private List<SnkVenteGroupViewDto> buildGroupedViews(List<SnkVente> rows) {
    Map<Integer, List<SnkVente>> childrenByParentId = rows.stream()
        .filter(row -> row.getParentId() != null)
        .collect(Collectors.groupingBy(SnkVente::getParentId, LinkedHashMap::new, Collectors.toList()));

    List<SnkVenteGroupViewDto> out = new ArrayList<>();
    for (SnkVente row : rows) {
      if (row.getParentId() != null) continue;
      if (row.isGroupParent()) {
        out.add(toGroupView(row, childrenByParentId.getOrDefault(row.getId(), List.of())));
        continue;
      }
      out.add(toGroupView(row, List.of()));
    }
    return out;
  }

  private SnkVenteGroupViewDto toGroupView(SnkVente parent, List<SnkVente> rawChildren) {
    List<SnkVente> children = rawChildren.stream()
        .sorted(Comparator
            .comparing((SnkVente child) -> child.getUnitIndex() == null ? Integer.MAX_VALUE : child.getUnitIndex())
            .thenComparing(SnkVente::getId))
        .toList();

    List<SnkVenteChildViewDto> childViews = children.stream().map(SnkVenteChildViewDto::fromEntity).toList();
    boolean grouped = parent.isGroupParent();
    int quantity = grouped ? childViews.size() : 1;
    int soldCount = grouped ? (int) children.stream().filter(child -> child.getDateVente() != null).count() : (parent.getDateVente() != null ? 1 : 0);
    BigDecimal totalRetail = grouped
        ? children.stream().map(this::safeRetail).reduce(BigDecimal.ZERO, BigDecimal::add)
        : safeRetail(parent);
    BigDecimal totalResell = grouped
        ? children.stream().map(this::safeResell).reduce(BigDecimal.ZERO, BigDecimal::add)
        : safeResell(parent);
    BigDecimal totalProfit = grouped
        ? children.stream().map(this::profitOf).reduce(BigDecimal.ZERO, BigDecimal::add)
        : profitOf(parent);

    LocalDate latestSaleDate = grouped
        ? children.stream()
            .map(SnkVente::getDateVente)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null)
        : parent.getDateVente();

    return new SnkVenteGroupViewDto(
        parent.getId(),
        parent.getParentId(),
        grouped,
        quantity,
        soldCount,
        parent.getNomItem(),
        parent.getDescription(),
        parent.getCategorie(),
        parent.getType(),
        parent.getDateAchat(),
        latestSaleDate,
        parent.getPrixRetail(),
        parent.getPrixResell(),
        totalRetail,
        totalResell,
        totalProfit,
        parent.getMetadata() == null ? new HashMap<>() : new HashMap<>(parent.getMetadata()),
        childViews
    );
  }

  private BigDecimal safeRetail(SnkVente vente) {
    return vente.getPrixRetail() == null ? BigDecimal.ZERO : vente.getPrixRetail();
  }

  private BigDecimal safeResell(SnkVente vente) {
    return vente.getPrixResell() == null ? BigDecimal.ZERO : vente.getPrixResell();
  }

  private BigDecimal profitOf(SnkVente vente) {
    return safeResell(vente).subtract(safeRetail(vente));
  }

  private LocalDate resolveParentDateAchat(List<SnkVente> rows) {
    return rows.stream()
        .map(SnkVente::getDateAchat)
        .filter(Objects::nonNull)
        .min(LocalDate::compareTo)
        .orElse(null);
  }

  private SnkVente updateGroupParent(Long userId, SnkVente existing, SnkVente payload) {
    List<SnkVente> children = snkVenteRepository.findByParent_IdOrderByUnitIndexAscIdAsc(existing.getId()).stream()
        .filter(child -> child.getUser() != null && userId.equals(child.getUser().getId()))
        .collect(Collectors.toCollection(ArrayList::new));

    applyFields(existing, payload);
    existing.setGroupParent(true);
    existing.setParent(null);
    existing.setUnitIndex(null);

    propagateParentFieldsToChildren(existing, children);

    SnkVente saved = snkVenteRepository.save(existing);
    if (!children.isEmpty()) {
      snkVenteRepository.saveAll(children);
    }
    syncGroupParentAggregates(userId, existing.getId());
    return saved;
  }

  private void propagateParentFieldsToChildren(SnkVente parent, List<SnkVente> children) {
    if (children == null || children.isEmpty()) return;

    List<BigDecimal> retailShares = splitAmountAcrossChildren(parent.getPrixRetail(), children.size());
    List<BigDecimal> resellShares = splitAmountAcrossChildren(parent.getPrixResell(), children.size());
    String childCategory = normalizeGroupParentCategory(parent.getCategorie(), parent.getType());

    for (int index = 0; index < children.size(); index += 1) {
      SnkVente child = children.get(index);
      child.setType(parent.getType());
      child.setCategorie(childCategory);
      child.setDateAchat(parent.getDateAchat());
      child.setDateVente(parent.getDateVente());
      child.setPrixRetail(retailShares.get(index));
      child.setPrixResell(resellShares.get(index));
    }
  }

  private void syncGroupParentAggregates(Long userId, Integer parentId) {
    if (parentId == null) return;

    SnkVente parent = snkVenteRepository.findById(parentId)
        .filter(candidate -> candidate.getUser() != null && userId.equals(candidate.getUser().getId()))
        .filter(SnkVente::isGroupParent)
        .orElse(null);
    if (parent == null) return;

    List<SnkVente> children = snkVenteRepository.findByParent_IdOrderByUnitIndexAscIdAsc(parentId).stream()
        .filter(child -> child.getUser() != null && userId.equals(child.getUser().getId()))
        .toList();
    if (children.isEmpty()) return;

    parent.setDateAchat(resolveParentDateAchat(children));
    parent.setPrixRetail(sumNullableAmounts(children, SnkVente::getPrixRetail));
    parent.setPrixResell(sumNullableAmounts(children, SnkVente::getPrixResell));
    parent.setDateVente(children.stream()
        .map(SnkVente::getDateVente)
        .filter(Objects::nonNull)
        .max(LocalDate::compareTo)
        .orElse(null));
    snkVenteRepository.save(parent);
  }

  private List<BigDecimal> splitAmountAcrossChildren(BigDecimal total, int count) {
    if (count <= 0) return List.of();
    if (total == null) {
      return java.util.stream.IntStream.range(0, count)
          .mapToObj(index -> (BigDecimal) null)
          .toList();
    }

    BigDecimal scaled = total.setScale(2, java.math.RoundingMode.HALF_UP);
    long totalCents = scaled.movePointRight(2).longValueExact();
    long sign = Long.compare(totalCents, 0L);
    long absoluteCents = Math.abs(totalCents);
    long baseCents = absoluteCents / count;
    long remainder = absoluteCents % count;

    List<BigDecimal> out = new ArrayList<>(count);
    for (int index = 0; index < count; index += 1) {
      long cents = baseCents + (index < remainder ? 1L : 0L);
      out.add(BigDecimal.valueOf(cents * sign, 2));
    }
    return out;
  }

  private BigDecimal sumNullableAmounts(List<SnkVente> rows, java.util.function.Function<SnkVente, BigDecimal> getter) {
    BigDecimal total = BigDecimal.ZERO;
    boolean hasValue = false;

    for (SnkVente row : rows) {
      BigDecimal value = getter.apply(row);
      if (value == null) continue;
      total = total.add(value);
      hasValue = true;
    }

    return hasValue ? total : null;
  }

  private String defaultGroupName(List<SnkVente> rows, String type) {
    String category = normalizeGroupParentCategory(rows.isEmpty() ? null : rows.get(0).getCategorie(), type);
    String firstName = trimToNull(rows.isEmpty() ? null : rows.get(0).getNomItem());
    String base = firstNonBlank(category, firstName, typeDisplayLabel(type), "Lot");
    return "Lot " + rows.size() + " - " + base;
  }

  private String firstNonBlank(String... values) {
    if (values == null) return "";
    for (String value : values) {
      String cleaned = trimToNull(value);
      if (cleaned != null) return cleaned;
    }
    return "";
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String cleaned = value.trim().replaceAll("\\s+", " ");
    return cleaned.isEmpty() ? null : cleaned;
  }

  private String normalizeCategoryKey(String value, String type) {
    String cleaned = trimToNull(value);
    if (cleaned == null) return "";
    if (isMainCategoryAlias(cleaned, type)) return "";
    return Normalizer.normalize(cleaned, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  private String normalizeGroupParentCategory(String value, String type) {
    String cleaned = trimToNull(value);
    if (cleaned == null || isMainCategoryAlias(cleaned, type)) return null;
    return cleaned;
  }

  private boolean isMainCategoryAlias(String value, String type) {
    String cleaned = trimToNull(value);
    if (cleaned == null) return false;
    String normalizedValue = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
    String normalizedType = normalizeItemType(type).toLowerCase(Locale.ROOT);
    String normalizedLabel = Normalizer.normalize(typeDisplayLabel(type), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
    return normalizedValue.equals(normalizedType) || normalizedValue.equals(normalizedLabel);
  }

  private String typeDisplayLabel(String type) {
    return switch (normalizeItemType(type)) {
      case "SNEAKER" -> "Sneakers";
      case "CLOTHING" -> "Vetements";
      case "ACCESSORY" -> "Accessoires";
      case "WATCH" -> "Montres";
      case "ELECTRONICS" -> "Electronique";
      case "COLLECTIBLE" -> "Collection";
      case "HOME" -> "Maison";
      case "POKEMON_CARD" -> "Pokemon";
      case "TICKET" -> "Tickets";
      case "OTHER" -> "Autre";
      default -> normalizeItemType(type).replace('_', ' ');
    };
  }

  private SnkVenteImportDto trimDto(SnkVenteImportDto dto) {
    dto.setNomItem(dto.getNomItem() != null ? dto.getNomItem().trim() : null);
    dto.setCategorie(dto.getCategorie() != null ? dto.getCategorie().trim() : null);
    dto.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
    return dto;
  }

  private void applyFields(
      SnkVente target,
      String nomItem,
      BigDecimal prixRetail,
      BigDecimal prixResell,
      LocalDate dateAchat,
      LocalDate dateVente,
      String description,
      String categorie
  ) {
    target.setNomItem(nomItem);
    target.setPrixRetail(prixRetail);
    target.setPrixResell(prixResell);
    target.setDateAchat(dateAchat);
    target.setDateVente(dateVente);
    target.setDescription(description);
    target.setCategorie(categorie);
    target.setType(normalizeItemType(target.getType()));
  }

  private void applyFields(SnkVente target, SnkVenteCreateDto dto) {
    String resolvedType = normalizeItemType(dto.type());
    Map<String, Object> metadata = sanitizeMetadata(resolvedType, dto.metadata());
    applyFields(
        target,
        dto.nomItem(),
        dto.prixRetail(),
        dto.prixResell(),
        dto.dateAchat(),
        dto.dateVente(),
        dto.description(),
        dto.categorie()
    );
    target.setType(resolvedType);
    target.setMetadata(metadata);
  }

  private void applyFields(SnkVente target, SnkVente payload) {
    String resolvedType = normalizeItemType(payload.getType());
    Map<String, Object> metadata = sanitizeMetadata(resolvedType, payload.getMetadata());
    applyFields(
        target,
        payload.getNomItem(),
        payload.getPrixRetail(),
        payload.getPrixResell(),
        payload.getDateAchat(),
        payload.getDateVente(),
        payload.getDescription(),
        payload.getCategorie()
    );
    target.setType(resolvedType);
    target.setMetadata(metadata);
  }

  private void applyFields(SnkVente target, SnkVenteImportDto dto) {
    String resolvedType = normalizeItemType(dto.getType());
    Map<String, Object> metadata = sanitizeMetadata(resolvedType, dto.getMetadata());
    applyFields(
        target,
        dto.getNomItem(),
        dto.getPrixRetail(),
        dto.getPrixResell(),
        dto.getDateAchat(),
        dto.getDateVente(),
        dto.getDescription(),
        dto.getCategorie()
    );
    target.setType(resolvedType);
    target.setMetadata(metadata);
  }

  static String normalizeItemType(String rawType) {
    String raw = rawType != null ? rawType.trim() : "";
    if (raw.isEmpty()) return DEFAULT_ITEM_TYPE;

    String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
    if (normalized.isEmpty()) return DEFAULT_ITEM_TYPE;

    String aliased = ITEM_TYPE_ALIASES.getOrDefault(normalized, normalized);
    if (aliased.length() <= MAX_TYPE_LENGTH) return aliased;
    return aliased.substring(0, MAX_TYPE_LENGTH).replaceAll("_+$", "");
  }

  private Map<String, Object> sanitizeMetadata(String type, Map<String, Object> metadata) {
    if (metadata == null) return new HashMap<>();
    Map<String, Object> cleaned = new HashMap<>();
    metadata.forEach((k, v) -> {
      if (k == null || v == null) return;
      String key = k.trim();
      if (key.isEmpty()) return;
      if (!SAFE_METADATA_KEY.matcher(key).matches()) return;
      Object val = sanitizeMetadataValue(v);
      if (val == null) return;
      if (isKnownMetadataKey(type, key) || isCustomType(type)) cleaned.put(key, val);
      // OTHER metadata is ignored unless the item uses a custom type.
    });
    return cleaned;
  }

  private boolean isKnownMetadataKey(String type, String key) {
    return switch (type) {
      case "TICKET" -> key.matches("^(eventDate|venue|section|row|seat|status|marketUrl)$");
      case "POKEMON_CARD" -> key.matches("^(set|language|rarity|condition|grade|marketUrl)$");
      case "SNEAKER" -> key.matches("^(size|sku|colorway|condition|boxCondition|marketUrl)$");
      case "OTHER" -> key.matches("^(size|sku|colorway|condition|reference|model|supplier|purchasePlace|marketUrl)$");
      default -> false;
    };
  }

  private boolean isCustomType(String type) {
    return !"SNEAKER".equals(type)
        && !"POKEMON_CARD".equals(type)
        && !"TICKET".equals(type)
        && !"OTHER".equals(type);
  }

  private Object sanitizeMetadataValue(Object value) {
    if (value instanceof String s) {
      String trimmed = s.trim();
      if (trimmed.isEmpty()) return null;
      return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value;
    }
    String text = String.valueOf(value).trim();
    if (text.isEmpty()) return null;
    return text.length() > 500 ? text.substring(0, 500) : text;
  }
}
