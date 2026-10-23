package com.jgmagalop.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OcurrenciaPendienteRepository extends JpaRepository<OcurrenciaPendiente, Long> {

    @Query("""
           SELECT o FROM OcurrenciaPendiente o
           JOIN FETCH o.recurrente r JOIN FETCH r.categoria
           WHERE o.usuario.id = :uid
           ORDER BY o.fecha ASC
           """)
    List<OcurrenciaPendiente> findDelUsuario(@Param("uid") Long uid);

    @Query("""
           SELECT o FROM OcurrenciaPendiente o
           JOIN FETCH o.recurrente r JOIN FETCH r.categoria
           WHERE o.id = :id AND o.usuario.id = :uid
           """)
    Optional<OcurrenciaPendiente> findDelUsuario(@Param("id") Long id, @Param("uid") Long uid);

    @Modifying
    @Query("DELETE FROM OcurrenciaPendiente o WHERE o.recurrente.id = :rid")
    int borrarDeRecurrente(@Param("rid") Long rid);
}