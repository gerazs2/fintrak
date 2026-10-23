package com.jgmagalop.services;

import com.jgmagalop.domain.*;
import com.jgmagalop.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecurrenteService {

    private final RecurrenteRepository repo;
    private final CategoriaRepository categoriaRepo;
    private final TransaccionRepository transaccionRepo;
    private final GeneradorRecurrentes generador;
    private final UsuarioActual usuarioActual;
    private final OcurrenciaPendienteRepository pendienteRepo;

    public RecurrenteService( RecurrenteRepository repo, 
    						  CategoriaRepository categoriaRepo,
                              TransaccionRepository transaccionRepo, 
                              GeneradorRecurrentes generador,
                              UsuarioActual usuarioActual,
                              OcurrenciaPendienteRepository pendienteRepo) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
        this.transaccionRepo = transaccionRepo;
        this.generador = generador;
        this.usuarioActual = usuarioActual;
        this.pendienteRepo = pendienteRepo;
    }

    // ---------- plantillas ----------

    @Transactional(readOnly = true)
    public List<RecurrenteResponse> listar() {
        return repo.findTodasDelUsuario(usuarioActual.get().getId())
                .stream().map(RecurrenteResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RecurrenteResponse obtener(Long id) {
        return RecurrenteResponse.from(buscar(id, usuarioActual.get().getId()));
    }

    @Transactional
    public RecurrenteResponse crear(RecurrenteRequest req) {
        Usuario u = usuarioActual.get();
        LocalDate hoy = GeneradorRecurrentes.hoy();

        if (req.fechaInicio().isBefore(hoy.minusYears(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de inicio no puede ser de hace más de un año");
        }
        validarFechaFin(req.fechaInicio(), req.fechaFin());

        Recurrente r = new Recurrente();
        r.setUsuario(u);
        r.setCategoria(categoria(req.categoriaId(), u.getId()));
        r.setMonto(req.monto());
        r.setDescripcion(limpiar(req.descripcion()));
        r.setFrecuencia(req.frecuencia());
        r.setFechaInicio(req.fechaInicio());
        r.setFechaFin(req.fechaFin());
        r.setGeneradas(0);
        r.setProximaFecha(req.fechaInicio());
        r.setModo(req.modo());
        r.setActiva(true);
        repo.save(r);

        // Si la fecha de inicio ya pasó o es hoy, genera de inmediato lo que corresponda
        generador.generar(r, hoy);
        return RecurrenteResponse.from(r);
    }

    @Transactional
    public RecurrenteResponse actualizar(Long id, RecurrenteUpdateRequest req) {
        Long uid = usuarioActual.get().getId();
        Recurrente r = buscar(id, uid);
        LocalDate hoy = GeneradorRecurrentes.hoy();

        validarFechaFin(r.getFechaInicio(), req.fechaFin());

        r.setMonto(req.monto());
        r.setDescripcion(limpiar(req.descripcion()));
        r.setCategoria(categoria(req.categoriaId(), uid));
        r.setModo(req.modo());
        r.setFechaFin(req.fechaFin());

        boolean reactivando = !r.isActiva() && req.activa();
        r.setActiva(req.activa());

        if (reactivando) {
            // Las ocurrencias que cayeron mientras estuvo pausada se saltan, no se generan
            while (r.fechaDeOcurrencia(r.getGeneradas()).isBefore(hoy)) {
                r.setGeneradas(r.getGeneradas() + 1);
            }
        }
        generador.generar(r, hoy);   // recalcula proximaFecha y genera la de hoy si toca
        return RecurrenteResponse.from(r);
    }

    @Transactional
    public void eliminar(Long id) {
        Recurrente r = buscar(id, usuarioActual.get().getId());
        pendienteRepo.borrarDeRecurrente(r.getId());          // NUEVO: sus pendientes se eliminan
        transaccionRepo.desvincularDeRecurrente(r.getId());   // las transacciones se conservan
        repo.delete(r);
    }

    @Transactional
    public int procesarMias() {
        return generador.procesarPendientesDelUsuario(usuarioActual.get().getId(), GeneradorRecurrentes.hoy());
    }

    // ---------- pendientes (modo CONFIRMAR) ----------

    @Transactional(readOnly = true)
    public List<OcurrenciaPendienteResponse> listarPendientes() {
        return pendienteRepo.findDelUsuario(usuarioActual.get().getId())
                .stream().map(OcurrenciaPendienteResponse::from).toList();
    }

    @Transactional
    public TransaccionResponse confirmar(Long pendienteId, ConfirmarPendienteRequest req) {
        Long uid = usuarioActual.get().getId();
        OcurrenciaPendiente o = buscarPendiente(pendienteId, uid);
        Recurrente r = o.getRecurrente();

        Transaccion t = new Transaccion();
        t.setUsuario(o.getUsuario());
        t.setCategoria(r.getCategoria());
        t.setTipo(r.getCategoria().getTipo());
        t.setMonto(req != null && req.monto() != null ? req.monto() : o.getMonto());
        t.setFecha(req != null && req.fecha() != null ? req.fecha() : o.getFecha());
        t.setDescripcion(r.getDescripcion());
        t.setRecurrente(r);
        transaccionRepo.save(t);

        pendienteRepo.delete(o);
        return TransaccionResponse.from(t);
    }

    @Transactional
    public void omitir(Long pendienteId) {
        pendienteRepo.delete(buscarPendiente(pendienteId, usuarioActual.get().getId()));
    }

    // ---------- helpers ----------

    private Recurrente buscar(Long id, Long uid) {
        return repo.findDelUsuario(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurrente no encontrada"));
    }

    private OcurrenciaPendiente buscarPendiente(Long id, Long uid) {
        return pendienteRepo.findDelUsuario(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pendiente no encontrado"));
    }

    private Categoria categoria(Long categoriaId, Long uid) {
        return categoriaRepo.findByIdAndUsuarioId(categoriaId, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría inválida"));
    }

    private void validarFechaFin(LocalDate inicio, LocalDate fin) {
        if (fin != null && fin.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de fin no puede ser anterior a la de inicio");
        }
    }

    private String limpiar(String s) {
        return s == null ? null : s.trim();
    }
}