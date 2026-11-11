import java.util.*;

// PasadaCodigoMuerto.java — Eliminación de código muerto (análisis de vida GLOBAL).
//
// Una asignación  x = ...  está muerta si x no está viva justo después de
// ella: ningún camino posterior usa ese valor antes de que x se reasigne.
// Con AnalisisVida se obtiene OUT[B] de cada bloque y se recorre el bloque
// de abajo hacia arriba manteniendo el conjunto de vivas:
//
//     vivas = OUT[B]
//     para cada cuádruplo, del último al primero:
//        si define x, x ∉ vivas y es eliminable  →  se elimina
//                                                   (sus operandos NO se agregan a vivas)
//        si no:  vivas = (vivas − {x}) ∪ usos
//
// Al ser global, una variable asignada al final de un ciclo y usada en la
// siguiente vuelta (o en la condición) está viva gracias a la arista de
// regreso, y una variable usada solo en una rama del si está viva por esa rama.
//
// Las variables del usuario también se eliminan si están muertas: ningún
// archivo de salida muestra sus valores finales.
//
// NUNCA se eliminan:
//   - param/call (impdig imprime; leerdig/leercad consumen una entrada, y
//     quitarlas cambiaría qué valor recibe la siguiente lectura)
//   - una división cuyo divisor no es una constante distinta de 0, porque
//     puede detener el programa con "División entre cero"
public class PasadaCodigoMuerto implements Pasada {

    public String nombre()      { return "muerto"; }
    public String descripcion() { return "Eliminacion de codigo muerto (vida global)"; }

    public boolean aplicar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        AnalisisVida vida = AnalisisVida.calcular(codigo);
        TreeMap<Integer, String> muertas = new TreeMap<>();   // índice -> motivo

        for (BloqueBasico b : vida.bloques) {
            Set<String> vivas = new HashSet<>(vida.salida.get(b.numero));
            // nombre -> texto de una asignación posterior (en el bloque) sin uso intermedio
            Map<String, String> reasignadas = new HashMap<>();

            for (int i = b.fin; i >= b.inicio; i--) {
                Cuadruplo c = codigo.get(i);
                String d = AnalisisCuadruplo.definida(codigo, i);

                if (d != null && !vivas.contains(d) && eliminable(c)) {
                    muertas.put(i, reasignadas.containsKey(d)
                            ? d + " se vuelve a asignar (" + reasignadas.get(d) + ") antes de usarse"
                            : d + " no esta viva: su valor nunca se usa despues");
                    reasignadas.put(d, c.aTresDirecciones().trim());
                    continue;   // eliminada: sus operandos no se vuelven vivos
                }
                if (d != null) {
                    vivas.remove(d);
                    reasignadas.put(d, c.op.equals("param")
                            ? codigo.get(i + 1).arg1 + "(" + d + ")"      // ej. leerdig(n)
                            : c.aTresDirecciones().trim());
                }
                for (String u : AnalisisCuadruplo.usos(codigo, i)) {
                    vivas.add(u);
                    reasignadas.remove(u);
                }
            }
        }

        if (muertas.isEmpty()) return false;
        // Bitácora en orden del programa; luego se borra de atrás hacia adelante
        for (Map.Entry<Integer, String> e : muertas.entrySet()) {
            int i = e.getKey();
            ctx.registrar(BloqueBasico.bloqueDe(vida.bloques, i), codigo.get(i), null, e.getValue());
        }
        for (int i : muertas.descendingKeySet()) codigo.remove(i);
        return true;
    }

    // Solo asignaciones sin efectos secundarios
    private static boolean eliminable(Cuadruplo c) {
        if (c.op.equals("param")) return false;   // lectura: consume una entrada
        if (c.op.equals("/")) {
            return ContextoOptimizacion.esEntero(c.arg2) && Integer.parseInt(c.arg2) != 0;
        }
        return c.op.equals("=") || AnalisisCuadruplo.esBinario(c) || AnalisisCuadruplo.esUnario(c);
    }
}
