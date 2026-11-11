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
    private List<Cuadruplo> original   = new ArrayList<>();
    private List<Cuadruplo> optimizado = new ArrayList<>();
    private int vueltasRealizadas = 0;

    public Optimizador() {
        // Las pasadas se registran aquí conforme se implementan (etapas 3 a 7)
        registrarPasada(new PasadaPlegado());
        registrarPasada(new PasadaPropagacionConstantes());
        registrarPasada(new PasadaPropagacionCopias());
        registrarPasada(new PasadaAlgebraica());
        registrarPasada(new PasadaSubexpresionesComunes());
        registrarPasada(new PasadaMirilla());
        registrarPasada(new PasadaCodigoMuerto());
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
        this.original = copiar(original);
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

    // ── Verificación ──────────────────────────────────────────────────────────
    // Ejecuta la lista original y la optimizada con las MISMAS entradas y
    // compara lo que imprimen. 'entradasArchivo' son las líneas de progfte.in;
    // si es null se usan los vectores automáticos.
    public static class Verificacion {
        public String fuente;
        public InterpreteCuadruplos.Resultado original, optimizado;
        public boolean iguales() {
            return original.observable().equals(optimizado.observable());
        }
        public boolean concluyente() {
            return !original.limiteAlcanzado && !optimizado.limiteAlcanzado;
        }
    }

    private final List<Verificacion> verificaciones = new ArrayList<>();

    public List<Verificacion> verificar(List<String> entradasArchivo) {
        verificaciones.clear();
        InterpreteCuadruplos interprete = new InterpreteCuadruplos(ctx.getTablaSimbolos());
        int n = (entradasArchivo != null) ? 1 : InterpreteCuadruplos.vectoresAutomaticos().size();
        for (int k = 0; k < n; k++) {
            // Cada ejecución necesita una fuente nueva (las fuentes llevan su posición)
            Verificacion v = new Verificacion();
            v.original   = interprete.ejecutar(original,   fuente(entradasArchivo, k));
            v.optimizado = interprete.ejecutar(optimizado, fuente(entradasArchivo, k));
            v.fuente     = fuente(entradasArchivo, k).nombre();
            verificaciones.add(v);
        }
        return verificaciones;
    }

    private static InterpreteCuadruplos.FuenteEntradas fuente(List<String> archivo, int k) {
        if (archivo != null) return InterpreteCuadruplos.deLista("progfte.in", archivo);
        return InterpreteCuadruplos.vectoresAutomaticos().get(k);
    }

    public boolean verificacionCorrecta() {
        for (Verificacion v : verificaciones) if (!v.iguales()) return false;
        return true;
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

        // ── Bloques básicos del código original ──
        sb.append(separador).append("  BLOQUES BASICOS (codigo original)\n").append(separador);
        sb.append(BloqueBasico.aTexto(BloqueBasico.dividir(original), original)).append("\n");

        // ── Variables vivas del código original ──
        sb.append(separador).append("  VARIABLES VIVAS POR BLOQUE (codigo original)\n").append(separador);
        sb.append(AnalisisVida.calcular(original).aTexto()).append("\n");

        // ── Bitácora ──
        sb.append(separador).append("  CAMBIOS APLICADOS\n").append(separador);
        sb.append(textoBitacora()).append("\n");

        // ── Código optimizado ──
        sb.append(separador).append("  CODIGO OPTIMIZADO\n").append(separador);
        sb.append(formatearCodigo(optimizado)).append("\n");

        // ── Bloques básicos del código optimizado ──
        sb.append(separador).append("  BLOQUES BASICOS (codigo optimizado)\n").append(separador);
        sb.append(BloqueBasico.aTexto(BloqueBasico.dividir(optimizado), optimizado)).append("\n");

        sb.append(separador).append("  VARIABLES VIVAS POR BLOQUE (codigo optimizado)\n").append(separador);
        sb.append(AnalisisVida.calcular(optimizado).aTexto()).append("\n");

        sb.append(textoMetricas()).append("\n");
        sb.append(textoVerificacion());
        return sb.toString();
    }

    // Bitácora agrupada por pasada (en el orden de las pasadas), y dentro de
    // cada grupo en el orden en que ocurrieron los cambios (vN = vuelta).
    public String textoBitacora() {
        List<CambioOptimizacion> bitacora = getBitacora();
        if (bitacora.isEmpty()) return "  Sin cambios.\n";
        StringBuilder sb = new StringBuilder();
        for (Pasada p : pasadas) {
            List<CambioOptimizacion> propios = new ArrayList<>();
            for (CambioOptimizacion c : bitacora) if (c.pasada.equals(p.nombre())) propios.add(c);
            if (!activas.get(p.nombre())) {
                sb.append("  -- ").append(p.descripcion()).append(": DESACTIVADA\n");
                continue;
            }
            sb.append("  -- ").append(p.descripcion()).append(": ")
              .append(propios.size()).append(" cambio(s)\n");
            for (CambioOptimizacion c : propios) sb.append("     ").append(c).append("\n");
        }
        return sb.toString();
    }

    public String textoMetricas() {
        StringBuilder sb = new StringBuilder();
        String separador = "=".repeat(65) + "\n";
        MetricasCodigo antes   = MetricasCodigo.calcular(original, ctx);
        MetricasCodigo despues = MetricasCodigo.calcular(optimizado, ctx);

        sb.append(separador).append("  METRICAS DE COSTO\n").append(separador);
        sb.append(MetricasCodigo.encabezado());
        sb.append(MetricasCodigo.fila("Cuadruplos", antes.cuadruplos, despues.cuadruplos));
        sb.append(MetricasCodigo.fila("Temporales", antes.temporales, despues.temporales));
        sb.append(MetricasCodigo.fila("Etiquetas",  antes.etiquetas,  despues.etiquetas));
        for (Verificacion v : verificaciones) {
            sb.append(MetricasCodigo.fila("Ejecutadas (" + v.fuente + ")",
                    v.original.instrucciones, v.optimizado.instrucciones));
        }
        sb.append("  (Ejecutadas = cuadruplos ejecutados por el interprete, sin contar etiquetas)\n");
        return sb.toString();
    }

    public String textoVerificacion() {
        StringBuilder sb = new StringBuilder();
        String separador = "=".repeat(65) + "\n";
        sb.append(separador).append("  VERIFICACION (interprete de cuadruplos)\n").append(separador);
        if (verificaciones.isEmpty()) return sb.append("  No se ejecuto la verificacion.\n").toString();

        for (Verificacion v : verificaciones) {
            String estado = !v.iguales() ? "DIFERENTE"
                    : (v.concluyente() ? "IGUAL" : "IGUAL (no concluyente: limite de pasos)");
            sb.append("  Entradas: ").append(v.fuente).append("  ->  ").append(estado).append("\n");
            List<String> a = v.original.observable(), b = v.optimizado.observable();
            sb.append(String.format("     %-28s | %s%n", "ORIGINAL", "OPTIMIZADO"));
            for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
                sb.append(String.format("     %-28s | %s%n",
                        i < a.size() ? a.get(i) : "", i < b.size() ? b.get(i) : ""));
            }
            if (a.isEmpty() && b.isEmpty()) sb.append("     (sin salida)\n");
        }
        sb.append("  Resultado: ").append(verificacionCorrecta()
                ? "la version optimizada produce la MISMA salida."
                : "ERROR: la version optimizada produce una salida DISTINTA.").append("\n");
        return sb.toString();
    }
}
