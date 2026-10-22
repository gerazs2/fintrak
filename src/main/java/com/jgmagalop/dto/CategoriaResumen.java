package com.jgmagalop.dto;

import java.math.BigDecimal;;

public record CategoriaResumen(Long categoriaId, String nombre, BigDecimal total, BigDecimal porcentaje) {

}
