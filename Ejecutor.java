import java.util.*;

// Ejecutor.java
// Interpreta y EJECUTA de verdad el programa RAFI, usando el mismo AST que
// ya validó AnalizadorSemantico. Solo se llama si el programa no tiene
// errores sintácticos ni semánticos.
//
// A diferencia del semántico (que solo calcula TIPOS), este calcula VALORES
// reales: hace las sumas de verdad, evalúa las condiciones de verdad, y
// cuando encuentra leerdig/leercad, le pregunta al usuario por teclado y
// guarda lo que responda en la variable correspondiente.
public class Ejecutor {

    // Valores reales en tiempo de ejecución: nombre de variable -> su valor actual
    private Map<String, Object> valores = new HashMap<>();
    private Scanner teclado;

    public Ejecutor(Map<String, String[]> tablaSimbolos, Scanner teclado) {
        this.teclado = teclado;
        // Inicializa cada variable con su valor por defecto según su tipo
        for (Map.Entry<String, String[]> entrada : tablaSimbolos.entrySet()) {
            String tipo = entrada.getValue()[0];
            switch (tipo) {
                case "entero":   valores.put(entrada.getKey(), 0);     break;
                case "cadena":   valores.put(entrada.getKey(), "");    break;
                case "booleano": valores.put(entrada.getKey(), false); break;
            }
        }
    }

    public void ejecutar(NodoArbol raiz) {
        NodoArbol sentencias = buscarHijoTipo(raiz, "SENTENCIAS");
        ejecutarSentencias(sentencias);
    }

    private NodoArbol buscarHijoTipo(NodoArbol nodo, String tipo) {
        if (nodo == null) return null;
        for (NodoArbol h : nodo.hijos) {
            if (h.tipo.equals(tipo)) return h;
        }
        return null;
    }

    private void ejecutarSentencias(NodoArbol contenedor) {
        if (contenedor == null) return;
        for (NodoArbol s : contenedor.hijos) {
            ejecutarSentencia(s);
        }
    }

    private void ejecutarSentencia(NodoArbol s) {
        switch (s.tipo) {
            case "ASIGNACION":  ejecutarAsignacion(s);  break;
            case "CONDICIONAL": ejecutarCondicional(s); break;
            case "CICLO":       ejecutarCiclo(s);       break;
            case "IMPRESION":   ejecutarImpresion(s);   break;
            case "LECTURA":     ejecutarLectura(s);     break;
            default: break;
        }
    }

    private void ejecutarAsignacion(NodoArbol s) {
        NodoArbol idNodo   = buscarHijoTipo(s, "ID");
        NodoArbol exprNodo = buscarHijoTipo(s, "EXPR_LOGICA");
        if (idNodo == null || exprNodo == null) return;
        valores.put(idNodo.valor, evaluar(exprNodo));
    }

    private void ejecutarCondicional(NodoArbol s) {
        NodoArbol cond = buscarHijoTipo(s, "EXPR_LOGICA");
        boolean resultado = (Boolean) evaluar(cond);
        if (resultado) {
            ejecutarSentencias(buscarHijoTipo(s, "SENTENCIAS_ENTONCES"));
        } else {
            NodoArbol sino = buscarHijoTipo(s, "SENTENCIAS_SINO");
            if (sino != null) ejecutarSentencias(sino);
        }
    }

    private void ejecutarCiclo(NodoArbol s) {
        NodoArbol cond   = buscarHijoTipo(s, "EXPR_LOGICA");
        NodoArbol cuerpo = buscarHijoTipo(s, "SENTENCIAS_CICLO");
        while ((Boolean) evaluar(cond)) {
            ejecutarSentencias(cuerpo);
        }
    }

    private void ejecutarImpresion(NodoArbol s) {
        NodoArbol exprNodo = buscarHijoTipo(s, "EXPR_LOGICA");
        if (exprNodo == null) return;
        System.out.println(evaluar(exprNodo));
    }

