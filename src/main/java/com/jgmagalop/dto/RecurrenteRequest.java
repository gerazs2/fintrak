package com.jgmagalop.dto;

import com.jgmagalop.domain.Frecuencia;
import com.jgmagalop.domain.ModoRecurrente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurrenteRequest(
        @Schema(example = "8000") @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monto,
        @Schema(example = "Renta") @Size(max = 255) String descripcion,
        @Schema(example = "1") @NotNull Long categoriaId,
        @Schema(example = "MENSUAL") @NotNull Frecuencia frecuencia,
        @Schema(example = "2026-11-01") @NotNull LocalDate fechaInicio,
        @Schema(example = "CONFIRMAR") @NotNull ModoRecurrente modo,
        @Schema(example = "2027-10-31") LocalDate fechaFin) {}