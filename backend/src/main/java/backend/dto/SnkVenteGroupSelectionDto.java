package backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SnkVenteGroupSelectionDto(
    @Size(min = 2, max = 50)
    List<@NotNull Integer> ids
) {}
