package com.jgmagalop.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByUsuarioIdOrderByNombreAsc(Long usuarioId);

    List<Categoria> findByUsuarioIdAndTipoOrderByNombreAsc(Long usuarioId, TipoMovimiento tipo);

    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNombreIgnoreCaseAndTipo(Long usuarioId, String nombre, TipoMovimiento tipo);
}