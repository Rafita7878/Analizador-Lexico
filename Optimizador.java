import java.util.*;

// Optimizador.java
// Unidad 3 — Optimización de código.
//
// Recibe la lista de cuádruplos que produjo GeneradorCodigoIntermedio y
// devuelve una lista NUEVA optimizada. La lista original no se modifica
// (se trabaja sobre una copia profunda), para poder compararlas después.
//
// Cada optimización es una Pasada independiente que se puede desactivar.
// Las pasadas activas se aplican en orden, una vuelta tras otra, hasta que
// ninguna cambie nada (punto fijo) o se llegue a MAX_VUELTAS, porque unas
// habilitan a otras (ej. plegar una condición permite quitar un salto).
//
// Desde la línea de comandos:   java Main -cse -licm   (desactiva esas dos)
//                                java Main -todas       (no optimiza nada)
public class Optimizador {

    private static final int MAX_VUELTAS = 10;

    // Pasadas en el orden en que se aplican dentro de cada vuelta
    private final List<Pasada> pasadas = new ArrayList<>();
    private final Map<String, Boolean> activas = new LinkedHashMap<>();

    private ContextoOptimizacion ctx;
    private List<Cuadruplo> optimizado = new ArrayList<>();
    private int vueltasRealizadas = 0;

    public Optimizador() {
        // Las pasadas se registran aquí conforme se implementan (etapas 3 a 7)
    }

    private void registrarPasada(Pasada p) {
        pasadas.add(p);
        activas.put(p.nombre(), true);
    }

    // ── Configuración ─────────────────────────────────────────────────────────
    public boolean activar(String nombre, boolean valor) {
        if (!activas.containsKey(nombre)) return false;
        activas.put(nombre, valor);
        return true;
    }

    // Interpreta los argumentos de Main. Devuelve advertencias para mostrar.
    public List<String> configurar(String[] args) {
        List<String> avisos = new ArrayList<>();
        for (String a : args) {
            if (a.equals("-todas")) {
                activas.replaceAll((k, v) -> false);
            } else if (a.startsWith("-") && activar(a.substring(1), false)) {
                // desactivada correctamente
            } else {
                avisos.add("ADVERTENCIA: argumento '" + a + "' no reconocido. Pasadas validas: "
                        + (activas.isEmpty() ? "(ninguna implementada)" : String.join(", ", activas.keySet()))
                        + ", o -todas");
            }
        }
        return avisos;
    }

    // ── Punto de entrada ──────────────────────────────────────────────────────
    public List<Cuadruplo> optimizar(List<Cuadruplo> original, Map<String, String[]> tablaSimbolos) {
        ctx = new ContextoOptimizacion(tablaSimbolos);
        optimizado = copiar(original);
        vueltasRealizadas = 0;

        boolean huboCambio = true;
        while (huboCambio && vueltasRealizadas < MAX_VUELTAS) {
            vueltasRealizadas++;
            huboCambio = false;
            for (Pasada p : pasadas) {
                if (!activas.get(p.nombre())) continue;
                ctx.pasadaActual = p.nombre();
                ctx.vueltaActual = vueltasRealizadas;
                if (p.aplicar(optimizado, ctx)) huboCambio = true;
            }
        }
        return optimizado;
    }

    public static List<Cuadruplo> copiar(List<Cuadruplo> lista) {
        List<Cuadruplo> copia = new ArrayList<>();
        for (Cuadruplo c : lista) copia.add(new Cuadruplo(c.op, c.arg1, c.arg2, c.resultado));
        return copia;
    }

    public List<CambioOptimizacion> getBitacora() {
        return ctx == null ? List.of() : ctx.getBitacora();
    }

    public List<Cuadruplo> getOptimizado() {
        return optimizado;
    }

    // ── Salida ────────────────────────────────────────────────────────────────
    // Código de tres direcciones + tabla de cuádruplos de cualquier lista
    public static String formatearCodigo(List<Cuadruplo> codigo) {
        StringBuilder sb = new StringBuilder();
        String divisor = "-".repeat(65) + "\n";
        for (Cuadruplo c : codigo) sb.append(c.aTresDirecciones()).append("\n");
        sb.append("\n");
        sb.append(String.format(" %-4s | %-9s | %-12s | %-12s | %s%n",
                "No.", "OP", "ARG1", "ARG2", "RESULTADO"));
        sb.append(divisor);
        for (int i = 0; i < codigo.size(); i++) {
            Cuadruplo c = codigo.get(i);
            sb.append(String.format(" %-4d | %-9s | %-12s | %-12s | %s%n",
                    i, c.op, c.arg1, c.arg2, c.resultado));
        }
        sb.append(divisor);
        return sb.toString();
    }

    public String aTexto() {
        StringBuilder sb = new StringBuilder();
        String separador = "=".repeat(65) + "\n";

        sb.append(separador);
        sb.append("  OPTIMIZACION DE CODIGO — LENGUAJE RAFI\n");
        sb.append("  Archivo: progfte.opt\n");
        sb.append(separador).append("\n");

        // ── Pasadas ──
        sb.append(separador).append("  PASADAS\n").append(separador);
        if (pasadas.isEmpty()) {
            sb.append("  (ninguna pasada implementada todavia)\n");
        } else {
            for (Pasada p : pasadas) {
                sb.append(String.format("  [%s] %-11s %s%n",
                        activas.get(p.nombre()) ? "X" : " ", p.nombre(), p.descripcion()));
            }
        }
        sb.append("  Vueltas realizadas: ").append(vueltasRealizadas).append("\n\n");

        // ── Bitácora ──
        sb.append(separador).append("  CAMBIOS APLICADOS\n").append(separador);
        List<CambioOptimizacion> bitacora = getBitacora();
        if (bitacora.isEmpty()) {
            sb.append("  Sin cambios.\n");
        } else {
            for (CambioOptimizacion c : bitacora) sb.append("  ").append(c).append("\n");
        }
        sb.append("\n");

        // ── Código optimizado ──
        sb.append(separador).append("  CODIGO OPTIMIZADO\n").append(separador);
        sb.append(formatearCodigo(optimizado));

        return sb.toString();
    }
}
