package backend.dto;


import backend.entity.ItemType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;

public record SnkVenteCreateDto(
  @NotBlank @Size(max = 200) String nomItem,
  @PositiveOrZero BigDecimal prixRetail,
  @PositiveOrZero BigDecimal prixResell,
  LocalDate dateAchat,
  LocalDate dateVente,
  @Size(max = 500) String description,
  @Size(max = 60) String categorie,
  ItemType type,
  Map<String, Object> metadata
) {}
