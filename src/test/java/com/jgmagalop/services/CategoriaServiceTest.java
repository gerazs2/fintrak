package com.jgmagalop.services;

import com.jgmagalop.domain.*;
import com.jgmagalop.dto.CategoriaRequest;
import com.jgmagalop.dto.CategoriaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock CategoriaRepository repo;
    @Mock TransaccionRepository transaccionRepo;
    @Mock PresupuestoRepository presupuestoRepo;
    @Mock UsuarioActual usuarioActual;

    @InjectMocks CategoriaService service;

    Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@mail.com");
        when(usuarioActual.get()).thenReturn(usuario);
    }

    @Test
    void crear_categoriaNueva_seGuardaConNombreLimpio() {
        when(repo.existsByUsuarioIdAndNombreIgnoreCaseAndTipo(1L, "Comida", TipoMovimiento.GASTO))
                .thenReturn(false);
        when(repo.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CategoriaResponse res = service.crear(new CategoriaRequest("  Comida  ", TipoMovimiento.GASTO));

        assertEquals(10L, res.id());
        assertEquals("Comida", res.nombre());
        assertEquals(TipoMovimiento.GASTO, res.tipo());
    }

    @Test
    void crear_duplicada_lanza409() {
        when(repo.existsByUsuarioIdAndNombreIgnoreCaseAndTipo(1L, "Comida", TipoMovimiento.GASTO))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.crear(new CategoriaRequest("Comida", TipoMovimiento.GASTO)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repo, never()).save(any());
    }

    @Test
    void obtener_deOtroUsuario_lanza404() {
        when(repo.findByIdAndUsuarioId(99L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obtener(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void eliminar_conTransacciones_lanza409YNoBorra() {
        Categoria c = new Categoria();
        c.setId(5L);
        when(repo.findByIdAndUsuarioId(5L, 1L)).thenReturn(Optional.of(c));
        when(transaccionRepo.existsByCategoriaId(5L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.eliminar(5L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repo, never()).delete(any());
    }
}