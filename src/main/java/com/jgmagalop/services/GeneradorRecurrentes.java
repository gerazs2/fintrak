package com.jgmagalop.services;

import com.jgmagalop.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class GeneradorRecurrentes {

    public static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private final RecurrenteRepository repo;
    private final TransaccionRepository transaccionRepo;
    private final OcurrenciaPendienteRepository pendienteRepo;

    public GeneradorRecurrentes(
    							 RecurrenteRepository repo, 
    							 TransaccionRepository transaccionRepo,
    							 OcurrenciaPendienteRepository pendienteRepo
    						   ) {
        this.repo = repo;
        this.transaccionRepo = transaccionRepo;
        this.pendienteRepo = pendienteRepo;
    }

    /** "Hoy" según la zona horaria de la app, no la del servidor (Render usa UTC). */
    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    @Transactional
    public int procesarPendientes(LocalDate hoy) {
        return procesar(repo.findPendientes(hoy), hoy);
    }

    @Transactional
    public int procesarPendientesDelUsuario(Long uid, LocalDate hoy) {
        return procesar(repo.findPendientesDelUsuario(uid, hoy), hoy);
    }

    private int procesar(List<Recurrente> lista, LocalDate hoy) {
        int total = 0;
        for (Recurrente r : lista) total += generar(r, hoy);
        return total;
    }

	/** Genera todas las ocurrencias vencidas hasta hoy (se pone al corriente). */
	@Transactional
	public int generar(Recurrente r, LocalDate hoy) {
	    int creadas = 0;
	    while (r.isActiva()) {
	        LocalDate fecha = r.fechaDeOcurrencia(r.getGeneradas());
	
	        if (r.getFechaFin() != null && fecha.isAfter(r.getFechaFin())) {
	            r.setActiva(false);          // ya terminó su vigencia
	            break;
	        }
	        if (fecha.isAfter(hoy)) break;   // aún no le toca
	
	        if (r.getModo() == ModoRecurrente.AUTOMATICO) {
	            // Se registra directamente como transacción
	            Transaccion t = new Transaccion();
	            t.setUsuario(r.getUsuario());
	            t.setCategoria(r.getCategoria());
	            t.setTipo(r.getCategoria().getTipo());
	            t.setMonto(r.getMonto());
	            t.setFecha(fecha);
	            t.setDescripcion(r.getDescripcion());
	            t.setRecurrente(r);
	            transaccionRepo.save(t);
	        } else {
	            // Queda pendiente hasta que el usuario lo confirme u omita
	            OcurrenciaPendiente o = new OcurrenciaPendiente();
	            o.setRecurrente(r);
	            o.setUsuario(r.getUsuario());
	            o.setFecha(fecha);
	            o.setMonto(r.getMonto());
	            pendienteRepo.save(o);
	        }
	
	        r.setGeneradas(r.getGeneradas() + 1);
	        creadas++;
	    }
	    r.setProximaFecha(r.fechaDeOcurrencia(r.getGeneradas()));
	    return creadas;
	}
}