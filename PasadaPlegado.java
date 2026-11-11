import java.util.*;

// PasadaPlegado.java — Plegado de constantes.
// Si TODOS los operandos de una operación son constantes, se calcula el
// resultado en tiempo de compilación y la operación se vuelve una copia:
//
//      t1 = 8 + 2         →   t1 = 10
//      t2 = 3 < 5         →   t2 = verdadero
//      t3 = menos 4       →   t3 = -4
//
// El cálculo usa InterpreteCuadruplos.operar, la misma semántica que el
// intérprete y que Ejecutor (int de Java, división truncada).
// NO se pliega una división entre 0: en ejecución debe lanzar el error.
public class PasadaPlegado implements Pasada {

    public String nombre()      { return "plegado"; }
    public String descripcion() { return "Plegado de constantes"; }

    public boolean aplicar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        List<BloqueBasico> bloques = BloqueBasico.dividir(codigo);
        boolean cambio = false;

        for (int i = 0; i < codigo.size(); i++) {
            Cuadruplo c = codigo.get(i);
            Object valor;
            try {
                if (AnalisisCuadruplo.esBinario(c)
                        && ContextoOptimizacion.esConstante(c.arg1)
                        && ContextoOptimizacion.esConstante(c.arg2)) {
                    valor = InterpreteCuadruplos.operar(c.op, constante(c.arg1), constante(c.arg2));
                } else if (c.op.equals("menos") && ContextoOptimizacion.esEntero(c.arg1)) {
                    valor = -Integer.parseInt(c.arg1);
                } else if (c.op.equals("no") && ContextoOptimizacion.esBooleano(c.arg1)) {
                    valor = !c.arg1.equals("verdadero");
                } else {
                    continue;
                }
            } catch (ErrorEjecucion e) {
                continue;   // división entre cero: se deja para que falle en ejecución
            }

            Cuadruplo antes = AnalisisCuadruplo.copia(c);
            String k = AnalisisCuadruplo.aConstante(valor);
            c.op = "="; c.arg1 = k; c.arg2 = "";
            ctx.registrar(BloqueBasico.bloqueDe(bloques, i), antes, c,
                    "operandos constantes: se calcula en compilacion");
            cambio = true;
        }
        return cambio;
    }

    private static Object constante(String s) {
        if (ContextoOptimizacion.esEntero(s))   return Integer.parseInt(s);
        if (ContextoOptimizacion.esBooleano(s)) return s.equals("verdadero");
        return s.substring(1, s.length() - 1);  // cadena sin comillas
    }
}
