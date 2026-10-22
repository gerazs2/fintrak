package com.jgmagalop.services;

import com.jgmagalop.domain.TipoMovimiento;
import com.jgmagalop.domain.TransaccionRepository;
import com.jgmagalop.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class ResumenService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final TransaccionRepository repo;
    private final UsuarioActual usuarioActual;

    public ResumenService(TransaccionRepository repo, UsuarioActual usuarioActual) {
        this.repo = repo;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public ResumenResponse mensual(YearMonth mes) {
        if (mes == null) mes = YearMonth.now();
        LocalDate desde = mes.atDay(1);
        LocalDate hasta = mes.atEndOfMonth();
        Long uid = usuarioActual.get().getId();

        BigDecimal ingresos = repo.sumarPorTipo(uid, TipoMovimiento.INGRESO, desde, hasta);
        BigDecimal gastos   = repo.sumarPorTipo(uid, TipoMovimiento.GASTO, desde, hasta);

        List<CategoriaResumen> gastosCat = conPorcentaje(
                repo.totalesPorCategoria(uid, TipoMovimiento.GASTO, desde, hasta), gastos);
        List<CategoriaResumen> ingresosCat = conPorcentaje(
                repo.totalesPorCategoria(uid, TipoMovimiento.INGRESO, desde, hasta), ingresos);

        return new ResumenResponse(
                mes.toString(),
                ingresos.setScale(2, RoundingMode.HALF_UP),
                gastos.setScale(2, RoundingMode.HALF_UP),
                ingresos.subtract(gastos).setScale(2, RoundingMode.HALF_UP),
                gastosCat,
                ingresosCat);
    }

    private List<CategoriaResumen> conPorcentaje(List<TotalCategoria> totales, BigDecimal totalGeneral) {
        return totales.stream()
                .map(t -> new CategoriaResumen(
                        t.categoriaId(),
                        t.nombre(),
                        t.total().setScale(2, RoundingMode.HALF_UP),
                        totalGeneral.signum() == 0
                                ? BigDecimal.ZERO.setScale(2)
                                : t.total().multiply(CIEN).divide(totalGeneral, 2, RoundingMode.HALF_UP)))
                .toList();
    }
}