    // Aquí es donde realmente se le pregunta al usuario por teclado.
    // leerdig exige un ENTERO válido (repite hasta que lo sea);
    // leercad acepta cualquier texto tal cual lo escriba.
    private void ejecutarLectura(NodoArbol s) {
        if (s.hijos.isEmpty()) return;
        String instruccion = s.hijos.get(0).tipo; // "LEERDIG" o "LEERCAD"
        NodoArbol idNodo = buscarHijoTipo(s, "ID");
        if (idNodo == null) return;
        String nombre = idNodo.valor;

        if (instruccion.equals("LEERDIG")) {
            Integer valor = null;
            while (valor == null) {
                System.out.print("Ingresa un valor ENTERO para '" + nombre + "': ");
                String entrada = teclado.nextLine().trim();
                try {
                    valor = Integer.parseInt(entrada);
                } catch (NumberFormatException e) {
                    System.out.println("  Eso no es un número entero válido, intenta de nuevo.");
                }
            }
            valores.put(nombre, valor);
        } else { // LEERCAD
            System.out.print("Ingresa una CADENA para '" + nombre + "': ");
            String entrada = teclado.nextLine();
            valores.put(nombre, entrada);
        }
    }

    // Evalúa el VALOR real de cualquier nodo de expresión (mismo recorrido
    // que tipoDe en AnalizadorSemantico, pero calculando en vez de solo
    // verificando tipos).
    private Object evaluar(NodoArbol n) {
        if (n == null) return null;

        switch (n.tipo) {

            case "ID":
                return valores.get(n.valor);

            case "CENT":
                return Integer.parseInt(n.valor);

            case "CLIT": {
                String texto = n.valor;
                if (texto.length() >= 2 && texto.startsWith("\"") && texto.endsWith("\"")) {
                    return texto.substring(1, texto.length() - 1);
                }
                return texto;
            }

            case "VERDADERO": return Boolean.TRUE;
            case "FALSO":     return Boolean.FALSE;

            case "NO":
                return !((Boolean) evaluar(n.hijos.get(0)));

            case "EXPR_LOGICA": {
                if (n.hijos.size() == 1) return evaluar(n.hijos.get(0));
                boolean resultado = (Boolean) evaluar(n.hijos.get(0));
                for (int i = 1; i < n.hijos.size(); i += 2) {
                    String op = n.hijos.get(i).valor; // "y" u "o"
                    boolean der = (Boolean) evaluar(n.hijos.get(i + 1));
                    resultado = op.equals("y") ? (resultado && der) : (resultado || der);
                }
                return resultado;
            }

            case "EXPR_RELACIONAL": {
                if (n.hijos.size() == 1) return evaluar(n.hijos.get(0));
                Object izq = evaluar(n.hijos.get(0));
                Object der = evaluar(n.hijos.get(2));
                String op  = n.hijos.get(1).valor;

                int cmp;
                if (izq instanceof Integer)      cmp = ((Integer) izq).compareTo((Integer) der);
                else if (izq instanceof String)  cmp = ((String) izq).compareTo((String) der);
                else                              cmp = ((Boolean) izq).compareTo((Boolean) der);

                switch (op) {
                    case "<":  return cmp < 0;
                    case ">":  return cmp > 0;
                    case "=":  return cmp == 0;
                    case "<=": return cmp <= 0;
                    case ">=": return cmp >= 0;
                    case "<>": return cmp != 0;
                    default:   return false;
                }
            }

            case "EXPRESION":
            case "TERMINO": {
                List<NodoArbol> operandos  = new ArrayList<>();
                List<String>    operadores = new ArrayList<>();
                for (NodoArbol h : n.hijos) {
                    if (h.tipo.equals("OPERADOR")) operadores.add(h.valor);
                    else operandos.add(h);
                }
                // Un solo operando → no hay operador real, el valor pasa tal cual
                // (puede ser cadena o booleano, no forzosamente entero)
                if (operandos.size() == 1) {
                    return evaluar(operandos.get(0));
                }
                int resultado = (Integer) evaluar(operandos.get(0));
                for (int i = 0; i < operadores.size(); i++) {
                    NodoArbol nodoSiguiente = operandos.get(i + 1);
                    int val = (Integer) evaluar(nodoSiguiente);
                    switch (operadores.get(i)) {
                        case "+": resultado += val; break;
                        case "-": resultado -= val; break;
                        case "*": resultado *= val; break;
                        case "/":
                            if (val == 0) {
                                throw new ErrorEjecucion(
                                        "División entre cero al evaluar la expresión",
                                        nodoSiguiente.linea, nodoSiguiente.columna);
                            }
                            resultado /= val;
                            break;
                    }
                }
                return resultado;
            }

            default:
                if (!n.hijos.isEmpty()) return evaluar(n.hijos.get(0));
                return null;
        }
    }
}