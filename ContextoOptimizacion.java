import java.util.*;

// ContextoOptimizacion.java
// Información compartida por todas las pasadas:
//   - la tabla de símbolos del semántico (nombre -> [tipo, línea]), para saber
//     qué nombres son variables del usuario y de qué tipo;
//   - la bitácora de cambios;
//   - utilidades para clasificar operandos (constante, temporal, variable).
//
// Los cuádruplos no guardan tipos; las constantes se reconocen por su forma:
//   entero   →  8, -3          cadena →  "abc" (con comillas)
//   booleano →  verdadero / falso
public class ContextoOptimizacion {

    private final Map<String, String[]> tablaSimbolos;
    private final List<CambioOptimizacion> bitacora = new ArrayList<>();

    // Los fija el Optimizador antes de llamar a cada pasada
    String pasadaActual = "";
    int    vueltaActual = 0;

    public ContextoOptimizacion(Map<String, String[]> tablaSimbolos) {
        this.tablaSimbolos = tablaSimbolos;
    }

    // ── Bitácora ──────────────────────────────────────────────────────────────
    public void registrar(int bloque, Cuadruplo antes, Cuadruplo despues, String motivo) {
        String a = (antes   == null) ? "(nueva)"     : antes.aTresDirecciones();
        String d = (despues == null) ? "(eliminada)" : despues.aTresDirecciones();
        bitacora.add(new CambioOptimizacion(vueltaActual, pasadaActual, bloque, a, d, motivo));
    }

    public List<CambioOptimizacion> getBitacora() {
        return bitacora;
    }

    // ── Clasificación de operandos ────────────────────────────────────────────
    public Map<String, String[]> getTablaSimbolos() {
        return tablaSimbolos;
    }

    public boolean esVariableUsuario(String s) {
        return tablaSimbolos.containsKey(s);
    }

    // Un temporal es tN y NO es una variable declarada por el usuario
    public boolean esTemporal(String s) {
        return s != null && s.matches("t\\d+") && !esVariableUsuario(s);
    }

    public static boolean esEntero(String s) {
        return s != null && s.matches("-?\\d+");
    }

    public static boolean esCadena(String s) {
        return s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"");
    }

    public static boolean esBooleano(String s) {
        return "verdadero".equals(s) || "falso".equals(s);
    }

    public static boolean esConstante(String s) {
        return esEntero(s) || esCadena(s) || esBooleano(s);
    }
}
