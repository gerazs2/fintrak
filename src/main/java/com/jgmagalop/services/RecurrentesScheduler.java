package com.jgmagalop.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurrentesScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurrentesScheduler.class);

    private final GeneradorRecurrentes generador;

    public RecurrentesScheduler(GeneradorRecurrentes generador) {
        this.generador = generador;
    }

    // Cada vez que la app arranca (en Render, también al despertar)
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        ejecutar("arranque");
    }

    // Todos los días a las 6:00 hora de México, si la app está despierta
    @Scheduled(cron = "0 0 6 * * *", zone = "America/Mexico_City")
    public void diario() {
        ejecutar("programado");
    }

    private void ejecutar(String origen) {
        try {
            int n = generador.procesarPendientes(GeneradorRecurrentes.hoy());
            log.info("Recurrentes ({}): {} transacciones generadas", origen, n);
        } catch (Exception e) {
            log.error("Error al procesar recurrentes ({})", origen, e);
        }
    }
}