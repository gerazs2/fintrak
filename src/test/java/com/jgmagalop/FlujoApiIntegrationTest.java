package com.jgmagalop;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FlujoApiIntegrationTest {

    @Autowired MockMvc mvc;

    // ---------- helpers ----------

    /** Registra un usuario con email único, hace login y devuelve "Bearer <token>". */
    private String nuevoUsuarioConToken() throws Exception {
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

    private Integer crearCategoria(String token, String nombre, String tipo) throws Exception {
        String body = mvc.perform(post("/api/categorias")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","tipo":"%s"}
                                """.formatted(nombre, tipo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    // ---------- tests ----------

    @Test
    void sinToken_responde401() throws Exception {
        mvc.perform(get("/api/categorias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInventado_responde401() throws Exception {
        mvc.perform(get("/api/categorias").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroDuplicado_responde409() throws Exception {
        String json = """
                {"email":"dup@mail.com","password":"Password123","nombre":"Dup"}
                """;
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void registroInvalido_responde400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"no-es-email","password":"123","nombre":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void crearYListarCategorias() throws Exception {
        String token = nuevoUsuarioConToken();
        crearCategoria(token, "Comida", "GASTO");
        crearCategoria(token, "Sueldo", "INGRESO");

        mvc.perform(get("/api/categorias").param("tipo", "GASTO").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Comida"));
    }

    @Test
    void usuarioNoPuedeVerCategoriaDeOtro() throws Exception {
        String tokenA = nuevoUsuarioConToken();
        String tokenB = nuevoUsuarioConToken();
        Integer idDeA = crearCategoria(tokenA, "Privada", "GASTO");

        mvc.perform(get("/api/categorias/" + idDeA).header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void transaccionTomaTipoDeCategoria_yResumenLaSuma() throws Exception {
        String token = nuevoUsuarioConToken();
        Integer comida = crearCategoria(token, "Comida", "GASTO");
        Integer sueldo = crearCategoria(token, "Sueldo", "INGRESO");
        String hoy = LocalDate.now().toString();

        mvc.perform(post("/api/transacciones")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":250.50,"fecha":"%s","descripcion":"Super","categoriaId":%d}
                                """.formatted(hoy, comida)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("GASTO"));

        mvc.perform(post("/api/transacciones")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":1000,"fecha":"%s","categoriaId":%d}
                                """.formatted(hoy, sueldo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("INGRESO"));

        mvc.perform(get("/api/resumen").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresos").value(1000.0))
                .andExpect(jsonPath("$.gastos").value(250.5))
                .andExpect(jsonPath("$.balance").value(749.5));
    }

    @Test
    void cambiarPassword_invalidaTokenAnterior() throws Exception {
        String email = "pass-" + UUID.randomUUID() + "@mail.com";
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"Password123","nombre":"P"}
                        """.formatted(email)));

        String login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123"}
                                """.formatted(email)))
                .andReturn().getResponse().getContentAsString();
        String tokenViejo = "Bearer " + JsonPath.read(login, "$.token");

        // El iat del JWT va en segundos: esperamos para que el token viejo sea claramente anterior
        Thread.sleep(1100);

        String cambio = mvc.perform(put("/api/usuarios/me/password")
                        .header("Authorization", tokenViejo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"passwordActual":"Password123","passwordNueva":"NuevaPass456"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tokenNuevo = "Bearer " + JsonPath.read(cambio, "$.token");

        mvc.perform(get("/api/usuarios/me").header("Authorization", tokenViejo))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me").header("Authorization", tokenNuevo))
                .andExpect(status().isOk());
    }
}