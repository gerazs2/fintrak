package com.jgmagalop.services;

import com.jgmagalop.domain.*;
import com.jgmagalop.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository repo;
    private final TransaccionRepository transaccionRepo;
    private final PresupuestoRepository presupuestoRepo;
    private final UsuarioActual usuarioActual;
    private final RecurrenteRepository recurrenteRepo;

    public CategoriaService( CategoriaRepository repo, 
    						 UsuarioActual usuarioActual, 
    						 TransaccionRepository transaccionRepo,
    						 PresupuestoRepository presupuestoRepo,
    						 RecurrenteRepository recurrenteRepo
    						) {
        this.repo = repo;
        this.usuarioActual = usuarioActual;
        this.transaccionRepo = transaccionRepo;
        this.presupuestoRepo = presupuestoRepo;
        this.recurrenteRepo = recurrenteRepo;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(TipoMovimiento tipo) {
        Long uid = usuarioActual.get().getId();
        List<Categoria> lista = (tipo == null)
                ? repo.findByUsuarioIdOrderByNombreAsc(uid)
                : repo.findByUsuarioIdAndTipoOrderByNombreAsc(uid, tipo);
        return lista.stream().map(CategoriaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.from(buscar(id, usuarioActual.get().getId()));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        Usuario u = usuarioActual.get();
        validarDuplicado(u.getId(), req);

        Categoria c = new Categoria();
        c.setNombre(req.nombre().trim());
        c.setTipo(req.tipo());
        c.setUsuario(u);
        return CategoriaResponse.from(repo.save(c));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest req) {
        Usuario u = usuarioActual.get();
        Categoria c = buscar(id, u.getId());

        boolean cambio = !c.getNombre().equalsIgnoreCase(req.nombre().trim()) || c.getTipo() != req.tipo();

        if (c.getTipo() != req.tipo() && enUso(c.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el tipo: la categoría tiene transacciones, presupuestos o recurrentes");
        }

        if (cambio) validarDuplicado(u.getId(), req);

        c.setNombre(req.nombre().trim());
        c.setTipo(req.tipo());
        return CategoriaResponse.from(repo.save(c));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria c = buscar(id, usuarioActual.get().getId());
        if (enUso(c.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: la categoría tiene transacciones, presupuestos o recurrentes");
        }
        repo.delete(c);
    }

    private Categoria buscar(Long id, Long uid) {
        return repo.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    private void validarDuplicado(Long uid, CategoriaRequest req) {
        if (repo.existsByUsuarioIdAndNombreIgnoreCaseAndTipo(uid, req.nombre().trim(), req.tipo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre y tipo");
        }
    }
    
    private boolean enUso(Long categoriaId) {
        return transaccionRepo.existsByCategoriaId(categoriaId)
                || presupuestoRepo.existsByCategoriaId(categoriaId)
                || recurrenteRepo.existsByCategoriaId(categoriaId);
    }
}