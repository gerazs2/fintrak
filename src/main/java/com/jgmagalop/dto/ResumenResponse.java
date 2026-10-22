package com.jgmagalop.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenResponse(
        String mes,
        BigDecimal ingresos,
        BigDecimal gastos,
        BigDecimal balance,
        List<CategoriaResumen> gastosPorCategoria,
        List<CategoriaResumen> ingresosPorCategoria) {}