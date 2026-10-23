package com.jgmagalop.web;

import com.jgmagalop.domain.TipoMovimiento;
import com.jgmagalop.services.TransaccionService;

import io.swagger.v3.oas.annotations.Operation;

import com.jgmagalop.dto.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final TransaccionService service;

    public TransaccionController(TransaccionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<TransaccionResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(desde, hasta, tipo, categoriaId, q, page, size);
    }

    @GetMapping("/{id}")
    public TransaccionResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<TransaccionResponse> crear(@Valid @RequestBody TransaccionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public TransaccionResponse actualizar(@PathVariable Long id, @Valid @RequestBody TransaccionRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Exportar transacciones a CSV",
            description = "Acepta los mismos filtros que el listado, sin paginación (máximo 10,000 filas). Por defecto, el mes actual.")
 @GetMapping("/exportar")
 public ResponseEntity<byte[]> exportar(
         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
         @RequestParam(required = false) TipoMovimiento tipo,
         @RequestParam(required = false) Long categoriaId,
         @RequestParam(required = false) String q) {

     ArchivoCsv csv = service.exportarCsv(desde, hasta, tipo, categoriaId, q);

     return ResponseEntity.ok()
             .header(HttpHeaders.CONTENT_DISPOSITION,
                     ContentDisposition.attachment().filename(csv.nombre()).build().toString())
             .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
             .body(csv.contenido());
 }
}