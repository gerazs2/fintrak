package com.jgmagalop.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByCategoriaId(Long categoriaId);

    @Query("""
           SELECT t FROM Transaccion t
           JOIN FETCH t.categoria
           WHERE t.usuario.id = :uid
             AND t.fecha BETWEEN :desde AND :hasta
             AND (:tipo IS NULL OR t.tipo = :tipo)
             AND (:categoriaId IS NULL OR t.categoria.id = :categoriaId)
           ORDER BY t.fecha DESC, t.id DESC
           """)
    List<Transaccion> buscar(@Param("uid") Long uid,
                             @Param("desde") LocalDate desde,
                             @Param("hasta") LocalDate hasta,
                             @Param("tipo") TipoMovimiento tipo,
                             @Param("categoriaId") Long categoriaId);
}