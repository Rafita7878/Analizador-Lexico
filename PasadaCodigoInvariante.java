import java.util.*;

// PasadaCodigoInvariante.java — Código invariante en ciclos (mientras).
//
// Detección de ciclos: una arista de regreso B → H (H aparece antes que B,
// como el "goto Linicio" del mientras) define un ciclo natural con cabecera H:
// H más todos los bloques que llegan a B sin pasar por H. Se procesan del
// ciclo más interno al más externo; lo que sale de un ciclo interno puede
// volver a salir del externo en la siguiente revisión.
//
// Una instrucción  d = a op b  (o  d = menos a, d = no a, d = a)  es INVARIANTE
// si cada operando es constante o no se asigna en ningún bloque del ciclo
// (contando leerdig/leercad como asignación).
//
// Hay dos formas de sacarla, antes de la etiqueta de la cabecera:
//
//  A. MOVER la instrucción completa, si además:
//       - d se asigna una sola vez dentro del ciclo,
//       - d no está viva al entrar a la cabecera (el ciclo no lee el valor
//         anterior de d),
//       - d no está viva en ninguna salida del ciclo (si el ciclo se ejecuta
//         cero veces, d debe conservar su valor anterior),
//       - la operación no puede fallar.
//
//  B. Si falla alguna condición sobre d (por ejemplo d se imprime después del
//     ciclo), se saca solo la OPERACIÓN a un temporal nuevo y dentro del ciclo
//     queda una copia:
//           tN = a * b            (antes del ciclo, una sola vez)
//         L1: ...
//           k = tN                (cada vuelta: copia en lugar de multiplicar)
//     Esto es necesario porque la propagación de copias fusiona  t = a * b ;
//     k = t  en  k = a * b , así que el invariante suele tener como destino una
//     variable del usuario y no un temporal.
//
// Nunca se saca una operación que pueda fallar (división cuyo divisor no es
// una constante distinta de 0): si el ciclo no se ejecuta, el programa
// original no fallaría.
//
// El código se inserta antes de la etiqueta de la cabecera solo si la única
// entrada al ciclo desde fuera es el bloque anterior, que llega sin saltar.
public class PasadaCodigoInvariante implements Pasada {

    public String nombre()      { return "invariante"; }
    public String descripcion() { return "Codigo invariante en ciclos (mientras)"; }

    private static class Ciclo {
        BloqueBasico cabecera;
        Set<Integer> bloques = new TreeSet<>();
    }

