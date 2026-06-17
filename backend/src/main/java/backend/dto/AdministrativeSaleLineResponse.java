package backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdministrativeSaleLineResponse(
    Integer id,
    String reference,
    String itemName,
    LocalDate saleDate,
    LocalDate cashInDate,
    BigDecimal saleAmount,
    BigDecimal purchaseAmount,
    String category,
    String type,
    String paymentMethod
) {}
