package com.jgmagalop;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestBase {

    @Autowired
    protected MockMvc mvc;

    /** Registra un usuario con email único y devuelve "Bearer <token>". */
    protected String nuevoUsuarioConToken() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@mail.com";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123","nombre":"Test"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    protected Integer crearCategoria(String token, String nombre, String tipo) throws Exception {
        String body = postJson(token, "/api/categorias", """
                {"nombre":"%s","tipo":"%s"}
                """.formatted(nombre, tipo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected Integer crearTransaccion(String token, Integer categoriaId, String monto,
                                       LocalDate fecha, String descripcion) throws Exception {
        String body = postJson(token, "/api/transacciones", """
                {"monto":%s,"fecha":"%s","descripcion":"%s","categoriaId":%d}
                """.formatted(monto, fecha, descripcion, categoriaId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected ResultActions postJson(String token, String url, String json) throws Exception {
        return mvc.perform(post(url)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}