    public boolean aplicar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = false;
        // Se saca una instrucción a la vez y se recalcula todo, porque al sacar
        // una pueden volverse invariantes las que dependían de ella.
        for (int guardia = 0; guardia < 200 && sacarUna(codigo, ctx); guardia++) cambio = true;
        return cambio;
    }

    private boolean sacarUna(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        AnalisisVida vida = AnalisisVida.calcular(codigo);
        List<BloqueBasico> bloques = vida.bloques;

        List<Ciclo> ciclos = detectarCiclos(bloques);
        ciclos.sort(Comparator.comparingInt(c -> c.bloques.size()));   // internos primero

        for (Ciclo ciclo : ciclos) {
            if (!entradaUnica(ciclo, bloques, codigo)) continue;
            BloqueBasico h = ciclo.cabecera;
            String nombreCiclo = codigo.get(h.inicio).op.equals("etiq")
                    ? codigo.get(h.inicio).resultado : "B" + h.numero;

            // Cuántas veces se asigna cada nombre dentro del ciclo
            Map<String, Integer> asignaciones = new HashMap<>();
            for (int k : ciclo.bloques) {
                BloqueBasico b = bloques.get(k);
                for (int i = b.inicio; i <= b.fin; i++) {
                    String d = AnalisisCuadruplo.definida(codigo, i);
                    if (d != null) asignaciones.merge(d, 1, Integer::sum);
                }
            }

            for (int k : ciclo.bloques) {
                BloqueBasico b = bloques.get(k);
                for (int i = b.inicio; i <= b.fin; i++) {
                    Cuadruplo c = codigo.get(i);
                    boolean expresion = AnalisisCuadruplo.esBinario(c) || AnalisisCuadruplo.esUnario(c);
                    if (!expresion && !c.op.equals("=")) continue;
                    if (!operandosInvariantes(codigo, i, asignaciones)) continue;
                    if (!noPuedeFallar(c)) continue;
                    String d = c.resultado;

                    boolean moverCompleta = asignaciones.get(d) == 1
                            && !vida.entrada.get(h.numero).contains(d)
                            && !vivaEnSalidas(d, ciclo, bloques, vida);

                    if (moverCompleta) {
                        codigo.remove(i);
                        codigo.add(h.inicio, c);
                        ctx.registrar(b.numero, c, c, "invariante: se mueve antes de " + nombreCiclo
                                + "; " + descripcionOperandos(c) + " en el ciclo");
                        return true;
                    }
                    if (expresion) {
                        String t = nuevoTemporal(codigo, ctx);
                        Cuadruplo antes = AnalisisCuadruplo.copia(c);
                        Cuadruplo calculo = new Cuadruplo(c.op, c.arg1, c.arg2, t);
                        c.op = "="; c.arg1 = t; c.arg2 = "";
                        codigo.add(h.inicio, calculo);
                        ctx.registrar(b.numero, null, calculo, "invariante: " + antes.aTresDirecciones().trim()
                                + " se calcula una vez antes de " + nombreCiclo);
                        ctx.registrar(b.numero, antes, c, "solo sale la operacion: " + d + " "
                                + porQueNoSeMueve(d, asignaciones, h, ciclo, bloques, vida));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── Ciclos naturales a partir de aristas de regreso ──────────────────────
    private static List<Ciclo> detectarCiclos(List<BloqueBasico> bloques) {
        Map<Integer, Ciclo> porCabecera = new LinkedHashMap<>();
        for (BloqueBasico b : bloques) {
            for (int s : b.sucesores) {
                if (s > b.numero) continue;           // no es arista de regreso
                Ciclo ciclo = porCabecera.computeIfAbsent(s, x -> {
                    Ciclo nuevo = new Ciclo();
                    nuevo.cabecera = bloques.get(x);
                    nuevo.bloques.add(x);
                    return nuevo;
                });
                // Bloques que llegan a b sin pasar por la cabecera
                Deque<Integer> pendientes = new ArrayDeque<>();
                if (ciclo.bloques.add(b.numero)) pendientes.add(b.numero);
                while (!pendientes.isEmpty()) {
                    for (int p : bloques.get(pendientes.poll()).predecesores) {
                        if (ciclo.bloques.add(p)) pendientes.add(p);
                    }
                }
            }
        }
        return new ArrayList<>(porCabecera.values());
    }

    // La única entrada desde fuera debe ser el bloque anterior, sin saltar a la cabecera
    private static boolean entradaUnica(Ciclo ciclo, List<BloqueBasico> bloques, List<Cuadruplo> codigo) {
        BloqueBasico h = ciclo.cabecera;
        for (int p : h.predecesores) {
            if (ciclo.bloques.contains(p)) continue;
            if (p != h.numero - 1) return false;
            // Si el bloque anterior llega SALTANDO a la etiqueta, se brincaría el código insertado
            Cuadruplo ultimo = codigo.get(bloques.get(p).fin);
            Cuadruplo primero = codigo.get(h.inicio);
            if (BloqueBasico.esSalto(ultimo) && primero.op.equals("etiq")
                    && ultimo.resultado.equals(primero.resultado)) return false;
        }
        return true;
    }

    private static boolean operandosInvariantes(List<Cuadruplo> codigo, int i, Map<String, Integer> asignaciones) {
        Cuadruplo c = codigo.get(i);
        if (!ContextoOptimizacion.esConstante(c.arg1) && asignaciones.containsKey(c.arg1)) return false;
        if (AnalisisCuadruplo.esBinario(c)
                && !ContextoOptimizacion.esConstante(c.arg2) && asignaciones.containsKey(c.arg2)) return false;
        return true;
    }

    private static boolean noPuedeFallar(Cuadruplo c) {
        if (!c.op.equals("/")) return true;
        return ContextoOptimizacion.esEntero(c.arg2) && Integer.parseInt(c.arg2) != 0;
    }

    // ¿d está viva al entrar a algún bloque fuera del ciclo al que se sale desde el ciclo?
    private static boolean vivaEnSalidas(String d, Ciclo ciclo, List<BloqueBasico> bloques, AnalisisVida vida) {
        for (int k : ciclo.bloques) {
            for (int s : bloques.get(k).sucesores) {
                if (!ciclo.bloques.contains(s) && vida.entrada.get(s).contains(d)) return true;
            }
        }
        return false;
    }

    private static String porQueNoSeMueve(String d, Map<String, Integer> asignaciones, BloqueBasico h,
                                          Ciclo ciclo, List<BloqueBasico> bloques, AnalisisVida vida) {
        if (asignaciones.get(d) > 1) return "se asigna varias veces en el ciclo";
        if (vivaEnSalidas(d, ciclo, bloques, vida)) return "se usa despues del ciclo (si no hay vueltas debe conservar su valor)";
        return "se usa en el ciclo antes de asignarse";
    }

    private static String descripcionOperandos(Cuadruplo c) {
        List<String> nombres = new ArrayList<>();
        if (!ContextoOptimizacion.esConstante(c.arg1)) nombres.add(c.arg1);
        if (!c.arg2.isEmpty() && !ContextoOptimizacion.esConstante(c.arg2)) nombres.add(c.arg2);
        if (nombres.isEmpty()) return "sus operandos son constantes y no cambian";
        return String.join(" y ", nombres) + (nombres.size() == 1 ? " no cambia" : " no cambian");
    }

    // Temporal nuevo: uno más que el mayor tN usado (y que no sea variable del usuario)
    private static String nuevoTemporal(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        int mayor = 0;
        for (Cuadruplo c : codigo) {
            for (String s : new String[]{c.arg1, c.arg2, c.resultado}) {
                if (ctx.esTemporal(s)) mayor = Math.max(mayor, Integer.parseInt(s.substring(1)));
            }
        }
        String t;
        do { t = "t" + (++mayor); } while (ctx.esVariableUsuario(t));
        return t;
    }
}
