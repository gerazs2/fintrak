package com.jgmagalop.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monto,
        @NotNull LocalDate fecha,
        @Size(max = 255) String descripcion,
        @NotNull Long categoriaId) {}