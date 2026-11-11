import java.util.*;

// MetricasCodigo.java
// Métricas de costo estáticas de una lista de cuádruplos:
//   cuadruplos  → total de cuádruplos (incluye las 'etiq')
//   temporales  → temporales distintos que se definen (tN)
//   etiquetas   → cuádruplos 'etiq'
// Las instrucciones EJECUTADAS son dinámicas: las da InterpreteCuadruplos
// por cada vector de entradas.
public class MetricasCodigo {

    public int cuadruplos;
    public int temporales;
    public int etiquetas;

    public static MetricasCodigo calcular(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        MetricasCodigo m = new MetricasCodigo();
        Set<String> temps = new HashSet<>();
        m.cuadruplos = codigo.size();
        for (Cuadruplo c : codigo) {
            if (c.op.equals("etiq")) m.etiquetas++;
            else if (ctx.esTemporal(c.resultado)) temps.add(c.resultado);
        }
        m.temporales = temps.size();
        return m;
    }

    // Fila "METRICA | ANTES | DESPUES | AHORRO"
    public static String fila(String nombre, int antes, int despues) {
        String ahorro = (antes == 0) ? "-" :
                String.format("%.1f%%", 100.0 * (antes - despues) / antes);
        return String.format("  %-34s | %8d | %8d | %7s%n", nombre, antes, despues, ahorro);
    }

    public static String encabezado() {
        return String.format("  %-34s | %8s | %8s | %7s%n", "METRICA", "ANTES", "DESPUES", "AHORRO")
                + "  " + "-".repeat(65) + "\n";
    }
}
