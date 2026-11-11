import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

// Pruebas.java — arnés de pruebas de la Unidad 3 (no forma parte del compilador).
//
// Recorre cada subcarpeta de pruebas/ que tenga progfte.txt (y opcionalmente
// progfte.in, un valor por línea) y hace dos comparaciones:
//
//   1. Ejecutor (AST)  vs  InterpreteCuadruplos sobre los cuádruplos ORIGINALES
//      → valida que el intérprete imita al Ejecutor.
//   2. Cuádruplos originales  vs  cuádruplos OPTIMIZADOS (ambos en el intérprete)
//      → valida que la optimización no cambió la salida.
//
// Se comparan las líneas impresas y el mensaje de error de ejecución (los
// cuádruplos no guardan renglón/columna, así que esos datos no se comparan).
//
// Uso (desde la raíz del repositorio):   pruebas/ejecutar.sh [opciones]
//   -v              muestra salidas lado a lado y la bitácora de cambios
//   -<pasada>       desactiva esa pasada (igual que en Main), ej. -copias
//   -todas          sin optimizaciones
//   <nombre>        solo ejecuta esa prueba, ej. ciclo
public class Pruebas {

    public static void main(String[] args) throws Exception {
        boolean verbose = false;
        List<String> opcionesOpt = new ArrayList<>();
        Set<String> soloEstas = new HashSet<>();
        for (String a : args) {
            if (a.equals("-v")) verbose = true;
            else if (a.startsWith("-")) opcionesOpt.add(a);
            else soloEstas.add(a);
        }

        List<Path> carpetas = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Path.of("pruebas"))) {
            for (Path p : ds) {
                if (Files.exists(p.resolve("progfte.txt"))
                        && (soloEstas.isEmpty() || soloEstas.contains(p.getFileName().toString()))) {
                    carpetas.add(p);
                }
            }
        }
        Collections.sort(carpetas);

        int fallas = 0;
        for (Path dir : carpetas) {
            if (!probar(dir, opcionesOpt.toArray(new String[0]), verbose)) fallas++;
        }
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  " + (carpetas.size() - fallas) + " de " + carpetas.size() + " pruebas correctas"
                + (fallas == 0 ? "" : "  — " + fallas + " CON DIFERENCIAS"));
        System.out.println("=".repeat(70));
        System.exit(fallas == 0 ? 0 : 1);
    }

    private static boolean probar(Path dir, String[] opcionesOpt, boolean verbose) throws Exception {
        String nombre = dir.getFileName().toString();
        String fuente = Files.readString(dir.resolve("progfte.txt"));
        Path rutaIn = dir.resolve("progfte.in");
        List<String> entradas = Files.exists(rutaIn) ? Files.readAllLines(rutaIn) : null;

        System.out.println("\n[" + nombre + "]  progfte.in = "
                + (entradas == null ? "(no existe: vectores automaticos)" : entradas));

        // ── Fases 1 a 4: mismas llamadas que Main ───────────────────────────
        List<Token> tokens = new AnalizadorLexico().analizar(fuente);
        // construirTablaSimbolos es privado en Main; se invoca por reflexión
        // para no modificar Main solo por las pruebas.
        Method m = Main.class.getDeclaredMethod("construirTablaSimbolos", List.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String[]> tablaTokens = (Map<String, String[]>) m.invoke(null, tokens);

        AnalizadorSintactico sint = new AnalizadorSintactico(tokens, tablaTokens);
        NodoArbol arbol = sint.analizar();
        AnalizadorSemantico sem = new AnalizadorSemantico();
        if (!sint.tieneErrores()) sem.analizar(arbol);
        if (sint.tieneErrores() || !sem.getErrores().isEmpty()) {
            System.out.println("  FALLA: el programa tiene errores de compilacion");
            sint.getErrores().forEach(e -> System.out.println("    " + e));
            sem.getErrores().forEach(e -> System.out.println("    " + e));
            return false;
        }
        List<Cuadruplo> cuads = new GeneradorCodigoIntermedio().generar(arbol);

        // ── Optimización ────────────────────────────────────────────────────
        Optimizador opt = new Optimizador();
        opt.configurar(opcionesOpt).forEach(a -> System.out.println("  " + a));
        List<Cuadruplo> optimizado = opt.optimizar(cuads, sem.getTablaSimbolos());
        List<Optimizador.Verificacion> verifs = opt.verificar(entradas);

        boolean ok = true;

        // ── 1. Ejecutor vs intérprete (solo con progfte.in: el Ejecutor lee teclado)
        if (entradas != null) {
            List<String> obsEjecutor = ejecutarEjecutor(arbol, sem.getTablaSimbolos(), entradas);
            List<String> obsInterprete = verifs.get(0).original.observable();
            boolean igual = obsEjecutor.equals(obsInterprete);
            ok &= igual;
            System.out.println("  Ejecutor vs interprete(original) : " + (igual ? "IGUAL" : "DIFERENTE"));
            if (verbose || !igual) ladoALado("EJECUTOR (AST)", obsEjecutor, "INTERPRETE (original)", obsInterprete);
        }

        // ── 2. Original vs optimizado ───────────────────────────────────────
        MetricasCodigo antes = MetricasCodigo.calcular(cuads, new ContextoOptimizacion(sem.getTablaSimbolos()));
        MetricasCodigo desp  = MetricasCodigo.calcular(optimizado, new ContextoOptimizacion(sem.getTablaSimbolos()));
        for (Optimizador.Verificacion v : verifs) {
            ok &= v.iguales();
            System.out.printf("  original vs optimizado [%-11s]: %-9s ejecutadas %d -> %d%n",
                    v.fuente, v.iguales() ? "IGUAL" : "DIFERENTE",
                    v.original.instrucciones, v.optimizado.instrucciones);
            if (verbose || !v.iguales()) {
                ladoALado("ORIGINAL", v.original.observable(), "OPTIMIZADO", v.optimizado.observable());
            }
        }
        System.out.printf("  cuadruplos %d -> %d   temporales %d -> %d   etiquetas %d -> %d   cambios: %s%n",
                antes.cuadruplos, desp.cuadruplos, antes.temporales, desp.temporales,
                antes.etiquetas, desp.etiquetas, resumenCambios(opt.getBitacora()));

        if (verbose) {
            System.out.println("  Bitacora:");
            for (CambioOptimizacion c : opt.getBitacora()) System.out.println("    " + c);
            System.out.println("  Codigo optimizado:");
            for (Cuadruplo c : optimizado) System.out.println("    " + c.aTresDirecciones());
        }
        return ok;
    }

    // Ejecuta el Ejecutor con el teclado sustituido por progfte.in y captura lo que imprime
    private static List<String> ejecutarEjecutor(NodoArbol arbol, Map<String, String[]> tabla,
                                                 List<String> entradas) throws Exception {
        PrintStream real = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        String error = null;
        try {
            new Ejecutor(tabla, new Scanner(String.join("\n", entradas) + "\n")).ejecutar(arbol);
        } catch (ErrorEjecucion e) {
            error = e.getMessage();
        } catch (NoSuchElementException e) {
            error = "progfte.in no tiene suficientes valores para el Ejecutor";
        } finally {
            System.setOut(real);
        }
        // Se quitan los mensajes de lectura: no son salida del programa RAFI
        String texto = buf.toString(StandardCharsets.UTF_8)
                .replaceAll("Ingresa un valor ENTERO para '[^']*': ", "")
                .replaceAll("Ingresa una CADENA para '[^']*': ", "")
                .replaceAll("  Eso no es un número entero válido, intenta de nuevo.\n", "");
        List<String> obs = new ArrayList<>();
        for (String l : texto.split("\n")) if (!l.isEmpty()) obs.add(l);
        if (error != null) obs.add("ERROR: " + error);
        return obs;
    }

    private static void ladoALado(String t1, List<String> a, String t2, List<String> b) {
        System.out.printf("      %-40s | %s%n", t1, t2);
        if (a.isEmpty() && b.isEmpty()) System.out.printf("      %-40s | %s%n", "(sin salida)", "(sin salida)");
        for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
            System.out.printf("      %-40s | %s%n", i < a.size() ? a.get(i) : "", i < b.size() ? b.get(i) : "");
        }
    }

    private static String resumenCambios(List<CambioOptimizacion> bitacora) {
        if (bitacora.isEmpty()) return "ninguno";
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (CambioOptimizacion c : bitacora) conteo.merge(c.pasada, 1, Integer::sum);
        return conteo.toString();
    }
}
