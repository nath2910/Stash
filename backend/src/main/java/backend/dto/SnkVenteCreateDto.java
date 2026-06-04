package backend.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
  @Size(max = 80) String type,
  Map<String, Object> metadata,
  @Min(1) @Max(50) Integer quantity
) {}
