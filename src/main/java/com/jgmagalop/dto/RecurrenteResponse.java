package com.jgmagalop.dto;

import com.jgmagalop.domain.Frecuencia;
import com.jgmagalop.domain.ModoRecurrente;
import com.jgmagalop.domain.Recurrente;
import com.jgmagalop.domain.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurrenteResponse(
        Long id, BigDecimal monto, String descripcion,
        Long categoriaId, String categoriaNombre, TipoMovimiento tipo,
        Frecuencia frecuencia, ModoRecurrente modo,
        LocalDate fechaInicio, LocalDate fechaFin,
        LocalDate proximaFecha, int generadas, boolean activa) {

    public static RecurrenteResponse from(Recurrente r) {
        return new RecurrenteResponse(
                r.getId(), r.getMonto(), r.getDescripcion(),
                r.getCategoria().getId(), r.getCategoria().getNombre(), r.getCategoria().getTipo(),
                r.getFrecuencia(), r.getModo(),
                r.getFechaInicio(), r.getFechaFin(),
                r.isActiva() ? r.getProximaFecha() : null,
                r.getGeneradas(), r.isActiva());
    }
}