import java.util.*;

// BloqueBasico.java
// Un bloque básico es una secuencia de cuádruplos consecutivos en la que el
// control entra solo por la primera instrucción (el LÍDER) y sale solo por la
// última. Algoritmo de líderes (Aho, sec. 8.4.1), adaptado a los cuádruplos
// que produce GeneradorCodigoIntermedio:
//
//   1. El primer cuádruplo del programa es líder.
//   2. Todo cuádruplo 'etiq' es líder (es destino de algún salto).
//   3. Todo cuádruplo que sigue inmediatamente a un 'goto' o a un 'ifFalse'
//      es líder.
//
// Cada bloque va desde su líder hasta el cuádruplo anterior al siguiente líder.
//
// Sucesores (grafo de flujo):
//   - termina en goto L      → el bloque que empieza en L
//   - termina en ifFalse x L → el bloque que empieza en L  y  el bloque siguiente
//   - cualquier otro caso    → el bloque siguiente (si existe)
// Un bloque sin sucesores es la salida del programa.
public class BloqueBasico {

    public int numero;
    public int inicio;             // índice del líder en la lista
    public int fin;                // índice del último cuádruplo (inclusivo)
    public String motivoLider;     // por qué 'inicio' es líder
    public List<Integer> sucesores    = new ArrayList<>();
    public List<Integer> predecesores = new ArrayList<>();

    public BloqueBasico(int numero, int inicio, String motivoLider) {
        this.numero      = numero;
        this.inicio      = inicio;
        this.motivoLider = motivoLider;
    }

    public static boolean esSalto(Cuadruplo c) {
        return c.op.equals("goto") || c.op.equals("ifFalse");
    }

    // ── División en bloques ───────────────────────────────────────────────────
    public static List<BloqueBasico> dividir(List<Cuadruplo> codigo) {
        List<BloqueBasico> bloques = new ArrayList<>();
        if (codigo.isEmpty()) return bloques;

        // 1. Marcar líderes con su motivo (si hay varios motivos se concatenan)
        Map<Integer, String> lideres = new TreeMap<>();
        agregarMotivo(lideres, 0, "primer cuadruplo");
        for (int i = 0; i < codigo.size(); i++) {
            Cuadruplo c = codigo.get(i);
            if (c.op.equals("etiq")) {
                agregarMotivo(lideres, i, "etiqueta " + c.resultado);
            }
            if (esSalto(c) && i + 1 < codigo.size()) {
                agregarMotivo(lideres, i + 1, "sigue al " + c.op + " de [" + i + "]");
            }
        }

        // 2. Formar los bloques: de cada líder al anterior del siguiente líder
        List<Integer> indices = new ArrayList<>(lideres.keySet());
        for (int k = 0; k < indices.size(); k++) {
            BloqueBasico b = new BloqueBasico(k, indices.get(k), lideres.get(indices.get(k)));
            b.fin = (k + 1 < indices.size()) ? indices.get(k + 1) - 1 : codigo.size() - 1;
            bloques.add(b);
        }

        // 3. Grafo de flujo: etiqueta → bloque que la contiene como líder
        Map<String, Integer> bloqueDeEtiqueta = new HashMap<>();
        for (BloqueBasico b : bloques) {
            Cuadruplo primero = codigo.get(b.inicio);
            if (primero.op.equals("etiq")) bloqueDeEtiqueta.put(primero.resultado, b.numero);
        }

        for (BloqueBasico b : bloques) {
            Cuadruplo ultimo = codigo.get(b.fin);
            boolean haySiguiente = b.numero + 1 < bloques.size();

            if (ultimo.op.equals("goto")) {
                agregarArista(bloques, b.numero, bloqueDeEtiqueta.get(ultimo.resultado));
            } else if (ultimo.op.equals("ifFalse")) {
                agregarArista(bloques, b.numero, bloqueDeEtiqueta.get(ultimo.resultado));
                if (haySiguiente) agregarArista(bloques, b.numero, b.numero + 1);
            } else if (haySiguiente) {
                agregarArista(bloques, b.numero, b.numero + 1);
            }
        }
        return bloques;
    }

    private static void agregarMotivo(Map<Integer, String> lideres, int i, String motivo) {
        lideres.merge(i, motivo, (a, b) -> a + "; " + b);
    }

    private static void agregarArista(List<BloqueBasico> bloques, int desde, Integer hacia) {
        if (hacia == null) return;   // salto a una etiqueta inexistente: no debería ocurrir
        BloqueBasico origen = bloques.get(desde);
        if (!origen.sucesores.contains(hacia)) origen.sucesores.add(hacia);
        BloqueBasico destino = bloques.get(hacia);
        if (!destino.predecesores.contains(desde)) destino.predecesores.add(desde);
    }

    // Bloque al que pertenece el cuádruplo de índice i
    public static int bloqueDe(List<BloqueBasico> bloques, int i) {
        for (BloqueBasico b : bloques) {
            if (i >= b.inicio && i <= b.fin) return b.numero;
        }
        return -1;
    }

    // ── Salida ────────────────────────────────────────────────────────────────
    public static String aTexto(List<BloqueBasico> bloques, List<Cuadruplo> codigo) {
        StringBuilder sb = new StringBuilder();
        String divisor = "-".repeat(65) + "\n";

        sb.append(String.format(" %-4s | %-9s | %-12s | %-12s | %s%n",
                "BLQ", "RANGO", "PREDECESOR", "SUCESOR", "LIDER PORQUE"));
        sb.append(divisor);
        for (BloqueBasico b : bloques) {
            sb.append(String.format(" %-4s | %-9s | %-12s | %-12s | %s%n",
                    "B" + b.numero,
                    "[" + b.inicio + ".." + b.fin + "]",
                    nombres(b.predecesores, "ENTRADA"),
                    nombres(b.sucesores, "SALIDA"),
                    b.motivoLider));
        }
        sb.append(divisor).append("\n");

        // Código agrupado por bloque
        for (BloqueBasico b : bloques) {
            sb.append("  B").append(b.numero).append(":\n");
            for (int i = b.inicio; i <= b.fin; i++) {
                sb.append(String.format("   %3d %s%n", i, codigo.get(i).aTresDirecciones()));
            }
        }
        return sb.toString();
    }

    private static String nombres(List<Integer> lista, String siVacia) {
        if (lista.isEmpty()) return siVacia;
        StringBuilder sb = new StringBuilder();
        for (int n : lista) {
            if (sb.length() > 0) sb.append(",");
            sb.append("B").append(n);
        }
        return sb.toString();
    }
}
