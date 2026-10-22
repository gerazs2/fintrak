package com.jgmagalop.web;

import com.jgmagalop.dto.ResumenResponse;
import com.jgmagalop.services.ResumenService;
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
}