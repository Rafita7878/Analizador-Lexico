import java.util.*;

// AnalisisCuadruplo.java
// Utilidades compartidas por las pasadas: qué operandos USA un cuádruplo y
// qué nombre DEFINE (asigna).
//
//   op binario (+ - * / < > == <= >= <> y o)  usa arg1, arg2   define resultado
//   = , menos , no                             usa arg1         define resultado
//   ifFalse                                    usa arg1
//   param x  seguido de call impdig/impcad     usa x
//   param x  seguido de call leerdig/leercad   DEFINE x  (no lo usa: lo sobrescribe)
//   call, goto, etiq                           ni usa ni define nombres
public class AnalisisCuadruplo {

    public static final Set<String> BINARIOS = Set.of(
            "+", "-", "*", "/", "<", ">", "==", "<=", ">=", "<>", "y", "o");

    public static boolean esBinario(Cuadruplo c) { return BINARIOS.contains(c.op); }

    public static boolean esUnario(Cuadruplo c) { return c.op.equals("menos") || c.op.equals("no"); }

    // 'param' cuyo call siguiente es una lectura → el parámetro es de salida.
    // GeneradorCodigoIntermedio siempre emite param inmediatamente antes de call.
    public static boolean esParamDeLectura(List<Cuadruplo> codigo, int i) {
        if (!codigo.get(i).op.equals("param")) return false;
        for (int k = i + 1; k < codigo.size(); k++) {
            Cuadruplo c = codigo.get(k);
            if (c.op.equals("call")) return c.arg1.equals("leerdig") || c.arg1.equals("leercad");
            if (!c.op.equals("param")) return false;
        }
        return false;
    }

    public static boolean usaArg1(List<Cuadruplo> codigo, int i) {
        Cuadruplo c = codigo.get(i);
        if (esBinario(c) || esUnario(c)) return true;
        if (c.op.equals("=") || c.op.equals("ifFalse")) return true;
        return c.op.equals("param") && !esParamDeLectura(codigo, i);
    }

    public static boolean usaArg2(List<Cuadruplo> codigo, int i) {
        return esBinario(codigo.get(i));
    }

    // Nombre que define el cuádruplo i, o null si no define ninguno
    public static String definida(List<Cuadruplo> codigo, int i) {
        Cuadruplo c = codigo.get(i);
        if (esBinario(c) || esUnario(c) || c.op.equals("=")) return c.resultado;
        if (esParamDeLectura(codigo, i)) return c.arg1;
        return null;
    }

    // Nombres usados como operando (no constantes) en el cuádruplo i
    public static List<String> usos(List<Cuadruplo> codigo, int i) {
        List<String> r = new ArrayList<>();
        Cuadruplo c = codigo.get(i);
        if (usaArg1(codigo, i) && !ContextoOptimizacion.esConstante(c.arg1)) r.add(c.arg1);
        if (usaArg2(codigo, i) && !ContextoOptimizacion.esConstante(c.arg2)) r.add(c.arg2);
        return r;
    }

    // Cuántas veces se usa 'nombre' como operando en todo el programa
    public static int contarUsos(List<Cuadruplo> codigo, String nombre) {
        int n = 0;
        for (int i = 0; i < codigo.size(); i++) {
            for (String u : usos(codigo, i)) if (u.equals(nombre)) n++;
        }
        return n;
    }

    // Valor Java → constante en la notación de los cuádruplos
    public static String aConstante(Object v) {
        if (v instanceof Boolean) return ((Boolean) v) ? "verdadero" : "falso";
        if (v instanceof String)  return "\"" + v + "\"";
        return String.valueOf(v);
    }

    public static Cuadruplo copia(Cuadruplo c) {
        return new Cuadruplo(c.op, c.arg1, c.arg2, c.resultado);
    }
}
