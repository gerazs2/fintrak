package com.jgmagalop.services;

import com.jgmagalop.domain.*;
import com.jgmagalop.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class TransaccionService {

    private final TransaccionRepository repo;
    private final CategoriaRepository categoriaRepo;
    private final UsuarioActual usuarioActual;
    
    private static final int TAMANIO_MAX = 100;
    private static final int MAX_EXPORTAR = 10_000;

    public TransaccionService(TransaccionRepository repo,
                              CategoriaRepository categoriaRepo,
                              UsuarioActual usuarioActual) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransaccionResponse> listar(LocalDate desde, LocalDate hasta,
                                                    TipoMovimiento tipo, Long categoriaId,
                                                    String q, int page, int size) {
        YearMonth mes = YearMonth.now();
        if (desde == null) desde = mes.atDay(1);
        if (hasta == null) hasta = mes.atEndOfMonth();
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'desde' no puede ser mayor que 'hasta'");
        }
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'page' no puede ser negativo");
        }
        if (size < 1 || size > TAMANIO_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'size' debe estar entre 1 y " + TAMANIO_MAX);
        }

        // Búsqueda vacía o solo espacios = sin filtro
        String texto = (q == null || q.isBlank()) ? null : q.trim();

        Long uid = usuarioActual.get().getId();
        return PageResponse.from(
                repo.buscar(uid, desde, hasta, tipo, categoriaId, texto, PageRequest.of(page, size)),
                TransaccionResponse::from);
    }
    
    /*@Transactional(readOnly = true)
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
    }*/

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
    
    @Transactional(readOnly = true)
    public ArchivoCsv exportarCsv(LocalDate desde, LocalDate hasta,
                                  TipoMovimiento tipo, Long categoriaId, String q) {
        YearMonth mes = YearMonth.now();
        if (desde == null) desde = mes.atDay(1);
        if (hasta == null) hasta = mes.atEndOfMonth();
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'desde' no puede ser mayor que 'hasta'");
        }
        String texto = (q == null || q.isBlank()) ? null : q.trim();
        Long uid = usuarioActual.get().getId();

        Page<Transaccion> page = repo.buscar(uid, desde, hasta, tipo, categoriaId, texto,
                PageRequest.of(0, MAX_EXPORTAR));
        if (page.getTotalElements() > MAX_EXPORTAR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Demasiadas transacciones (" + page.getTotalElements() + "). Reduce el rango de fechas o usa filtros");
        }

        StringBuilder sb = new StringBuilder("\uFEFF");   // BOM: Excel reconoce UTF-8
        sb.append("Fecha,Tipo,Categoría,Descripción,Monto,Origen\r\n");
        for (Transaccion t : page.getContent()) {
            sb.append(t.getFecha()).append(',')
              .append(t.getTipo()).append(',')
              .append(celda(t.getCategoria().getNombre())).append(',')
              .append(celda(t.getDescripcion())).append(',')
              .append(t.getMonto().toPlainString()).append(',')
              .append(t.getRecurrente() != null ? "Recurrente" : "Manual")
              .append("\r\n");
        }

        String nombre = "fintrack_" + desde + "_" + hasta + ".csv";
        return new ArchivoCsv(nombre, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Escapa un valor de texto para CSV y evita que Excel lo interprete como fórmula. */
    private String celda(String valor) {
        if (valor == null) return "";
        String v = valor;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}