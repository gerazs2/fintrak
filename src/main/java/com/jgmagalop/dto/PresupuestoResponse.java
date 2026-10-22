package com.jgmagalop.dto;

import java.math.BigDecimal;

public record PresupuestoResponse(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        String mes,
        BigDecimal limite,
        BigDecimal gastado,
        BigDecimal restante,
        BigDecimal porcentaje,
        String estado) {}