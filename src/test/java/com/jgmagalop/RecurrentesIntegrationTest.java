package com.jgmagalop;

import com.jayway.jsonpath.JsonPath;
import com.jgmagalop.services.GeneradorRecurrentes;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecurrentesIntegrationTest extends ApiTestBase {

    private final LocalDate hoy = GeneradorRecurrentes.hoy();
    private final LocalDate inicio = hoy.minusMonths(2);   // genera 3 ocurrencias

    private Integer crearRecurrente(String token, Integer categoriaId, String modo) throws Exception {
        String body = postJson(token, "/api/recurrentes", """
                {"monto":500,"descripcion":"Gimnasio","categoriaId":%d,
                 "frecuencia":"MENSUAL","modo":"%s","fechaInicio":"%s"}
                """.formatted(categoriaId, modo, inicio))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generadas").value(3))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void verificarTransacciones(String token, Integer categoriaId, int esperadas) throws Exception {
        mvc.perform(get("/api/transacciones").header("Authorization", token)
                        .param("desde", inicio.toString())
                        .param("hasta", hoy.toString())
                        .param("categoriaId", categoriaId.toString()))
                .andExpect(jsonPath("$.totalElementos").value(esperadas));
    }

    @Test
    void modoAutomatico_creaTransaccionesDirectas() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Gym", "GASTO");
        crearRecurrente(token, cat, "AUTOMATICO");

        verificarTransacciones(token, cat, 3);
        mvc.perform(get("/api/recurrentes/pendientes").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void modoConfirmar_flujoCompleto() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Gym", "GASTO");
        crearRecurrente(token, cat, "CONFIRMAR");

        // No se registra nada automáticamente
        verificarTransacciones(token, cat, 0);

        String lista = mvc.perform(get("/api/recurrentes/pendientes").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        Integer p1 = JsonPath.read(lista, "$[0].id");
        Integer p2 = JsonPath.read(lista, "$[1].id");
        Integer p3 = JsonPath.read(lista, "$[2].id");

        // Confirmar sin body: usa monto y fecha del pendiente
        mvc.perform(post("/api/recurrentes/pendientes/" + p1 + "/confirmar").header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto").value(500.0))
                .andExpect(jsonPath("$.tipo").value("GASTO"));

        // Omitir
        mvc.perform(post("/api/recurrentes/pendientes/" + p2 + "/omitir").header("Authorization", token))
                .andExpect(status().isNoContent());

        // Confirmar ajustando el monto
        postJson(token, "/api/recurrentes/pendientes/" + p3 + "/confirmar", """
                {"monto":650}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto").value(650.0));

        mvc.perform(get("/api/recurrentes/pendientes").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
        verificarTransacciones(token, cat, 2);

        // Procesar de nuevo no regenera lo omitido ni lo confirmado
        mvc.perform(post("/api/recurrentes/procesar").header("Authorization", token))
                .andExpect(jsonPath("$.generadas").value(0));
    }

    @Test
    void noPuedeConfirmarPendienteDeOtroUsuario() throws Exception {
        String tokenA = nuevoUsuarioConToken();
        String tokenB = nuevoUsuarioConToken();
        Integer cat = crearCategoria(tokenA, "Gym", "GASTO");
        crearRecurrente(tokenA, cat, "CONFIRMAR");

        String lista = mvc.perform(get("/api/recurrentes/pendientes").header("Authorization", tokenA))
                .andReturn().getResponse().getContentAsString();
        Integer pendienteDeA = JsonPath.read(lista, "$[0].id");

        mvc.perform(post("/api/recurrentes/pendientes/" + pendienteDeA + "/confirmar")
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_conservaTransaccionesYBorraPendientes() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer catAuto = crearCategoria(token, "Renta", "GASTO");
        Integer catConf = crearCategoria(token, "Gym", "GASTO");
        Integer auto = crearRecurrente(token, catAuto, "AUTOMATICO");
        Integer conf = crearRecurrente(token, catConf, "CONFIRMAR");

        mvc.perform(delete("/api/recurrentes/" + auto).header("Authorization", token))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/recurrentes/" + conf).header("Authorization", token))
                .andExpect(status().isNoContent());

        verificarTransacciones(token, catAuto, 3);
        mvc.perform(get("/api/recurrentes/pendientes").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void categoriaUsadaPorRecurrente_noSePuedeEliminar() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Gym", "GASTO");
        crearRecurrente(token, cat, "CONFIRMAR");

        mvc.perform(delete("/api/categorias/" + cat).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    void fechaFinAnteriorAInicio_responde400() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Gym", "GASTO");

        postJson(token, "/api/recurrentes", """
                {"monto":500,"categoriaId":%d,"frecuencia":"MENSUAL","modo":"AUTOMATICO",
                 "fechaInicio":"%s","fechaFin":"%s"}
                """.formatted(cat, hoy, hoy.minusDays(1)))
                .andExpect(status().isBadRequest());
    }
}