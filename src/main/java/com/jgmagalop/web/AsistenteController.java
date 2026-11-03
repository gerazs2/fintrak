package com.jgmagalop.web;

import com.jgmagalop.dto.AnalisisResponse;
import com.jgmagalop.services.AsistenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@Tag(name = "Asistente IA", description = "Análisis de tus finanzas generado por IA")
@RestController
@RequestMapping("/api/asistente")
public class AsistenteController {

    private final AsistenteService service;

    public AsistenteController(AsistenteService service) {
        this.service = service;
    }

    @Operation(summary = "Analizar un mes",
               description = "Genera un resumen con alertas, tendencia y recomendaciones. "
                           + "Por defecto, el mes actual. Tiene un límite diario por usuario.")
    @PostMapping("/analisis")
    public AnalisisResponse analizar(
            @Parameter(description = "Mes a analizar (yyyy-MM)", example = "2026-11")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.analizar(mes);
    }
}