package com.jgmagalop.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PresupuestoMontoRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monto) {}