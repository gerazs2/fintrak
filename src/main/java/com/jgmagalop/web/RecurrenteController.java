package com.jgmagalop.web;

import com.jgmagalop.dto.*;
import com.jgmagalop.services.RecurrenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Recurrentes", description = "Plantillas de ingresos y gastos que se repiten")
@RestController
@RequestMapping("/api/recurrentes")
public class RecurrenteController {

    private final RecurrenteService service;

    public RecurrenteController(RecurrenteService service) {
        this.service = service;
    }

    @Operation(summary = "Listar mis recurrentes")
    @GetMapping
    public List<RecurrenteResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Obtener una recurrente")
    @GetMapping("/{id}")
    public RecurrenteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear recurrente",
               description = "Si la fecha de inicio ya pasó, genera de inmediato las transacciones atrasadas.")
    @PostMapping
    public ResponseEntity<RecurrenteResponse> crear(@Valid @RequestBody RecurrenteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @Operation(summary = "Editar, pausar o reactivar una recurrente")
    @PutMapping("/{id}")
    public RecurrenteResponse actualizar(@PathVariable Long id, @Valid @RequestBody RecurrenteUpdateRequest req) {
        return service.actualizar(id, req);
    }

    @Operation(summary = "Eliminar recurrente", description = "Las transacciones ya generadas se conservan.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Generar ahora mis transacciones pendientes")
    @PostMapping("/procesar")
    public Map<String, Integer> procesar() {
        return Map.of("generadas", service.procesarMias());
    }
    
    @Operation(summary = "Ver pagos pendientes de confirmar")
    @GetMapping("/pendientes")
    public List<OcurrenciaPendienteResponse> pendientes() {
        return service.listarPendientes();
    }

    @Operation(summary = "Confirmar un pendiente",
               description = "Lo registra como transacción. Puedes ajustar el monto o la fecha; si no envías body, se usan los del pendiente.")
    @PostMapping("/pendientes/{id}/confirmar")
    public ResponseEntity<TransaccionResponse> confirmar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ConfirmarPendienteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.confirmar(id, req));
    }

    @Operation(summary = "Omitir un pendiente", description = "Ese periodo no se registra.")
    @PostMapping("/pendientes/{id}/omitir")
    public ResponseEntity<Void> omitir(@PathVariable Long id) {
        service.omitir(id);
        return ResponseEntity.noContent().build();
    }
}