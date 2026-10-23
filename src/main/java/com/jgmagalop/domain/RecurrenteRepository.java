package com.jgmagalop.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurrenteRepository extends JpaRepository<Recurrente, Long> {

    @Query("""
           SELECT r FROM Recurrente r JOIN FETCH r.categoria
           WHERE r.usuario.id = :uid
           ORDER BY r.activa DESC, r.proximaFecha ASC
           """)
    List<Recurrente> findTodasDelUsuario(@Param("uid") Long uid);

    @Query("""
           SELECT r FROM Recurrente r JOIN FETCH r.categoria
           WHERE r.id = :id AND r.usuario.id = :uid
           """)
    Optional<Recurrente> findDelUsuario(@Param("id") Long id, @Param("uid") Long uid);

    @Query("""
           SELECT r FROM Recurrente r JOIN FETCH r.categoria
           WHERE r.activa = true AND r.proximaFecha <= :hoy
           """)
    List<Recurrente> findPendientes(@Param("hoy") LocalDate hoy);

    @Query("""
           SELECT r FROM Recurrente r JOIN FETCH r.categoria
           WHERE r.activa = true AND r.proximaFecha <= :hoy AND r.usuario.id = :uid
           """)
    List<Recurrente> findPendientesDelUsuario(@Param("uid") Long uid, @Param("hoy") LocalDate hoy);

    boolean existsByCategoriaId(Long categoriaId);
}