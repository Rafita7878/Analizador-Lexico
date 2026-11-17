import java.util.*;

// PasadaMirilla.java — Optimización de mirilla sobre saltos y control de flujo.
// Revisa ventanas pequeñas del código alrededor de los saltos:
//
//  1. ifFalse con condición constante
//        ifFalse falso goto L      →  goto L         (siempre salta)
//        ifFalse verdadero goto L  →  (eliminada)    (nunca salta)
//
//  2. Salto a salto: si en L solo hay otro salto, se va directo al final
//        goto L1  ...  L1: goto L2   →   goto L2  ...  L1: goto L2
//     (también para ifFalse x goto L1)
//
//  3. Salto a la siguiente instrucción: si entre el salto y su etiqueta
//     solo hay etiquetas, el salto no hace nada
//        goto L1                      (eliminada)
//      L1:                      →   L1:
//
//  4. Código inalcanzable: se eliminan los bloques básicos a los que no se
//     llega desde el bloque de entrada siguiendo el grafo de flujo
//     (por ejemplo, la rama "entonces" después de convertir un ifFalse
//     falso en goto).
//
//  5. Etiquetas sin uso: una 'etiq' que ningún goto/ifFalse referencia.
//
// El ciclo mientras normal NO se toca: su etiqueta de inicio sigue siendo el
// destino del goto de regreso, y ese goto no salta a la instrucción siguiente.
public class PasadaMirilla implements Pasada {

    public String nombre()      { return "mirilla"; }
    public String descripcion() { return "Mirilla: saltos, codigo inalcanzable y etiquetas"; }

    public boolean aplicar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = false;
        cambio |= ifFalseConstante(codigo, ctx);
        cambio |= saltoASalto(codigo, ctx);
        cambio |= saltoASiguiente(codigo, ctx);
        cambio |= codigoInalcanzable(codigo, ctx);
        cambio |= etiquetasSinUso(codigo, ctx);
        return cambio;
    }

    // ── 1. ifFalse con condición constante ───────────────────────────────────
    private boolean ifFalseConstante(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = false;
        for (int i = 0; i < codigo.size(); i++) {
            Cuadruplo c = codigo.get(i);
            if (!c.op.equals("ifFalse") || !ContextoOptimizacion.esBooleano(c.arg1)) continue;
            int bloque = BloqueBasico.bloqueDe(BloqueBasico.dividir(codigo), i);
            Cuadruplo antes = AnalisisCuadruplo.copia(c);
            if (c.arg1.equals("falso")) {
                c.op = "goto"; c.arg1 = "";
                ctx.registrar(bloque, antes, c, "la condicion siempre es falsa: el salto siempre ocurre");
            } else {
                codigo.remove(i);
                i--;
                ctx.registrar(bloque, antes, null, "la condicion siempre es verdadera: el salto nunca ocurre");
            }
            cambio = true;
        }
        return cambio;
    }

    // ── 2. Salto a salto ─────────────────────────────────────────────────────
    private boolean saltoASalto(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = false;
        List<BloqueBasico> bloques = BloqueBasico.dividir(codigo);
        for (int i = 0; i < codigo.size(); i++) {
            Cuadruplo c = codigo.get(i);
            if (!BloqueBasico.esSalto(c)) continue;

            // Seguir la cadena L1 -> L2 -> ... (con protección contra ciclos)
            String destino = c.resultado;
            Set<String> visitadas = new HashSet<>();
            visitadas.add(destino);
            while (true) {
                Cuadruplo siguiente = primeraInstruccionEn(codigo, destino);
                if (siguiente == null || !siguiente.op.equals("goto")) break;
                if (!visitadas.add(siguiente.resultado)) break;
                destino = siguiente.resultado;
            }
            if (!destino.equals(c.resultado)) {
                Cuadruplo antes = AnalisisCuadruplo.copia(c);
                String original = c.resultado;
                c.resultado = destino;
                ctx.registrar(BloqueBasico.bloqueDe(bloques, i), antes, c,
                        "en " + original + " solo hay un goto " + destino);
                cambio = true;
            }
        }
        return cambio;
    }

    // Primera instrucción que no es etiqueta a partir de la etiqueta L
    private static Cuadruplo primeraInstruccionEn(List<Cuadruplo> codigo, String etiqueta) {
        for (int i = 0; i < codigo.size(); i++) {
            Cuadruplo c = codigo.get(i);
            if (c.op.equals("etiq") && c.resultado.equals(etiqueta)) {
                for (int k = i + 1; k < codigo.size(); k++) {
                    if (!codigo.get(k).op.equals("etiq")) return codigo.get(k);
                }
                return null;
            }
        }
        return null;
    }

    // ── 3. Salto a la instrucción siguiente ──────────────────────────────────
    private boolean saltoASiguiente(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = false;
        for (int i = 0; i < codigo.size(); i++) {
            Cuadruplo c = codigo.get(i);
            if (!BloqueBasico.esSalto(c)) continue;
            boolean llegaSolo = false;
            for (int k = i + 1; k < codigo.size() && codigo.get(k).op.equals("etiq"); k++) {
                if (codigo.get(k).resultado.equals(c.resultado)) { llegaSolo = true; break; }
            }
            if (!llegaSolo) continue;
            int bloque = BloqueBasico.bloqueDe(BloqueBasico.dividir(codigo), i);
            codigo.remove(i);
            ctx.registrar(bloque, c, null,
                    "salta a " + c.resultado + ", que es la instruccion siguiente");
            i--;
            cambio = true;
        }
        return cambio;
    }

    // ── 4. Código inalcanzable ───────────────────────────────────────────────
    private boolean codigoInalcanzable(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        List<BloqueBasico> bloques = BloqueBasico.dividir(codigo);
        if (bloques.isEmpty()) return false;

        boolean[] alcanzado = new boolean[bloques.size()];
        Deque<Integer> pendientes = new ArrayDeque<>();
        pendientes.add(0);
        alcanzado[0] = true;
        while (!pendientes.isEmpty()) {
            for (int s : bloques.get(pendientes.poll()).sucesores) {
                if (!alcanzado[s]) { alcanzado[s] = true; pendientes.add(s); }
            }
        }

        boolean cambio = false;
        // de atrás hacia adelante para que borrar no mueva los rangos pendientes
        for (int k = bloques.size() - 1; k >= 0; k--) {
            if (alcanzado[k]) continue;
            BloqueBasico b = bloques.get(k);
            for (int i = b.inicio; i <= b.fin; i++) {
                ctx.registrar(b.numero, codigo.get(i), null,
                        "bloque B" + b.numero + " inalcanzable desde la entrada");
            }
            codigo.subList(b.inicio, b.fin + 1).clear();
            cambio = true;
        }
        return cambio;
    }

    // ── 5. Etiquetas sin uso ─────────────────────────────────────────────────
    private boolean etiquetasSinUso(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        Set<String> usadas = new HashSet<>();
        for (Cuadruplo c : codigo) if (BloqueBasico.esSalto(c)) usadas.add(c.resultado);

        boolean cambio = false;
        for (int i = 0; i < codigo.size(); i++) {
            Cuadruplo c = codigo.get(i);
            if (!c.op.equals("etiq") || usadas.contains(c.resultado)) continue;
            int bloque = BloqueBasico.bloqueDe(BloqueBasico.dividir(codigo), i);
            codigo.remove(i);
            ctx.registrar(bloque, c, null, "ningun salto llega a " + c.resultado);
            i--;
            cambio = true;
        }
        return cambio;
    }
}
