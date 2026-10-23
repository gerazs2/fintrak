package com.jgmagalop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfirmarPendienteRequest(
        @Schema(example = "650") @Positive @Digits(integer = 10, fraction = 2) BigDecimal monto,
        @Schema(example = "2026-11-03") LocalDate fecha) {}