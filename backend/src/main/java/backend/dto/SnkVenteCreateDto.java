package backend.dto;


import backend.entity.ItemType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SnkVenteCreateDto(
  String nomItem,
  BigDecimal prixRetail,
  BigDecimal prixResell,
  LocalDate dateAchat,
  LocalDate dateVente,
  String description,
  String categorie,
  ItemType type,
  Map<String, Object> metadata
) {}
