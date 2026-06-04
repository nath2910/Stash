package backend.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import backend.dto.SnkVenteCreateDto;
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
  private final UserRepository userRepository;

  public snkVenteService(SnkVenteRepository snkVenteRepository, UserRepository userRepository) {
    this.snkVenteRepository = snkVenteRepository;
    this.userRepository = userRepository;
  }

  private User getUserOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
  }

  @CacheEvict(cacheNames = "statsQueries", allEntries = true)
  public SnkVente creer(Long userId, SnkVenteCreateDto dto) {
    User user = getUserOrThrow(userId);
    return snkVenteRepository.save(buildEntity(user, dto));
  }

  @Transactional
  @CacheEvict(cacheNames = "statsQueries", allEntries = true)
  public List<SnkVente> creerPlusieurs(Long userId, SnkVenteCreateDto dto) {
    User user = getUserOrThrow(userId);
    int quantity = safeQuantity(dto.quantity());
    List<SnkVente> entities = java.util.stream.IntStream.range(0, quantity)
        .mapToObj(i -> buildEntity(user, dto))
        .collect(Collectors.toList());
    return snkVenteRepository.saveAll(entities);
  }

  private SnkVente buildEntity(User user, SnkVenteCreateDto dto) {
    SnkVente v = new SnkVente();
    v.setUser(user);
    applyFields(v, dto);
    return v;
  }

  private int safeQuantity(Integer quantity) {
    if (quantity == null) return 1;
    return Math.min(50, Math.max(1, quantity));
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
  public SnkVente lire(Long userId, Integer id) {
    return snkVenteRepository.findById(id)
        .filter(v -> v.getUser() != null && userId.equals(v.getUser().getId()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vente introuvable"));
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
  @CacheEvict(cacheNames = "statsQueries", allEntries = true)
  public void deleteVente(Long userId, Integer id) {
    SnkVente existing = snkVenteRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vente introuvable"));

    if (existing.getUser() == null || !userId.equals(existing.getUser().getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces interdit");
    }

    snkVenteRepository.delete(existing);
  }

  @Transactional
  @CacheEvict(cacheNames = "statsQueries", allEntries = true)
  public int deleteBulk(Long userId, List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return 0;
    return snkVenteRepository.deleteByUserAndIds(userId, ids);
  }

  public List<TopVenteProjection> getTop3VentesAnneeCourante(Long userId) {
    int currentYear = Year.now().getValue();
    return snkVenteRepository.topVentesYear(userId, currentYear).stream().limit(3).toList();
  }

  @Transactional
  @CacheEvict(cacheNames = "statsQueries", allEntries = true)
  public SnkVente updateVente(Long userId, Integer id, SnkVente payload) {
    SnkVente existing = snkVenteRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vente introuvable"));

    if (existing.getUser() == null || !userId.equals(existing.getUser().getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces interdit");
    }

    applyFields(existing, payload);
    return snkVenteRepository.save(existing);
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

  @CacheEvict(cacheNames = "statsQueries", allEntries = true)
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
    return entities.size();
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
      case "TICKET" -> key.matches("^(eventDate|venue|section|row|seat|status)$");
      case "POKEMON_CARD" -> key.matches("^(set|language|rarity|condition|grade)$");
      case "SNEAKER" -> key.matches("^(size|sku|colorway|condition|boxCondition)$");
      case "OTHER" -> key.matches("^(size|sku|colorway|condition|reference|model|supplier|purchasePlace)$");
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
