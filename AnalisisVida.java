import java.util.*;

// AnalisisVida.java — Análisis de variables vivas (global, sobre el grafo de flujo).
//
// Una variable está VIVA en un punto si su valor actual puede usarse más
// adelante en algún camino de ejecución antes de volver a asignarse.
//
// Por cada bloque B se calculan primero dos conjuntos locales, recorriendo B
// de arriba hacia abajo:
//   USE[B] = variables que B usa ANTES de asignarlas en B (las lee "de fuera")
//   DEF[B] = variables que B asigna (incluye leerdig/leercad: param x + call)
//
// Y luego las ecuaciones de flujo de datos (análisis hacia atrás):
//   OUT[B] = unión de IN[S] para cada sucesor S de B
//   IN[B]  = USE[B] ∪ (OUT[B] − DEF[B])
//
// Se parte de IN = OUT = ∅ y se repiten las ecuaciones sobre todos los bloques
// (de atrás hacia adelante, que converge más rápido) hasta que ningún conjunto
// cambie. Los ciclos (mientras) son la razón de iterar: el IN de la cabecera
// del ciclo alimenta el OUT del último bloque del cuerpo.
//
// Al salir del programa no hay nada vivo: ningún archivo de salida muestra los
// valores finales de las variables, lo único observable es lo que se imprime.
public class AnalisisVida {

    public List<BloqueBasico> bloques;
    public List<Set<String>> use = new ArrayList<>(), def = new ArrayList<>();
    public List<Set<String>> entrada = new ArrayList<>(), salida = new ArrayList<>();
    public int iteraciones = 0;

    public static AnalisisVida calcular(List<Cuadruplo> codigo) {
        AnalisisVida a = new AnalisisVida();
        a.bloques = BloqueBasico.dividir(codigo);

        // ── Conjuntos locales USE y DEF ──
        for (BloqueBasico b : a.bloques) {
            Set<String> u = new TreeSet<>(), d = new TreeSet<>();
            for (int i = b.inicio; i <= b.fin; i++) {
                for (String nombre : AnalisisCuadruplo.usos(codigo, i)) {
                    if (!d.contains(nombre)) u.add(nombre);
                }
                String definida = AnalisisCuadruplo.definida(codigo, i);
                if (definida != null) d.add(definida);
            }
            a.use.add(u);
            a.def.add(d);
            a.entrada.add(new TreeSet<>());
            a.salida.add(new TreeSet<>());
        }

        // ── Iteración hasta punto fijo ──
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            a.iteraciones++;
            for (int k = a.bloques.size() - 1; k >= 0; k--) {
                Set<String> out = new TreeSet<>();
                for (int s : a.bloques.get(k).sucesores) out.addAll(a.entrada.get(s));

                Set<String> in = new TreeSet<>(out);
                in.removeAll(a.def.get(k));
                in.addAll(a.use.get(k));

                if (!out.equals(a.salida.get(k)) || !in.equals(a.entrada.get(k))) {
                    a.salida.set(k, out);
                    a.entrada.set(k, in);
                    cambio = true;
                }
            }
        }
        return a;
    }

    public String aTexto() {
        StringBuilder sb = new StringBuilder();
        // Ancho de cada columna según el conjunto más largo
        int wu = 3, wd = 3, wi = 18;
        for (int k = 0; k < bloques.size(); k++) {
            wu = Math.max(wu, conjunto(use.get(k)).length());
            wd = Math.max(wd, conjunto(def.get(k)).length());
            wi = Math.max(wi, conjunto(entrada.get(k)).length());
        }
        String fila = " %-4s | %-6s | %-" + wu + "s | %-" + wd + "s | %-" + wi + "s | %s%n";
        sb.append(String.format(fila, "BLQ", "SUCES.", "USE", "DEF", "IN (vivas entrada)", "OUT (vivas salida)"));
        sb.append("-".repeat(40 + wu + wd + wi)).append("\n");
        for (BloqueBasico b : bloques) {
            int k = b.numero;
            StringBuilder suc = new StringBuilder();
            for (int s : b.sucesores) suc.append(suc.length() > 0 ? "," : "").append("B").append(s);
            sb.append(String.format(fila, "B" + k, suc.length() == 0 ? "-" : suc,
                    conjunto(use.get(k)), conjunto(def.get(k)),
                    conjunto(entrada.get(k)), conjunto(salida.get(k))));
        }
        sb.append("  Iteraciones hasta el punto fijo: ").append(iteraciones)
          .append(" (la ultima solo confirma que nada cambio)\n");
        return sb.toString();
    }

    private static String conjunto(Set<String> s) {
        return "{" + String.join(",", s) + "}";
    }
}
