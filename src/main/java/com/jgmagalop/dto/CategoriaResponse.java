package com.jgmagalop.dto;


import com.jgmagalop.domain.Categoria;
import com.jgmagalop.domain.TipoMovimiento;

public record CategoriaResponse(Long id, String nombre, TipoMovimiento tipo) {

    public static CategoriaResponse from(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getTipo());
    }
}