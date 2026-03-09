package backend.dto;

import backend.entity.ItemType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnkVenteImportDto {
  private String nomItem;
  private BigDecimal prixRetail;
  private BigDecimal prixResell;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dateAchat;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dateVente;

  private String description;
  private String categorie;
  private ItemType type;
  private Map<String, Object> metadata;

}
