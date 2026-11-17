import java.util.*;

// PasadaAlgebraica.java — Simplificación algebraica y reducción de fuerza.
// Aplica identidades que valen para CUALQUIER valor del operando x, así que
// sirven aunque x sea desconocido (leído con leerdig):
//
//   x + 0, 0 + x, x - 0, x * 1, 1 * x, x / 1   →  x
//   x * 0, 0 * x, x - x                        →  0
//   x * 2, 2 * x                               →  x + x   (reducción de fuerza)
//   x y verdadero, verdadero y x, x o falso, falso o x   →  x
//   x y falso, falso y x                       →  falso
//   x o verdadero, verdadero o x               →  verdadero
//   x == x, x <= x, x >= x                     →  verdadero
//   x <> x, x < x, x > x                       →  falso
//
// NO se simplifican  0 / x  ni  x / x : si x vale 0 el programa original
// debe detenerse con "División entre cero".
public class PasadaAlgebraica implements Pasada {

    public String nombre()      { return "algebraica"; }
    public String descripcion() { return "Simplificacion algebraica"; }

    public boolean aplicar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        List<BloqueBasico> bloques = BloqueBasico.dividir(codigo);
        boolean cambio = false;

        for (int i = 0; i < codigo.size(); i++) {
            Cuadruplo c = codigo.get(i);
            if (!AnalisisCuadruplo.esBinario(c)) continue;
            // Si ambos son constantes es trabajo del plegado
            if (ContextoOptimizacion.esConstante(c.arg1) && ContextoOptimizacion.esConstante(c.arg2)) continue;

            Cuadruplo antes = AnalisisCuadruplo.copia(c);
            String motivo = simplificar(c);
            if (motivo != null) {
                ctx.registrar(BloqueBasico.bloqueDe(bloques, i), antes, c, motivo);
                cambio = true;
            }
        }
        return cambio;
    }

    // Modifica c si aplica alguna identidad; devuelve el motivo o null
    private String simplificar(Cuadruplo c) {
        String a = c.arg1, b = c.arg2;
        boolean mismos = a.equals(b);

        switch (c.op) {
            case "+":
                if (b.equals("0")) return copia(c, a, "x + 0 = x");
                if (a.equals("0")) return copia(c, b, "0 + x = x");
                break;
            case "-":
                if (b.equals("0")) return copia(c, a, "x - 0 = x");
                if (mismos)        return copia(c, "0", "x - x = 0");
                break;
            case "*":
                if (b.equals("1")) return copia(c, a, "x * 1 = x");
                if (a.equals("1")) return copia(c, b, "1 * x = x");
                if (b.equals("0")) return copia(c, "0", "x * 0 = 0");
                if (a.equals("0")) return copia(c, "0", "0 * x = 0");
                if (b.equals("2")) { c.op = "+"; c.arg2 = a; return "reduccion de fuerza: x * 2 = x + x"; }
                if (a.equals("2")) { c.op = "+"; c.arg1 = b; return "reduccion de fuerza: 2 * x = x + x"; }
                break;
            case "/":
                if (b.equals("1")) return copia(c, a, "x / 1 = x");
                break;
            case "y":
                if (b.equals("verdadero")) return copia(c, a, "x y verdadero = x");
                if (a.equals("verdadero")) return copia(c, b, "verdadero y x = x");
                if (b.equals("falso") || a.equals("falso")) return copia(c, "falso", "x y falso = falso");
                break;
            case "o":
                if (b.equals("falso")) return copia(c, a, "x o falso = x");
                if (a.equals("falso")) return copia(c, b, "falso o x = x");
                if (b.equals("verdadero") || a.equals("verdadero"))
                    return copia(c, "verdadero", "x o verdadero = verdadero");
                break;
            case "==": case "<=": case ">=":
                if (mismos) return copia(c, "verdadero", "x " + c.op + " x = verdadero");
                break;
            case "<>": case "<": case ">":
                if (mismos) return copia(c, "falso", "x " + c.op + " x = falso");
                break;
        }
        return null;
    }

    private String copia(Cuadruplo c, String valor, String motivo) {
        c.op = "="; c.arg1 = valor; c.arg2 = "";
        return motivo;
    }
}
