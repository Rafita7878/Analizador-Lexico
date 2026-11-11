import java.util.*;

// InterpreteCuadruplos.java
// Ejecuta una lista de cuádruplos (original u optimizada) para VERIFICAR que
// la optimización no cambió el comportamiento observable del programa.
//
// Imita la semántica de Ejecutor.java:
//   - aritmética int de Java (división truncada, desbordamiento con vuelta)
//   - 'y' / 'o' sin cortocircuito, comparaciones con compareTo
//   - división entre cero → ErrorEjecucion con el mismo mensaje
//   - impdig/impcad imprimen con toString (un booleano sale como true/false)
//   - cada variable empieza con el valor por defecto de su tipo
//
// No usa el teclado: las lecturas toman sus valores de una FuenteEntradas.
// Lo único observable es lo que se imprime (ningún archivo muestra los valores
// finales de las variables), así que el resultado es la lista de líneas
// impresas, más el error de ejecución si lo hubo.
public class InterpreteCuadruplos {

    public static final int LIMITE_PASOS = 1_000_000;
    public static final String MENSAJE_DIV_CERO = "División entre cero al evaluar la expresión";

    // ── Fuente de valores para leerdig / leercad ──────────────────────────────
    public interface FuenteEntradas {
        String nombre();
        String siguiente(boolean esEntero); // nunca null
    }

    // Valores de una lista (ej. progfte.in); al agotarse usa 0 / ""
    public static FuenteEntradas deLista(String nombre, List<String> valores) {
        return new FuenteEntradas() {
            int i = 0;
            public String nombre() { return nombre; }
            public String siguiente(boolean esEntero) {
                if (i < valores.size()) return valores.get(i++);
                return esEntero ? "0" : "";
            }
        };
    }

    // Vector automático: recorre los arreglos de forma cíclica
    public static FuenteEntradas vector(String nombre, int[] enteros, String[] cadenas) {
        return new FuenteEntradas() {
            int ie = 0, ic = 0;
            public String nombre() { return nombre; }
            public String siguiente(boolean esEntero) {
                if (esEntero) return String.valueOf(enteros[ie++ % enteros.length]);
                return cadenas[ic++ % cadenas.length];
            }
        };
    }

    // Vectores de respaldo cuando no existe progfte.in
    public static List<FuenteEntradas> vectoresAutomaticos() {
        return List.of(
                vector("auto: ceros",  new int[]{0},               new String[]{""}),
                vector("auto: unos",   new int[]{1},               new String[]{"abc"}),
                vector("auto: mixto",  new int[]{-5, 10, 3, 0, 7}, new String[]{"hola", "", "zz"}));
    }

    // ── Resultado de una ejecución ────────────────────────────────────────────
    public static class Resultado {
        public List<String> salida = new ArrayList<>();
        public String error = null;            // mensaje de ErrorEjecucion, si lo hubo
        public int instrucciones = 0;          // cuádruplos ejecutados (sin contar 'etiq')
        public boolean limiteAlcanzado = false;

        // Salida comparable: líneas impresas + error final
        public List<String> observable() {
            List<String> r = new ArrayList<>(salida);
            if (error != null) r.add("ERROR: " + error);
            if (limiteAlcanzado) r.add("LIMITE DE " + LIMITE_PASOS + " PASOS ALCANZADO");
            return r;
        }
    }

    // ── Estado ────────────────────────────────────────────────────────────────
    private final Map<String, String[]> tablaSimbolos;
    private Map<String, Object> valores;

    public InterpreteCuadruplos(Map<String, String[]> tablaSimbolos) {
        this.tablaSimbolos = tablaSimbolos;
    }

