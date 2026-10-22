package com.jgmagalop.services;

import com.jgmagalop.domain.*;
import com.jgmagalop.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class TransaccionService {

    private final TransaccionRepository repo;
    private final CategoriaRepository categoriaRepo;
    private final UsuarioActual usuarioActual;

    public TransaccionService(TransaccionRepository repo,
                              CategoriaRepository categoriaRepo,
                              UsuarioActual usuarioActual) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<TransaccionResponse> listar(LocalDate desde, LocalDate hasta,
                                            TipoMovimiento tipo, Long categoriaId) {
        // Por defecto: el mes actual
        YearMonth mes = YearMonth.now();
        if (desde == null) desde = mes.atDay(1);
        if (hasta == null) hasta = mes.atEndOfMonth();
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'desde' no puede ser mayor que 'hasta'");
        }

        Long uid = usuarioActual.get().getId();
        return repo.buscar(uid, desde, hasta, tipo, categoriaId)
                .stream().map(TransaccionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransaccionResponse obtener(Long id) {
        return TransaccionResponse.from(buscar(id, usuarioActual.get().getId()));
    }

    @Transactional
    public TransaccionResponse crear(TransaccionRequest req) {
        Usuario u = usuarioActual.get();
        Transaccion t = new Transaccion();
        t.setUsuario(u);
        aplicar(t, req, u.getId());
        return TransaccionResponse.from(repo.save(t));
    }

    @Transactional
    public TransaccionResponse actualizar(Long id, TransaccionRequest req) {
        Long uid = usuarioActual.get().getId();
        Transaccion t = buscar(id, uid);
        aplicar(t, req, uid);
        return TransaccionResponse.from(repo.save(t));
    }

    @Transactional
    public void eliminar(Long id) {
        repo.delete(buscar(id, usuarioActual.get().getId()));
    }

    private Transaccion buscar(Long id, Long uid) {
        return repo.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transacción no encontrada"));
    }

    private void aplicar(Transaccion t, TransaccionRequest req, Long uid) {
        Categoria c = categoriaRepo.findByIdAndUsuarioId(req.categoriaId(), uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría inválida"));

        t.setMonto(req.monto());
        t.setFecha(req.fecha());
        t.setDescripcion(req.descripcion() == null ? null : req.descripcion().trim());
        t.setCategoria(c);
        t.setTipo(c.getTipo());   // el tipo viene de la categoría
    }
}