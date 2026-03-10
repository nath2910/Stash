package backend.dto;

import backend.entity.ItemType;
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
  @NotBlank
  @Size(max = 200)
  private String nomItem;

  @PositiveOrZero
  private BigDecimal prixRetail;

  @PositiveOrZero
  private BigDecimal prixResell;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dateAchat;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dateVente;

  @Size(max = 500)
  private String description;

  @Size(max = 60)
  private String categorie;

  private ItemType type;
  private Map<String, Object> metadata;

}
