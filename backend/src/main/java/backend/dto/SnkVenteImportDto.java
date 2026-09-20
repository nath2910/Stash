package backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnkVenteImportDto {
  @NotBlank(message = "nom de l'item manquant")
  @Size(max = 200, message = "nom de l'item trop long (200 caractères maximum)")
  private String nomItem;

  @PositiveOrZero(message = "prix d'achat négatif")
  private BigDecimal prixRetail;

  @PositiveOrZero(message = "prix de vente négatif")
  private BigDecimal prixResell;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dateAchat;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dateVente;

  @Size(max = 500, message = "description trop longue (500 caractères maximum)")
  private String description;

  @Size(max = 60, message = "catégorie trop longue (60 caractères maximum)")
  private String categorie;

  @Size(max = 80, message = "type trop long (80 caractères maximum)")
  private String type;
  private Map<String, Object> metadata;

}
