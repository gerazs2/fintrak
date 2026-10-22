package com.jgmagalop.services;

import com.jgmagalop.domain.*;
import com.jgmagalop.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PresupuestoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal UMBRAL_ALERTA = BigDecimal.valueOf(80);

    private final PresupuestoRepository repo;
    private final CategoriaRepository categoriaRepo;
    private final TransaccionRepository transaccionRepo;
    private final UsuarioActual usuarioActual;

    public PresupuestoService(PresupuestoRepository repo,
                              CategoriaRepository categoriaRepo,
                              TransaccionRepository transaccionRepo,
                              UsuarioActual usuarioActual) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
        this.transaccionRepo = transaccionRepo;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listar(YearMonth mes) {
        if (mes == null) mes = YearMonth.now();
        Long uid = usuarioActual.get().getId();

        // Una sola consulta para el gasto de todas las categorías del mes
        Map<Long, BigDecimal> gastado = transaccionRepo
                .totalesPorCategoria(uid, TipoMovimiento.GASTO, mes.atDay(1), mes.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(TotalCategoria::categoriaId, TotalCategoria::total));

        return repo.findDelMes(uid, mes.atDay(1)).stream()
                .map(p -> armar(p, gastado.getOrDefault(p.getCategoria().getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PresupuestoResponse obtener(Long id) {
        Long uid = usuarioActual.get().getId();
        return conGasto(buscar(id, uid), uid);
    }

    @Transactional
    public PresupuestoResponse crear(PresupuestoRequest req) {
        Usuario u = usuarioActual.get();

        Categoria c = categoriaRepo.findByIdAndUsuarioId(req.categoriaId(), u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría inválida"));
        if (c.getTipo() != TipoMovimiento.GASTO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se pueden presupuestar categorías de tipo GASTO");
        }

        LocalDate periodo = req.mes().atDay(1);
        if (repo.existsByUsuarioIdAndCategoriaIdAndPeriodo(u.getId(), c.getId(), periodo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un presupuesto para esa categoría en ese mes");
        }

        Presupuesto p = new Presupuesto();
        p.setUsuario(u);
        p.setCategoria(c);
        p.setPeriodo(periodo);
        p.setMonto(req.monto());
        return conGasto(repo.save(p), u.getId());
    }

    @Transactional
    public PresupuestoResponse actualizarMonto(Long id, PresupuestoMontoRequest req) {
        Long uid = usuarioActual.get().getId();
        Presupuesto p = buscar(id, uid);
        p.setMonto(req.monto());
        return conGasto(repo.save(p), uid);
    }

    @Transactional
    public void eliminar(Long id) {
        repo.delete(buscar(id, usuarioActual.get().getId()));
    }

    // ---------- helpers ----------

    private Presupuesto buscar(Long id, Long uid) {
        return repo.findDelUsuario(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
    }

    private PresupuestoResponse conGasto(Presupuesto p, Long uid) {
        YearMonth mes = YearMonth.from(p.getPeriodo());
        BigDecimal gastado = transaccionRepo.sumarPorCategoria(
                uid, p.getCategoria().getId(), mes.atDay(1), mes.atEndOfMonth());
        return armar(p, gastado);
    }

    private PresupuestoResponse armar(Presupuesto p, BigDecimal gastado) {
        BigDecimal limite = p.getMonto();
        BigDecimal restante = limite.subtract(gastado);
        BigDecimal porcentaje = gastado.multiply(CIEN).divide(limite, 2, RoundingMode.HALF_UP);

        String estado;
        if (porcentaje.compareTo(CIEN) > 0) estado = "EXCEDIDO";
        else if (porcentaje.compareTo(UMBRAL_ALERTA) >= 0) estado = "ALERTA";
        else estado = "OK";

        return new PresupuestoResponse(
                p.getId(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                YearMonth.from(p.getPeriodo()).toString(),
                limite.setScale(2, RoundingMode.HALF_UP),
                gastado.setScale(2, RoundingMode.HALF_UP),
                restante.setScale(2, RoundingMode.HALF_UP),
                porcentaje,
                estado);
    }
}