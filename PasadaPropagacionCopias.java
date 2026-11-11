import java.util.*;

// PasadaPropagacionCopias.java — Propagación de copias (LOCAL, por bloque).
//
// 1. Fusión temporal → variable. El generador traduce  x := expr  como
//        t2 = 4 * t1            x = 4 * t1
//        x  = t2          →     (eliminada)
//    Se aplica solo si la copia va inmediatamente después y el temporal no se
//    usa en ningún otro lugar (los temporales se asignan una sola vez).
//
// 2. Propagación hacia adelante. Después de  x = y  (y es un nombre), los usos
//    de x se sustituyen por y mientras ni x ni y se redefinan en el bloque:
//        y = x                  y = x
//        t1 = y + 1     →       t1 = x + 1
//    Así la copia puede quedar muerta (la eliminará la etapa 6).
//
// Las copias de constantes ( x = 5 ) son trabajo de la propagación de constantes.
public class PasadaPropagacionCopias implements Pasada {

    public String nombre()      { return "copias"; }
    public String descripcion() { return "Propagacion de copias (local)"; }

    public boolean aplicar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = fusionar(codigo, ctx);
        cambio |= propagar(codigo, ctx);
        return cambio;
    }

    // ── 1. t = expr ; x = t   →   x = expr ────────────────────────────────────
    private boolean fusionar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = false;
        List<BloqueBasico> bloques = BloqueBasico.dividir(codigo);
        // De arriba hacia abajo (la bitácora queda en orden del programa).
        // Tras borrar, se recalculan los bloques porque los índices se recorren.
        for (int i = 0; i + 1 < codigo.size(); i++) {
            Cuadruplo def   = codigo.get(i);
            Cuadruplo copia = codigo.get(i + 1);
            String t = def.resultado;
            if (AnalisisCuadruplo.definida(codigo, i) == null || def.op.equals("param")) continue;
            if (!ctx.esTemporal(t)) continue;
            if (!copia.op.equals("=") || !copia.arg1.equals(t) || copia.resultado.equals(t)) continue;
            if (BloqueBasico.bloqueDe(bloques, i) != BloqueBasico.bloqueDe(bloques, i + 1)) continue;
            if (AnalisisCuadruplo.contarUsos(codigo, t) != 1) continue;

            int bloque = BloqueBasico.bloqueDe(bloques, i);
            Cuadruplo antesDef = AnalisisCuadruplo.copia(def);
            def.resultado = copia.resultado;
            ctx.registrar(bloque, antesDef, def, "el temporal " + t + " solo se copiaba a " + copia.resultado);
            ctx.registrar(bloque, copia, null, "copia innecesaria tras fusionar " + t);
            codigo.remove(i + 1);
            bloques = BloqueBasico.dividir(codigo);
            cambio = true;
        }
        return cambio;
    }

    // ── 2. x = y ; ... x ...   →   ... y ... ─────────────────────────────────
    private boolean propagar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = false;
        for (BloqueBasico b : BloqueBasico.dividir(codigo)) {
            Map<String, String> copias = new HashMap<>();   // x -> y

            for (int i = b.inicio; i <= b.fin; i++) {
                Cuadruplo c = codigo.get(i);
                Cuadruplo antes = AnalisisCuadruplo.copia(c);
                List<String> sustituidas = new ArrayList<>();

                if (AnalisisCuadruplo.usaArg1(codigo, i) && copias.containsKey(c.arg1)) {
                    sustituidas.add(c.arg1 + " es copia de " + copias.get(c.arg1));
                    c.arg1 = copias.get(c.arg1);
                }
                if (AnalisisCuadruplo.usaArg2(codigo, i) && copias.containsKey(c.arg2)) {
                    sustituidas.add(c.arg2 + " es copia de " + copias.get(c.arg2));
                    c.arg2 = copias.get(c.arg2);
                }
                if (!sustituidas.isEmpty()) {
                    ctx.registrar(b.numero, antes, c, String.join(", ", sustituidas));
                    cambio = true;
                }

                String def = AnalisisCuadruplo.definida(codigo, i);
                if (def != null) {
                    // def cambió: ya no es copia de nadie, y nadie es copia de def
                    copias.remove(def);
                    copias.values().removeIf(v -> v.equals(def));
                    if (c.op.equals("=") && !ContextoOptimizacion.esConstante(c.arg1)
                            && !c.arg1.equals(def)) {
                        copias.put(def, c.arg1);
                    }
                }
            }
        }
        return cambio;
    }
}
