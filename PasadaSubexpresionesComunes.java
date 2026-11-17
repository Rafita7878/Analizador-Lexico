import java.util.*;

// PasadaSubexpresionesComunes.java — Eliminación de subexpresiones comunes (LOCAL).
// Dentro de un bloque básico, si una expresión  a op b  ya se calculó y está
// guardada en h, y desde entonces ni a, ni b, ni h se han redefinido, el
// segundo cálculo se cambia por una copia:
//
//      x = a + b                x = a + b
//      ...               →      ...
//      y = a + b                y = x
//
// Operadores conmutativos (+ * y o == <>): a + b y b + a son la misma
// expresión, así que la llave se forma con los operandos ordenados.
// También aplica a los unarios (menos a, no a).
//
// Una redefinición invalida la expresión: una asignación a 'a' o 'b', o un
// leerdig/leercad (param + call) que los sobrescribe. Si se redefine h, la
// expresión ya no está disponible en h.
//
// Es local: la tabla empieza vacía en cada bloque, porque a un bloque se puede
// llegar por caminos donde la expresión no se calculó.
// Reutilizar  a / b  es seguro: si la primera división fallara, la segunda
// nunca se alcanzaría.
public class PasadaSubexpresionesComunes implements Pasada {

    private static final Set<String> CONMUTATIVOS = Set.of("+", "*", "y", "o", "==", "<>");

    public String nombre()      { return "cse"; }
    public String descripcion() { return "Subexpresiones comunes (local)"; }

    public boolean aplicar(List<Cuadruplo> codigo, ContextoOptimizacion ctx) {
        boolean cambio = false;
        for (BloqueBasico b : BloqueBasico.dividir(codigo)) {
            // llave de la expresión -> nombre que guarda su valor
            Map<String, String> disponibles = new LinkedHashMap<>();
            // llave -> operandos de la expresión (para invalidar)
            Map<String, List<String>> operandos = new HashMap<>();

            for (int i = b.inicio; i <= b.fin; i++) {
                Cuadruplo c = codigo.get(i);
                String llave = llave(c);

                if (llave != null && disponibles.containsKey(llave)) {
                    String h = disponibles.get(llave);
                    Cuadruplo antes = AnalisisCuadruplo.copia(c);
                    c.op = "="; c.arg1 = h; c.arg2 = "";
                    ctx.registrar(b.numero, antes, c, textoExpresion(antes) + " ya se calculo en " + h);
                    cambio = true;
                    llave = null;   // ahora es una copia, no una expresión
                }

                String def = AnalisisCuadruplo.definida(codigo, i);
                if (def != null) {
                    // Se pierden las expresiones que usan def o que estaban guardadas en def
                    Iterator<Map.Entry<String, String>> it = disponibles.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, String> e = it.next();
                        if (e.getValue().equals(def) || operandos.get(e.getKey()).contains(def)) {
                            it.remove();
                        }
                    }
                    // La nueva expresión queda disponible en def, salvo que se use a sí
                    // misma (i = i + 1: después de asignar, i + 1 ya no vale lo mismo)
                    if (llave != null && !c.arg1.equals(def) && !c.arg2.equals(def)) {
                        disponibles.put(llave, def);
                        operandos.put(llave, List.of(c.arg1, c.arg2));
                    }
                }
            }
        }
        return cambio;
    }

    // Llave normalizada de una expresión, o null si el cuádruplo no es expresión
    private static String llave(Cuadruplo c) {
        if (AnalisisCuadruplo.esUnario(c)) return c.op + "|" + c.arg1;
        if (!AnalisisCuadruplo.esBinario(c)) return null;
        String a = c.arg1, b = c.arg2;
        if (CONMUTATIVOS.contains(c.op) && a.compareTo(b) > 0) { String t = a; a = b; b = t; }
        return a + "|" + c.op + "|" + b;
    }

    private static String textoExpresion(Cuadruplo c) {
        if (AnalisisCuadruplo.esUnario(c)) return c.op + " " + c.arg1;
        return c.arg1 + " " + c.op + " " + c.arg2;
    }
}
