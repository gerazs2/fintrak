package com.jgmagalop.web;

import com.jgmagalop.dto.ResumenResponse;
import com.jgmagalop.dto.TendenciaMes;
import com.jgmagalop.services.ResumenService;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/resumen")
public class ResumenController {

    private final ResumenService service;

    public ResumenController(ResumenService service) {
        this.service = service;
    }

    @GetMapping
    public ResumenResponse mensual(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.mensual(mes);
    }
    
    @Operation(summary = "Tendencia mensual",
            description = "Ingresos, gastos y balance de los últimos N meses (1 a 24), del más antiguo al actual.")
	 @GetMapping("/tendencia")
	 public List<TendenciaMes> tendencia(@RequestParam(defaultValue = "6") int meses) {
	     return service.tendencia(meses);
	 }
}