    public Resultado ejecutar(List<Cuadruplo> codigo, FuenteEntradas entradas) {
        Resultado res = new Resultado();
        valores = new HashMap<>();
        for (Map.Entry<String, String[]> e : tablaSimbolos.entrySet()) {
            switch (e.getValue()[0]) {
                case "entero":   valores.put(e.getKey(), 0);     break;
                case "cadena":   valores.put(e.getKey(), "");    break;
                case "booleano": valores.put(e.getKey(), false); break;
            }
        }

        Map<String, Integer> etiquetas = new HashMap<>();
        for (int i = 0; i < codigo.size(); i++) {
            if (codigo.get(i).op.equals("etiq")) etiquetas.put(codigo.get(i).resultado, i);
        }

        Deque<String> parametros = new ArrayDeque<>();
        int pc = 0;
        try {
            while (pc < codigo.size()) {
                Cuadruplo c = codigo.get(pc);
                if (c.op.equals("etiq")) { pc++; continue; }

                if (++res.instrucciones > LIMITE_PASOS) {
                    res.instrucciones = LIMITE_PASOS;
                    res.limiteAlcanzado = true;
                    break;
                }

                switch (c.op) {
                    case "goto":
                        pc = etiquetas.get(c.resultado);
                        continue;
                    case "ifFalse":
                        if (!(Boolean) valor(c.arg1)) { pc = etiquetas.get(c.resultado); continue; }
                        break;
                    case "=":
                        valores.put(c.resultado, valor(c.arg1));
                        break;
                    case "menos":
                        valores.put(c.resultado, -((Integer) valor(c.arg1)));
                        break;
                    case "no":
                        valores.put(c.resultado, !((Boolean) valor(c.arg1)));
                        break;
                    case "param":
                        parametros.addLast(c.arg1);
                        break;
                    case "call":
                        ejecutarLlamada(c, parametros, entradas, res);
                        break;
                    default:
                        valores.put(c.resultado, operar(c.op, valor(c.arg1), valor(c.arg2)));
                }
                pc++;
            }
        } catch (ErrorEjecucion e) {
            res.error = e.getMessage();
        }
        return res;
    }

    private void ejecutarLlamada(Cuadruplo c, Deque<String> parametros,
                                 FuenteEntradas entradas, Resultado res) {
        int n = Integer.parseInt(c.arg2);
        List<String> args = new ArrayList<>();
        for (int k = 0; k < n; k++) args.add(0, parametros.removeLast());
        String arg = args.get(0);

        switch (c.arg1) {
            case "impdig":
            case "impcad":
                res.salida.add(String.valueOf(valor(arg)));
                break;
            case "leerdig": {
                // Igual que Ejecutor: descarta entradas que no son entero válido
                Integer leido = null;
                for (int intentos = 0; leido == null && intentos < 1000; intentos++) {
                    try { leido = Integer.parseInt(entradas.siguiente(true).trim()); }
                    catch (NumberFormatException e) { /* siguiente valor */ }
                }
                valores.put(arg, leido == null ? 0 : leido);
                break;
            }
            case "leercad":
                valores.put(arg, entradas.siguiente(false));
                break;
        }
    }

    // Valor de un operando: constante literal o nombre (variable/temporal)
    private Object valor(String s) {
        if (ContextoOptimizacion.esEntero(s))  return Integer.parseInt(s);
        if (ContextoOptimizacion.esCadena(s))  return s.substring(1, s.length() - 1);
        if (s.equals("verdadero")) return Boolean.TRUE;
        if (s.equals("falso"))     return Boolean.FALSE;
        return valores.get(s);
    }

    // Operación binaria con la misma semántica que Ejecutor (también la usará
    // el plegado de constantes, para que ambos calculen exactamente igual)
    @SuppressWarnings("unchecked")
    public static Object operar(String op, Object a, Object b) {
        switch (op) {
            case "+": return (Integer) a + (Integer) b;
            case "-": return (Integer) a - (Integer) b;
            case "*": return (Integer) a * (Integer) b;
            case "/":
                if ((Integer) b == 0) throw new ErrorEjecucion(MENSAJE_DIV_CERO, -1, -1);
                return (Integer) a / (Integer) b;
            case "y": return (Boolean) a && (Boolean) b;
            case "o": return (Boolean) a || (Boolean) b;
        }
        int cmp = ((Comparable<Object>) a).compareTo(b);
        switch (op) {
            case "<":  return cmp < 0;
            case ">":  return cmp > 0;
            case "==": return cmp == 0;
            case "<=": return cmp <= 0;
            case ">=": return cmp >= 0;
            case "<>": return cmp != 0;
        }
        throw new IllegalArgumentException("Operador desconocido: " + op);
    }
}
