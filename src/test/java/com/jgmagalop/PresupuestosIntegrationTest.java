package com.jgmagalop;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PresupuestosIntegrationTest extends ApiTestBase {

    private final String mes = YearMonth.now().toString();
    private final LocalDate hoy = LocalDate.now();

    private void crearPresupuesto(String token, Integer categoriaId, String monto) throws Exception {
        postJson(token, "/api/presupuestos", """
                {"categoriaId":%d,"mes":"%s","monto":%s}
                """.formatted(categoriaId, mes, monto))
                .andExpect(status().isCreated());
    }

    private void verificarEstado(String token, String porcentaje, String estado) throws Exception {
        mvc.perform(get("/api/presupuestos").header("Authorization", token).param("mes", mes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].porcentaje").value(Double.parseDouble(porcentaje)))
                .andExpect(jsonPath("$[0].estado").value(estado));
    }

    @Test
    void debajoDel80_estadoOK() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Comida", "GASTO");
        crearPresupuesto(token, cat, "1000");
        crearTransaccion(token, cat, "500", hoy, "Super");

        verificarEstado(token, "50.00", "OK");
    }

    @Test
    void entre80y100_estadoALERTA() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Comida", "GASTO");
        crearPresupuesto(token, cat, "1000");
        crearTransaccion(token, cat, "850", hoy, "Super");

        verificarEstado(token, "85.00", "ALERTA");
    }

    @Test
    void masDel100_estadoEXCEDIDO_yRestanteNegativo() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Comida", "GASTO");
        crearPresupuesto(token, cat, "1000");
        crearTransaccion(token, cat, "1200", hoy, "Super");

        verificarEstado(token, "120.00", "EXCEDIDO");
        mvc.perform(get("/api/presupuestos").header("Authorization", token).param("mes", mes))
                .andExpect(jsonPath("$[0].restante").value(-200.0));
    }

    @Test
    void categoriaDeIngreso_responde400() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer sueldo = crearCategoria(token, "Sueldo", "INGRESO");

        postJson(token, "/api/presupuestos", """
                {"categoriaId":%d,"mes":"%s","monto":1000}
                """.formatted(sueldo, mes))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicadoEnElMismoMes_responde409() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Comida", "GASTO");
        crearPresupuesto(token, cat, "1000");

        postJson(token, "/api/presupuestos", """
                {"categoriaId":%d,"mes":"%s","monto":2000}
                """.formatted(cat, mes))
                .andExpect(status().isConflict());
    }
}