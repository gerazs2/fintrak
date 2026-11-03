package com.jgmagalop.services.ia;

import com.jgmagalop.services.GeneradorRecurrentes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cuenta cuántos análisis de IA ha usado cada usuario en el día. */
@Component
public class LimiteDiarioIA {

    /** Cuántos usos lleva un usuario y en qué fecha. */
    private record Uso(LocalDate fecha, int cantidad) {}

    private final int maximo;
    private final Map<Long, Uso> usos = new ConcurrentHashMap<>();

    public LimiteDiarioIA(@Value("${ia.limite-diario}") int maximo) {
        this.maximo = maximo;
    }

    /** Lanza 429 si el usuario ya llegó al límite de hoy. No cuenta un uso. */
    public void verificar(Long uid) {
        if (usadosHoy(uid) >= maximo) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Alcanzaste el límite de " + maximo + " análisis por día");
        }
    }

    /** Registra un uso exitoso y devuelve cuántos le quedan hoy. */
    public int registrar(Long uid) {
        LocalDate hoy = GeneradorRecurrentes.hoy();
        Uso u = usos.compute(uid, (id, anterior) ->
                (anterior == null || !anterior.fecha().equals(hoy))
                        ? new Uso(hoy, 1)                              // primer uso del día
                        : new Uso(hoy, anterior.cantidad() + 1));      // un uso más hoy
        return maximo - u.cantidad();
    }

    /** Cuántos análisis le quedan hoy, sin modificar nada. */
    public int restantes(Long uid) {
        return maximo - usadosHoy(uid);
    }

    private int usadosHoy(Long uid) {
        Uso u = usos.get(uid);
        if (u == null || !u.fecha().equals(GeneradorRecurrentes.hoy())) {
            return 0;   // nunca lo ha usado, o su último uso fue otro día
        }
        return u.cantidad();
    }
}