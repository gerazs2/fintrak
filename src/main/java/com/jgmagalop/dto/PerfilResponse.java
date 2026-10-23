package com.jgmagalop.dto;

import com.jgmagalop.domain.Usuario;

public record PerfilResponse(Long id, String email, String nombre) {

    public static PerfilResponse from(Usuario u) {
        return new PerfilResponse(u.getId(), u.getEmail(), u.getNombre());
    }
}