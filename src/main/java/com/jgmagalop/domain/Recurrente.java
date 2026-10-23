package com.jgmagalop.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(indexes = @Index(name = "idx_recurrente_activa_proxima", columnList = "activa, proxima_fecha"))
@Getter
@Setter
@NoArgsConstructor
public class Recurrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frecuencia frecuencia;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @Column(nullable = false)
    private LocalDate proximaFecha;

    // Cuántas ocurrencias se han generado (o saltado)
    @Column(nullable = false)
    private int generadas;

    @Column(nullable = false)
    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Fecha de la ocurrencia número n (0 = fechaInicio). */
    public LocalDate fechaDeOcurrencia(int n) {
        return switch (frecuencia) {
            case SEMANAL   -> fechaInicio.plusWeeks(n);
            case QUINCENAL -> fechaInicio.plusWeeks(2L * n);
            case MENSUAL   -> fechaInicio.plusMonths(n);
            case ANUAL     -> fechaInicio.plusYears(n);
        };
    }
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModoRecurrente modo;
}