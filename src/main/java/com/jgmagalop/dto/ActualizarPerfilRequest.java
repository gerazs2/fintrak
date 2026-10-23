package com.jgmagalop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record ActualizarPerfilRequest(
        @Schema(example = "Juan Pérez") @NotBlank @Size(max = 100) String nombre) {}