package com.jgmagalop.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/secure")
    public ResponseEntity<String> secureEndpoint() {
        return ResponseEntity.ok("✅ Accediste a un endpoint protegido con JWT");
    }
}
