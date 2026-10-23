package com.jgmagalop.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.jgmagalop.domain.ModoRecurrente;

public record RecurrenteUpdateRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monto,
        @Size(max = 255) String descripcion,
        @NotNull Long categoriaId,
        LocalDate fechaFin,
        @NotNull ModoRecurrente modo,
        @NotNull Boolean activa) {}