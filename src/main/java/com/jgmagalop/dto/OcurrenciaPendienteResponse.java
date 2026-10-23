package com.jgmagalop.dto;

import com.jgmagalop.domain.OcurrenciaPendiente;
import com.jgmagalop.domain.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OcurrenciaPendienteResponse(
        Long id, Long recurrenteId, String descripcion,
        String categoriaNombre, TipoMovimiento tipo,
        LocalDate fecha, BigDecimal monto) {

    public static OcurrenciaPendienteResponse from(OcurrenciaPendiente o) {
        var r = o.getRecurrente();
        return new OcurrenciaPendienteResponse(
                o.getId(), r.getId(), r.getDescripcion(),
                r.getCategoria().getNombre(), r.getCategoria().getTipo(),
                o.getFecha(), o.getMonto());
    }
}