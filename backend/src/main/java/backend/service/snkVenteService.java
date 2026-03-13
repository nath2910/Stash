package backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import backend.dto.SnkVenteCreateDto;
import backend.dto.SnkVenteImportDto;
import backend.dto.TopVenteProjection;
import backend.entity.ItemType;
import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.SnkVenteRepository;
import backend.repository.SnkVenteRepository.BrandCount;
import backend.repository.UserRepository;

@Service
public class snkVenteService {

  private static final int MAX_IMPORT_ITEMS = 500;

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

  public SnkVente creer(Long userId, SnkVenteCreateDto dto) {
    User user = getUserOrThrow(userId);

    SnkVente v = new SnkVente();
    v.setUser(user);
    applyFields(v, dto);

    return snkVenteRepository.save(v);
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
  public void deleteVente(Long userId, Integer id) {
    SnkVente existing = snkVenteRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vente introuvable"));

    if (existing.getUser() == null || !userId.equals(existing.getUser().getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces interdit");
    }

    snkVenteRepository.delete(existing);
  }

  @Transactional
  public int deleteBulk(Long userId, List<Integer> ids) {
    if (ids == null || ids.isEmpty()) return 0;
    return snkVenteRepository.deleteByUserAndIds(userId, ids);
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
    target.setType(target.getType() != null ? target.getType() : ItemType.SNEAKER);
  }

  private void applyFields(SnkVente target, SnkVenteCreateDto dto) {
    ItemType resolvedType = dto.type() != null ? dto.type() : ItemType.SNEAKER;
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
    ItemType resolvedType = payload.getType() != null ? payload.getType() : ItemType.SNEAKER;
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
    ItemType resolvedType = dto.getType() != null ? dto.getType() : ItemType.SNEAKER;
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

  private Map<String, Object> sanitizeMetadata(ItemType type, Map<String, Object> metadata) {
    if (metadata == null) return new HashMap<>();
    Map<String, Object> cleaned = new HashMap<>();
    metadata.forEach((k, v) -> {
      if (k == null || v == null) return;
      String key = k.trim();
      if (key.isEmpty()) return;
      Object val = (v instanceof String s) ? s.trim() : v;
      switch (type) {
        case TICKET -> {
          if (key.matches("^(eventDate|venue|section|row|seat|status)$")) cleaned.put(key, val);
        }
        case POKEMON_CARD -> {
          if (key.matches("^(set|language|rarity|condition|grade)$")) cleaned.put(key, val);
        }
        case SNEAKER -> {
          if (key.matches("^(size|sku|colorway|condition|boxCondition)$")) cleaned.put(key, val);
        }
        case OTHER -> {
          // pas de champs imposés pour OTHER : tout est ignoré pour éviter du bruit côté BDD
        }
        default -> { }
      }
    });
    return cleaned;
  }
}
