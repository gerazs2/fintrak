package com.jgmagalop.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.YearMonth;

public record PresupuestoRequest(
        @NotNull Long categoriaId,
        @NotNull YearMonth mes,                       // JSON: "2026-10"
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monto) {}