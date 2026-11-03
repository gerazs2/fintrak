package com.jgmagalop.services.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

/**
 * Implementación de AsistenteIA para cualquier API con formato de OpenAI
 * (Groq, Ollama, Alibaba, OpenAI...). El proveedor se elige con ia.url e ia.modelo.
 */
@Component
public class ClienteOpenAICompatible implements AsistenteIA {

    private static final Logger log = LoggerFactory.getLogger(ClienteOpenAICompatible.class);

    private final RestClient http;
    private final String modelo;

    public ClienteOpenAICompatible(@Value("${ia.url}") String url,
                                   @Value("${ia.api-key}") String apiKey,
                                   @Value("${ia.modelo}") String modelo) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.http = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.modelo = modelo;
        System.out.print("Api Key: "+apiKey);
    }

    @Override
    public String completar(String instrucciones, String datos) {
        ChatRequest req = new ChatRequest(modelo, "none", 0.3, 600, List.of(
                new Mensaje("system", instrucciones),
                new Mensaje("user", datos)));
        try {
            ChatResponse res = http.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(ChatResponse.class);

            if (res == null || res.choices() == null || res.choices().isEmpty()
                    || res.choices().get(0).message() == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "El asistente devolvió una respuesta vacía");
            }
            return res.choices().get(0).message().content().trim();

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "El servicio de IA alcanzó su límite. Intenta más tarde");
        } catch (RestClientException e) {
            log.error("Error al llamar al servicio de IA", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El asistente no está disponible en este momento");
        }
    }

    // ---------- formato de la API (estilo OpenAI) ----------

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ChatRequest(String model,
                       @JsonProperty("reasoning_effort") String reasoningEffort,
                       Double temperature,
                       @JsonProperty("max_tokens") Integer maxTokens,
                       List<Mensaje> messages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Mensaje(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Mensaje message) {}
}