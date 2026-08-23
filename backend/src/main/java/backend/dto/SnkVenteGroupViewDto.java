package backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record SnkVenteGroupViewDto(
    Integer id,
    Integer parentId,
    boolean groupParent,
    Integer quantity,
    Integer soldCount,
    String nomItem,
    String description,
    String categorie,
    String type,
    LocalDate dateAchat,
    LocalDate dateVente,
    BigDecimal prixRetail,
    BigDecimal prixResell,
    BigDecimal totalRetail,
    BigDecimal totalResell,
    BigDecimal totalProfit,
    Map<String, Object> metadata,
    List<SnkVenteChildViewDto> children
) {}
