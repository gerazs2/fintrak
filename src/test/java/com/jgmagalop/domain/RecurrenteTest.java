package com.jgmagalop.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecurrenteTest {

    private Recurrente plantilla(Frecuencia f, LocalDate inicio) {
        Recurrente r = new Recurrente();
        r.setFrecuencia(f);
        r.setFechaInicio(inicio);
        return r;
    }

    @Test
    void mensual_finDeMes_noSeQuedaEn28() {
        Recurrente r = plantilla(Frecuencia.MENSUAL, LocalDate.of(2026, 1, 31));

        assertEquals(LocalDate.of(2026, 1, 31), r.fechaDeOcurrencia(0));
        assertEquals(LocalDate.of(2026, 2, 28), r.fechaDeOcurrencia(1));
        assertEquals(LocalDate.of(2026, 3, 31), r.fechaDeOcurrencia(2));   // vuelve al 31
        assertEquals(LocalDate.of(2026, 4, 30), r.fechaDeOcurrencia(3));
    }

    @Test
    void quincenal_cada14Dias() {
        Recurrente r = plantilla(Frecuencia.QUINCENAL, LocalDate.of(2026, 10, 1));

        assertEquals(LocalDate.of(2026, 10, 15), r.fechaDeOcurrencia(1));
        assertEquals(LocalDate.of(2026, 10, 29), r.fechaDeOcurrencia(2));
    }

    @Test
    void semanal_cada7Dias() {
        Recurrente r = plantilla(Frecuencia.SEMANAL, LocalDate.of(2026, 10, 1));

        assertEquals(LocalDate.of(2026, 10, 8), r.fechaDeOcurrencia(1));
        assertEquals(LocalDate.of(2026, 10, 22), r.fechaDeOcurrencia(3));
    }

    @Test
    void anual_desde29DeFebrero() {
        Recurrente r = plantilla(Frecuencia.ANUAL, LocalDate.of(2028, 2, 29));

        assertEquals(LocalDate.of(2029, 2, 28), r.fechaDeOcurrencia(1));   // año no bisiesto
        assertEquals(LocalDate.of(2032, 2, 29), r.fechaDeOcurrencia(4));   // bisiesto otra vez
    }
}