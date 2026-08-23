package backend.dto;

import backend.entity.SnkVente;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public record SnkVenteChildViewDto(
    Integer id,
    Integer parentId,
    Integer unitIndex,
    String nomItem,
    BigDecimal prixRetail,
    BigDecimal prixResell,
    LocalDate dateAchat,
    LocalDate dateVente,
    String description,
    String categorie,
    String type,
    Map<String, Object> metadata
) {

  public static SnkVenteChildViewDto fromEntity(SnkVente entity) {
    return new SnkVenteChildViewDto(
        entity.getId(),
        entity.getParentId(),
        entity.getUnitIndex(),
        entity.getNomItem(),
        entity.getPrixRetail(),
        entity.getPrixResell(),
        entity.getDateAchat(),
        entity.getDateVente(),
        entity.getDescription(),
        entity.getCategorie(),
        entity.getType(),
        entity.getMetadata() == null ? new HashMap<>() : new HashMap<>(entity.getMetadata())
    );
  }
}
