package com.jgmagalop.services;

import com.jgmagalop.dto.*;
import com.jgmagalop.services.ia.AsistenteIA;
import com.jgmagalop.services.ia.LimiteDiarioIA;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
public class AsistenteService {

    private static final String INSTRUCCIONES = """
            Eres el asistente de finanzas personales de la app FinTrack.
            Analiza SOLO los datos que te envía el usuario y responde en español de México.
            Reglas:
            - No inventes cifras: usa únicamente las que aparecen en los datos.
            - Si falta un dato, no lo supongas.
            - Máximo 180 palabras, en texto plano, con este formato:
              Resumen: 2 o 3 frases sobre ingresos, gastos y balance del mes.
              Alertas: presupuestos en ALERTA o EXCEDIDO; si no hay, dilo.
              Tendencia: cómo se compara el mes con los meses anteriores.
              Recomendaciones: 2 o 3 acciones concretas.
            - No recomiendes productos financieros ni inversiones específicas.
            """;

    private final ResumenService resumenService;
    private final PresupuestoService presupuestoService;
    private final AsistenteIA ia;
    private final LimiteDiarioIA limite;
    private final UsuarioActual usuarioActual;

    public AsistenteService(ResumenService resumenService,
                            PresupuestoService presupuestoService,
                            AsistenteIA ia,
                            LimiteDiarioIA limite,
                            UsuarioActual usuarioActual) {
        this.resumenService = resumenService;
        this.presupuestoService = presupuestoService;
        this.ia = ia;
        this.limite = limite;
        this.usuarioActual = usuarioActual;
    }

    public AnalisisResponse analizar(YearMonth mes) {
        if (mes == null) mes = YearMonth.now();
        Long uid = usuarioActual.get().getId();

        ResumenResponse resumen = resumenService.mensual(mes);

        // Sin movimientos: no hay nada que analizar y no gastamos una llamada a la IA
        if (resumen.ingresos().signum() == 0 && resumen.gastos().signum() == 0) {
            return new AnalisisResponse(mes.toString(),
                    "No hay movimientos registrados en " + mes + ", así que todavía no hay nada que analizar.",
                    limite.restantes(uid));
        }

        limite.verificar(uid);                               // 429 si ya no le quedan
        String datos = armarDatos(mes, resumen);
        String texto = ia.completar(INSTRUCCIONES, datos);   // 503 o 429 si la IA falla
        int restantes = limite.registrar(uid);               // solo si todo salió bien

        return new AnalisisResponse(mes.toString(), texto, restantes);
    }

    /** Arma el texto que se envía a la IA. Solo datos agregados, sin descripciones de transacciones. */
    private String armarDatos(YearMonth mes, ResumenResponse r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mes analizado: ").append(mes).append('\n');
        sb.append("Ingresos: ").append(r.ingresos()).append('\n');
        sb.append("Gastos: ").append(r.gastos()).append('\n');
        sb.append("Balance: ").append(r.balance()).append('\n');

        sb.append("\nGastos por categoría:\n");
        agregarCategorias(sb, r.gastosPorCategoria());

        sb.append("\nIngresos por categoría:\n");
        agregarCategorias(sb, r.ingresosPorCategoria());

        sb.append("\nPresupuestos del mes:\n");
        List<PresupuestoResponse> presupuestos = presupuestoService.listar(mes);
        if (presupuestos.isEmpty()) {
            sb.append("- (sin presupuestos)\n");
        }
        for (PresupuestoResponse p : presupuestos) {
            sb.append("- ").append(p.categoriaNombre())
              .append(": límite ").append(p.limite())
              .append(", gastado ").append(p.gastado())
              .append(", ").append(p.porcentaje()).append("%")
              .append(", estado ").append(p.estado()).append('\n');
        }

        sb.append("\nÚltimos 6 meses (hasta el actual):\n");
        for (TendenciaMes t : resumenService.tendencia(6)) {
            sb.append("- ").append(t.mes())
              .append(": ingresos ").append(t.ingresos())
              .append(", gastos ").append(t.gastos())
              .append(", balance ").append(t.balance()).append('\n');
        }
        return sb.toString();
    }

    private void agregarCategorias(StringBuilder sb, List<CategoriaResumen> lista) {
        if (lista.isEmpty()) {
            sb.append("- (ninguno)\n");
            return;
        }
        for (CategoriaResumen c : lista) {
            sb.append("- ").append(c.nombre())
              .append(": ").append(c.total())
              .append(" (").append(c.porcentaje()).append("%)\n");
        }
    }
}