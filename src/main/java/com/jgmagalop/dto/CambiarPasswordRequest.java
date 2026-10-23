package com.jgmagalop.dto;

import jakarta.validation.constraints.*;

public record CambiarPasswordRequest(
        @NotBlank String passwordActual,
        @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String passwordNueva) {}