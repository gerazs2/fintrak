package com.jgmagalop.web;

import com.jgmagalop.dto.*;
import com.jgmagalop.services.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Perfil", description = "Datos de la cuenta del usuario autenticado")
@RestController
@RequestMapping("/api/usuarios/me")
public class PerfilController {

    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @Operation(summary = "Ver mi perfil")
    @GetMapping
    public PerfilResponse obtener() {
        return service.obtener();
    }

    @Operation(summary = "Actualizar mi nombre")
    @PatchMapping
    public PerfilResponse actualizar(@Valid @RequestBody ActualizarPerfilRequest req) {
        return service.actualizar(req);
    }

    @Operation(summary = "Cambiar mi contraseña",
            description = "Invalida todos los tokens anteriores y devuelve uno nuevo.")
	 @PutMapping("/password")
	 public AuthResponse cambiarPassword(@Valid @RequestBody CambiarPasswordRequest req) {
	     return service.cambiarPassword(req);
	 }
}