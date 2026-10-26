package com.jgmagalop.dto;

import java.math.BigDecimal;

public record TendenciaMes(String mes, BigDecimal ingresos, BigDecimal gastos, BigDecimal balance) {}