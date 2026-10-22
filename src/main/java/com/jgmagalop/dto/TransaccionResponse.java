package com.jgmagalop.dto;

import com.jgmagalop.domain.TipoMovimiento;
import com.jgmagalop.domain.Transaccion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionResponse(
        Long id,
        BigDecimal monto,
        TipoMovimiento tipo,
        LocalDate fecha,
        String descripcion,
        Long categoriaId,
        String categoriaNombre) {

    public static TransaccionResponse from(Transaccion t) {
        return new TransaccionResponse(
                t.getId(), t.getMonto(), t.getTipo(), t.getFecha(), t.getDescripcion(),
                t.getCategoria().getId(), t.getCategoria().getNombre());
    }
}