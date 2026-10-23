package com.jgmagalop.domain;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jgmagalop.dto.TotalCategoria;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByCategoriaId(Long categoriaId);

   /* @Query("""
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
                             @Param("categoriaId") Long categoriaId);*/
    

	@Query(value = """
	       SELECT t FROM Transaccion t
	       JOIN FETCH t.categoria
	       WHERE t.usuario.id = :uid
	         AND t.fecha BETWEEN :desde AND :hasta
	         AND (:tipo IS NULL OR t.tipo = :tipo)
	         AND (:categoriaId IS NULL OR t.categoria.id = :categoriaId)
	         AND (:q IS NULL OR LOWER(t.descripcion) LIKE LOWER(CONCAT('%', :q, '%')))
	       ORDER BY t.fecha DESC, t.id DESC
	       """,
	       countQuery = """
	       SELECT COUNT(t) FROM Transaccion t
	       WHERE t.usuario.id = :uid
	         AND t.fecha BETWEEN :desde AND :hasta
	         AND (:tipo IS NULL OR t.tipo = :tipo)
	         AND (:categoriaId IS NULL OR t.categoria.id = :categoriaId)
	         AND (:q IS NULL OR LOWER(t.descripcion) LIKE LOWER(CONCAT('%', :q, '%')))
	       """)
	Page<Transaccion> buscar(@Param("uid") Long uid,
	                         @Param("desde") LocalDate desde,
	                         @Param("hasta") LocalDate hasta,
	                         @Param("tipo") TipoMovimiento tipo,
	                         @Param("categoriaId") Long categoriaId,
	                         @Param("q") String q,
	                         Pageable pageable);
    
    @Query("""
    	       SELECT COALESCE(SUM(t.monto), 0) FROM Transaccion t
    	       WHERE t.usuario.id = :uid
    	         AND t.tipo = :tipo
    	         AND t.fecha BETWEEN :desde AND :hasta
    	       """)
    	BigDecimal sumarPorTipo(@Param("uid") Long uid,
    	                        @Param("tipo") TipoMovimiento tipo,
    	                        @Param("desde") LocalDate desde,
    	                        @Param("hasta") LocalDate hasta);

    	@Query("""
    	       SELECT new com.jgmagalop.dto.TotalCategoria(c.id, c.nombre, SUM(t.monto))
    	       FROM Transaccion t JOIN t.categoria c
    	       WHERE t.usuario.id = :uid
    	         AND t.tipo = :tipo
    	         AND t.fecha BETWEEN :desde AND :hasta
    	       GROUP BY c.id, c.nombre
    	       ORDER BY SUM(t.monto) DESC
    	       """)
    	List<TotalCategoria> totalesPorCategoria(@Param("uid") Long uid,
    	                                         @Param("tipo") TipoMovimiento tipo,
    	                                         @Param("desde") LocalDate desde,
    	                                         @Param("hasta") LocalDate hasta);
    	
    	@Query("""
    		       SELECT COALESCE(SUM(t.monto), 0) FROM Transaccion t
    		       WHERE t.usuario.id = :uid
    		         AND t.categoria.id = :categoriaId
    		         AND t.fecha BETWEEN :desde AND :hasta
    		       """)
    		BigDecimal sumarPorCategoria(@Param("uid") Long uid,
    		                             @Param("categoriaId") Long categoriaId,
    		                             @Param("desde") LocalDate desde,
    		                             @Param("hasta") LocalDate hasta);
}