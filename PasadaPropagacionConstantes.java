import java.util.*;

// PasadaPropagacionConstantes.java — Propagación de constantes (LOCAL, por bloque).
// Dentro de un bloque básico, después de  x = c  (c constante), los usos de x
// se sustituyen por c hasta que x se vuelva a definir:
//
//      a = 5                 a = 5
//      t1 = a * 3     →      t1 = 5 * 3      (luego el plegado da t1 = 15)
//
// Es local: al entrar a un bloque no se sabe qué valor trae cada variable
// (puede venir de varios caminos o de una vuelta anterior del ciclo), así que
// la tabla de constantes empieza vacía en cada bloque.
// Un  param x ; call leerdig  DEFINE x con un valor desconocido: x deja de ser
// constante y ese param nunca se sustituye.
public class PasadaPropagacionConstantes implements Pasada {

    public String nombre()      { return "constantes"; }
    public String descripcion() { return "Propagacion de constantes (local)"; }

    public boolean aplicar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = false;
        for (BloqueBasico b : BloqueBasico.dividir(codigo)) {
            Map<String, String> constantes = new HashMap<>();

            for (int i = b.inicio; i <= b.fin; i++) {
                Cuadruplo c = codigo.get(i);
                Cuadruplo antes = AnalisisCuadruplo.copia(c);
                List<String> sustituidas = new ArrayList<>();

                // 1. Sustituir usos
                if (AnalisisCuadruplo.usaArg1(codigo, i) && constantes.containsKey(c.arg1)) {
                    sustituidas.add(c.arg1 + "=" + constantes.get(c.arg1));
                    c.arg1 = constantes.get(c.arg1);
                }
                if (AnalisisCuadruplo.usaArg2(codigo, i) && constantes.containsKey(c.arg2)) {
                    sustituidas.add(c.arg2 + "=" + constantes.get(c.arg2));
                    c.arg2 = constantes.get(c.arg2);
                }
                if (!sustituidas.isEmpty()) {
                    ctx.registrar(b.numero, antes, c, "se sabe que " + String.join(", ", sustituidas));
                    cambio = true;
                }

                // 2. Actualizar la tabla con lo que define este cuádruplo
                String def = AnalisisCuadruplo.definida(codigo, i);
                if (def != null) {
                    if (c.op.equals("=") && ContextoOptimizacion.esConstante(c.arg1)) {
                        constantes.put(def, c.arg1);
                    } else {
                        constantes.remove(def);  // valor calculado o leído: desconocido
                    }
                }
            }
        }
        return cambio;
    }
}
