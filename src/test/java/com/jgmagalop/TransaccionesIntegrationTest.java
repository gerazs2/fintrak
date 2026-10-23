package com.jgmagalop;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransaccionesIntegrationTest extends ApiTestBase {

    private final LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);

    @Test
    void paginacion_tresPaginas() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Comida", "GASTO");
        for (int i = 1; i <= 25; i++) {
            crearTransaccion(token, cat, "10", inicioMes, "Gasto " + i);
        }

        mvc.perform(get("/api/transacciones").header("Authorization", token)
                        .param("size", "10").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(5))
                .andExpect(jsonPath("$.totalElementos").value(25))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    void ordenadas_masRecientePrimero() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Comida", "GASTO");
        crearTransaccion(token, cat, "10", inicioMes, "Vieja");
        crearTransaccion(token, cat, "10", inicioMes.plusDays(1), "Nueva");

        mvc.perform(get("/api/transacciones").header("Authorization", token)
                        .param("desde", inicioMes.toString())
                        .param("hasta", inicioMes.plusDays(1).toString()))
                .andExpect(jsonPath("$.contenido[0].descripcion").value("Nueva"));
    }

    @Test
    void busquedaPorTexto_sinImportarMayusculas() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Transporte", "GASTO");
        crearTransaccion(token, cat, "80", inicioMes, "Uber al trabajo");
        crearTransaccion(token, cat, "90", inicioMes, "UBER regreso");
        crearTransaccion(token, cat, "30", inicioMes, "Metro");

        mvc.perform(get("/api/transacciones").header("Authorization", token).param("q", "uber"))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    void filtroPorTipo() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer gasto = crearCategoria(token, "Comida", "GASTO");
        Integer ingreso = crearCategoria(token, "Sueldo", "INGRESO");
        crearTransaccion(token, gasto, "100", inicioMes, "Super");
        crearTransaccion(token, ingreso, "5000", inicioMes, "Quincena");

        mvc.perform(get("/api/transacciones").header("Authorization", token).param("tipo", "INGRESO"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].descripcion").value("Quincena"));
    }

    @Test
    void parametrosDePaginaInvalidos_responden400() throws Exception {
        String token = nuevoUsuarioConToken();

        mvc.perform(get("/api/transacciones").header("Authorization", token).param("size", "500"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/transacciones").header("Authorization", token).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/transacciones").header("Authorization", token).param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void montoConMasDeDosDecimales_responde400() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer cat = crearCategoria(token, "Comida", "GASTO");

        postJson(token, "/api/transacciones", """
                {"monto":10.555,"fecha":"%s","categoriaId":%d}
                """.formatted(inicioMes, cat))
                .andExpect(status().isBadRequest());
    }
}