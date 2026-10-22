package com.jgmagalop.web;

import com.jgmagalop.dto.*;
import com.jgmagalop.services.PresupuestoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {

    private final PresupuestoService service;

    public PresupuestoController(PresupuestoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PresupuestoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.listar(mes);
    }

    @GetMapping("/{id}")
    public PresupuestoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<PresupuestoResponse> crear(@Valid @RequestBody PresupuestoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PatchMapping("/{id}")
    public PresupuestoResponse actualizarMonto(@PathVariable Long id,
                                               @Valid @RequestBody PresupuestoMontoRequest req) {
        return service.actualizarMonto(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}