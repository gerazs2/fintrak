package com.jgmagalop.services;

import com.jgmagalop.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneradorRecurrentesTest {

    @Mock RecurrenteRepository repo;
    @Mock TransaccionRepository transaccionRepo;
    @Mock OcurrenciaPendienteRepository pendienteRepo;

    @InjectMocks GeneradorRecurrentes generador;

    private static final LocalDate HOY = LocalDate.of(2026, 10, 22);

    private Recurrente plantilla(ModoRecurrente modo, Frecuencia f, LocalDate inicio) {
        Categoria c = new Categoria();
        c.setId(1L);
        c.setNombre("Gimnasio");
        c.setTipo(TipoMovimiento.GASTO);

        Usuario u = new Usuario();
        u.setId(1L);

        Recurrente r = new Recurrente();
        r.setCategoria(c);
        r.setUsuario(u);
        r.setModo(modo);
        r.setFrecuencia(f);
        r.setFechaInicio(inicio);
        r.setProximaFecha(inicio);
        r.setMonto(new BigDecimal("500.00"));
        r.setGeneradas(0);
        r.setActiva(true);
        return r;
    }

    @Test
    void automatico_seponeAlCorriente_creaTransacciones() {
        Recurrente r = plantilla(ModoRecurrente.AUTOMATICO, Frecuencia.MENSUAL, LocalDate.of(2026, 8, 15));

        int creadas = generador.generar(r, HOY);

        assertEquals(3, creadas);
        assertEquals(3, r.getGeneradas());
        assertEquals(LocalDate.of(2026, 11, 15), r.getProximaFecha());

        ArgumentCaptor<Transaccion> captor = ArgumentCaptor.forClass(Transaccion.class);
        verify(transaccionRepo, times(3)).save(captor.capture());
        List<LocalDate> fechas = captor.getAllValues().stream().map(Transaccion::getFecha).toList();
        assertEquals(List.of(
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 10, 15)), fechas);
        assertEquals(TipoMovimiento.GASTO, captor.getValue().getTipo());

        verify(pendienteRepo, never()).save(any());
    }

    @Test
    void confirmar_creaPendientes_noTransacciones() {
        Recurrente r = plantilla(ModoRecurrente.CONFIRMAR, Frecuencia.MENSUAL, LocalDate.of(2026, 8, 15));

        int creadas = generador.generar(r, HOY);

        assertEquals(3, creadas);
        verify(pendienteRepo, times(3)).save(any(OcurrenciaPendiente.class));
        verify(transaccionRepo, never()).save(any());
    }

    @Test
    void conFechaFin_seDesactivaAlTerminar() {
        Recurrente r = plantilla(ModoRecurrente.AUTOMATICO, Frecuencia.SEMANAL, LocalDate.of(2026, 10, 1));
        r.setFechaFin(LocalDate.of(2026, 10, 15));

        int creadas = generador.generar(r, HOY);

        assertEquals(3, creadas);          // 1, 8 y 15 de octubre
        assertFalse(r.isActiva());
    }

    @Test
    void fechaFutura_noGeneraNada() {
        Recurrente r = plantilla(ModoRecurrente.AUTOMATICO, Frecuencia.MENSUAL, LocalDate.of(2026, 11, 1));

        assertEquals(0, generador.generar(r, HOY));
        assertEquals(LocalDate.of(2026, 11, 1), r.getProximaFecha());
        verifyNoInteractions(transaccionRepo, pendienteRepo);
    }

    @Test
    void segundaEjecucion_noDuplica() {
        Recurrente r = plantilla(ModoRecurrente.AUTOMATICO, Frecuencia.MENSUAL, LocalDate.of(2026, 8, 15));

        generador.generar(r, HOY);
        int segunda = generador.generar(r, HOY);

        assertEquals(0, segunda);
        verify(transaccionRepo, times(3)).save(any());
    }

    @Test
    void inactiva_noGenera() {
        Recurrente r = plantilla(ModoRecurrente.AUTOMATICO, Frecuencia.MENSUAL, LocalDate.of(2026, 8, 15));
        r.setActiva(false);

        assertEquals(0, generador.generar(r, HOY));
        verifyNoInteractions(transaccionRepo, pendienteRepo);
    }
}