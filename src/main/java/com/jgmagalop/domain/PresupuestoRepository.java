package com.jgmagalop.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    @Query("""
           SELECT p FROM Presupuesto p
           JOIN FETCH p.categoria c
           WHERE p.usuario.id = :uid AND p.periodo = :periodo
           ORDER BY c.nombre ASC
           """)
    List<Presupuesto> findDelMes(@Param("uid") Long uid, @Param("periodo") LocalDate periodo);

    @Query("""
           SELECT p FROM Presupuesto p
           JOIN FETCH p.categoria
           WHERE p.id = :id AND p.usuario.id = :uid
           """)
    Optional<Presupuesto> findDelUsuario(@Param("id") Long id, @Param("uid") Long uid);

    boolean existsByUsuarioIdAndCategoriaIdAndPeriodo(Long usuarioId, Long categoriaId, LocalDate periodo);

    boolean existsByCategoriaId(Long categoriaId);
}