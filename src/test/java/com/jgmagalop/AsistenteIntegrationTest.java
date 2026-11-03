package com.jgmagalop;

import com.jgmagalop.services.ia.AsistenteIA;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AsistenteIntegrationTest extends ApiTestBase {

    // Reemplaza al ClienteOpenAICompatible real: en los tests nunca se llama a Groq
    @MockitoBean
    AsistenteIA ia;

    private final LocalDate hoy = LocalDate.now();

    /** Crea un usuario con un ingreso y un gasto en el mes actual. */
    private String usuarioConDatos() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer comida = crearCategoria(token, "Comida", "GASTO");
        Integer sueldo = crearCategoria(token, "Sueldo", "INGRESO");
        crearTransaccion(token, sueldo, "1000", hoy, "Nomina");
        crearTransaccion(token, comida, "300", hoy, "Super secreto");
        return token;
    }

    @Test
    void analisis_enviaSoloDatosAgregados_yDevuelveTexto() throws Exception {
        String token = usuarioConDatos();
        when(ia.completar(anyString(), anyString())).thenReturn("Resumen: vas bien.");

        mvc.perform(post("/api/asistente/analisis").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analisis").value("Resumen: vas bien."))
                .andExpect(jsonPath("$.restantesHoy").value(2));   // límite en tests = 3

        // Revisamos QUÉ se le envió a la IA
        ArgumentCaptor<String> datos = ArgumentCaptor.forClass(String.class);
        verify(ia).completar(anyString(), datos.capture());
        String enviado = datos.getValue();

        assertTrue(enviado.contains("Ingresos: 1000.00"));
        assertTrue(enviado.contains("Gastos: 300.00"));
        assertTrue(enviado.contains("Balance: 700.00"));
        assertTrue(enviado.contains("Comida"));
        assertFalse(enviado.contains("Super secreto"));   // las descripciones NO se envían
    }

    @Test
    void mesSinMovimientos_noLlamaALaIA() throws Exception {
        String token = nuevoUsuarioConToken();

        mvc.perform(post("/api/asistente/analisis").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restantesHoy").value(3));   // no se gastó ningún intento

        verifyNoInteractions(ia);
    }

    @Test
    void limiteDiario_alCuartoIntentoResponde429() throws Exception {
        String token = usuarioConDatos();
        when(ia.completar(anyString(), anyString())).thenReturn("ok");

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/asistente/analisis").header("Authorization", token))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/asistente/analisis").header("Authorization", token))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void iaNoDisponible_responde503_yNoConsumeIntento() throws Exception {
        String token = usuarioConDatos();

        // Simulamos que Groq está caído
        when(ia.completar(anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "caído"));

        mvc.perform(post("/api/asistente/analisis").header("Authorization", token))
                .andExpect(status().isServiceUnavailable());

        // Groq "vuelve": el siguiente intento debe mostrar que el fallo no contó
        reset(ia);
        when(ia.completar(anyString(), anyString())).thenReturn("ok");

        mvc.perform(post("/api/asistente/analisis").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restantesHoy").value(2));   // 3 - 1 exitoso
    }
}