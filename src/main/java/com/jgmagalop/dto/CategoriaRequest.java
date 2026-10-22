package com.jgmagalop.dto;

import com.jgmagalop.domain.TipoMovimiento;
import jakarta.validation.constraints.*;

public record CategoriaRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotNull TipoMovimiento tipo) {}