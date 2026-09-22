import java.util.*;

// AnalizadorSemantico.java
// Recorre el AST (NodoArbol) que produce AnalizadorSintactico y verifica:
//  - Que ninguna variable se declare dos veces.
//  - Que toda variable usada esté declarada.
//  - Que los tipos de las expresiones sean consistentes (verificación de tipos).
//  - Que las asignaciones respeten el tipo de la variable.
//  - Que impdig/impcad reciban el tipo correcto (entero/cadena).
//  - Que leerdig/leercad lean sobre una variable del tipo correcto.
//  - Que las condiciones de 'si' y 'mientras' sean de tipo booleano.
public class AnalizadorSemantico {

    // Tabla de símbolos propia, construida desde el AST:
    // nombre de variable -> [tipo, línea de declaración]
    private Map<String, String[]> tabla = new LinkedHashMap<>();

    private List<ErrorSemantico> errores = new ArrayList<>();

    // ── Punto de entrada ──────────────────────────────────────────────────────
    public void analizar(NodoArbol raiz) {
        if (raiz == null) return;

        NodoArbol declaraciones = buscarHijoTipo(raiz, "DECLARACIONES");
        if (declaraciones != null) {
            verificarDeclaraciones(declaraciones);
        }

        NodoArbol sentencias = buscarHijoTipo(raiz, "SENTENCIAS");
        visitarSentencias(sentencias);
    }

    public List<ErrorSemantico> getErrores() {
        return errores;
    }

    public Map<String, String[]> getTablaSimbolos() {
        return tabla;
    }

    private void registrarError(String mensaje, NodoArbol n) {
        int linea   = (n != null) ? n.linea   : -1;
        int columna = (n != null) ? n.columna : -1;
        errores.add(new ErrorSemantico(mensaje, linea, columna));
    }

    // Determina si un nodo de expresión es, en el fondo, la constante literal 0
    // (bajando por los nodos-envoltorio de un solo hijo: EXPR_LOGICA, EXPR_RELACIONAL,
    // EXPRESION, TERMINO). Si en el camino hay más de un hijo (una operación real),
    // ya no es un literal simple y se considera que no es "0" de forma detectable.
    private boolean esLiteralCero(NodoArbol n) {
        if (n == null) return false;
        if (n.tipo.equals("CENT")) return n.valor.equals("0");
        if (n.hijos.size() == 1) return esLiteralCero(n.hijos.get(0));
        return false;
    }

    // Busca el primer hijo directo de 'nodo' cuyo tipo sea 'tipo'
    private NodoArbol buscarHijoTipo(NodoArbol nodo, String tipo) {
        if (nodo == null) return null;
        for (NodoArbol h : nodo.hijos) {
            if (h.tipo.equals(tipo)) return h;
        }
        return null;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  DECLARACIONES — construye la tabla y detecta variables duplicadas
    // ────────────────────────────────────────────────────────────────────────
    private void verificarDeclaraciones(NodoArbol declaraciones) {
        for (NodoArbol decl : declaraciones.hijos) {
            if (!decl.tipo.equals("DECLARACION")) continue;

            String tipoActual = null;
            for (NodoArbol hijo : decl.hijos) {
                if (hijo.tipo.equals("TIPO")) {
                    tipoActual = hijo.valor;
                    continue;
                }
                if (hijo.tipo.equals("ID")) {
                    if (tabla.containsKey(hijo.valor)) {
                        registrarError("Variable '" + hijo.valor +
                                "' ya había sido declarada anteriormente", hijo);
                    } else {
                        tabla.put(hijo.valor, new String[]{ tipoActual, String.valueOf(hijo.linea) });
                    }
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SENTENCIAS — recorre cada sentencia y la despacha según su tipo
    // ────────────────────────────────────────────────────────────────────────
    private void visitarSentencias(NodoArbol contenedor) {
        if (contenedor == null) return;
        for (NodoArbol s : contenedor.hijos) {
            visitarSentencia(s);
        }
    }

    private void visitarSentencia(NodoArbol s) {
        switch (s.tipo) {
            case "ASIGNACION":  verificarAsignacion(s); break;
            case "CONDICIONAL": verificarCondicional(s); break;
            case "CICLO":       verificarCiclo(s); break;
            case "IMPRESION":   verificarImpresion(s); break;
            case "LECTURA":     verificarLectura(s); break;
            default: break; // ERROR u otros nodos que el sintáctico ya reportó
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  ASIGNACION: tipo(variable) debe ser igual a tipo(expresión)
    // ────────────────────────────────────────────────────────────────────────
    private void verificarAsignacion(NodoArbol s) {
        NodoArbol idNodo   = buscarHijoTipo(s, "ID");
        NodoArbol exprNodo = buscarHijoTipo(s, "EXPR_LOGICA");
        if (idNodo == null || exprNodo == null) return; // sentencia incompleta, ya reportada antes

        if (!tabla.containsKey(idNodo.valor)) {
            registrarError("Variable '" + idNodo.valor + "' no está declarada", idNodo);
            return;
        }

        String tipoVar  = tabla.get(idNodo.valor)[0];
        String tipoExpr = tipoDe(exprNodo);

        if (!tipoVar.equals(tipoExpr) && !tipoExpr.equals("error")) {
            registrarError("No se puede asignar un valor de tipo '" + tipoExpr +
                    "' a la variable '" + idNodo.valor + "' de tipo '" + tipoVar + "'", exprNodo);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CONDICIONAL (si): condición debe ser booleano; recorre ambas ramas
    // ────────────────────────────────────────────────────────────────────────
    private void verificarCondicional(NodoArbol s) {
        verificarCondicionGeneral(s, "si");
        NodoArbol entonces = buscarHijoTipo(s, "SENTENCIAS_ENTONCES");
        NodoArbol sino     = buscarHijoTipo(s, "SENTENCIAS_SINO");
        visitarSentencias(entonces);
        visitarSentencias(sino);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CICLO (mientras): condición debe ser booleano; recorre el cuerpo
    // ────────────────────────────────────────────────────────────────────────
    private void verificarCiclo(NodoArbol s) {
        verificarCondicionGeneral(s, "mientras");
        NodoArbol cuerpo = buscarHijoTipo(s, "SENTENCIAS_CICLO");
        visitarSentencias(cuerpo);
    }

    private void verificarCondicionGeneral(NodoArbol s, String etiqueta) {
        NodoArbol cond = buscarHijoTipo(s, "EXPR_LOGICA");
        if (cond == null) return;

        String tipo = tipoDe(cond);
        if (!tipo.equals("booleano") && !tipo.equals("error")) {
            registrarError("La condición de '" + etiqueta + "' debe ser de tipo 'booleano' " +
                    "(se encontró '" + tipo + "')", cond);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  IMPRESION: impdig exige entero, impcad exige cadena
    // ────────────────────────────────────────────────────────────────────────
    private void verificarImpresion(NodoArbol s) {
        if (s.hijos.isEmpty()) return;
        String instruccion   = s.hijos.get(0).tipo; // "IMPDIG" o "IMPCAD"
        NodoArbol exprNodo   = buscarHijoTipo(s, "EXPR_LOGICA");
        if (exprNodo == null) return;

        String tipoEsperado = instruccion.equals("IMPDIG") ? "entero" : "cadena";
        String tipoReal     = tipoDe(exprNodo);

        if (!tipoReal.equals(tipoEsperado) && !tipoReal.equals("error")) {
            registrarError("'" + s.hijos.get(0).valor + "' espera un valor de tipo '" + tipoEsperado +
                    "' pero se encontró '" + tipoReal + "'", exprNodo);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  LECTURA: leerdig exige variable entero, leercad exige variable cadena
    // ────────────────────────────────────────────────────────────────────────
    private void verificarLectura(NodoArbol s) {
        if (s.hijos.isEmpty()) return;
        String instruccion = s.hijos.get(0).tipo; // "LEERDIG" o "LEERCAD"
        NodoArbol idNodo    = buscarHijoTipo(s, "ID");
        if (idNodo == null) return; // no declarada, ya se reportó en el sintáctico

        String tipoEsperado = instruccion.equals("LEERDIG") ? "entero" : "cadena";
        String tipoVar = tabla.containsKey(idNodo.valor) ? tabla.get(idNodo.valor)[0] : "error";

        if (!tipoVar.equals(tipoEsperado) && !tipoVar.equals("error")) {
            registrarError("'" + s.hijos.get(0).valor + "' requiere una variable de tipo '" + tipoEsperado +
                    "' pero '" + idNodo.valor + "' es de tipo '" + tipoVar + "'", idNodo);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  tipoDe: calcula recursivamente el tipo de cualquier nodo de expresión
    //  y va registrando los errores de tipo que encuentre en el camino.
    //  Devuelve "entero" | "cadena" | "booleano" | "error"
    // ────────────────────────────────────────────────────────────────────────
    private String tipoDe(NodoArbol n) {
        if (n == null) return "error";

        switch (n.tipo) {

            case "ID":
                if (!tabla.containsKey(n.valor)) {
                    registrarError("Variable '" + n.valor + "' no está declarada", n);
                    return "error";
                }
                return tabla.get(n.valor)[0];

            case "CENT":
                return "entero";

            case "CLIT":
                return "cadena";

            case "VERDADERO":
            case "FALSO":
                return "booleano";

            case "NO": {
                String t = tipoDe(n.hijos.get(0));
                if (!t.equals("booleano") && !t.equals("error")) {
                    registrarError("El operador 'no' requiere un operando de tipo 'booleano' " +
                            "(se encontró '" + t + "')", n);
                    return "error";
                }
                return "booleano";
            }

            case "EXPR_LOGICA": {
                // Un solo hijo → simplemente pasa el tipo hacia arriba
                if (n.hijos.size() == 1) return tipoDe(n.hijos.get(0));

                boolean huboError = false;
                for (NodoArbol h : n.hijos) {
                    if (h.tipo.equals("OP_LOGICO")) continue;
                    String t = tipoDe(h);
                    if (!t.equals("booleano") && !t.equals("error")) {
                        registrarError("Los operadores 'y'/'o' requieren operandos de tipo 'booleano' " +
                                "(se encontró '" + t + "')", h);
                        huboError = true;
                    }
                }
                return huboError ? "error" : "booleano";
            }

            case "EXPR_RELACIONAL": {
                // Sin operador relacional → pasa el tipo aritmético hacia arriba
                if (n.hijos.size() == 1) return tipoDe(n.hijos.get(0));

                String izq = tipoDe(n.hijos.get(0));
                String der = tipoDe(n.hijos.get(2));
                if (!izq.equals(der) && !izq.equals("error") && !der.equals("error")) {
                    registrarError("No se puede comparar un valor de tipo '" + izq +
                            "' con uno de tipo '" + der + "'", n);
                    return "error";
                }
                return "booleano";
            }

            case "EXPRESION":
            case "TERMINO": {
                List<NodoArbol> operandos  = new ArrayList<>();
                List<String>    operadores = new ArrayList<>();
                for (NodoArbol h : n.hijos) {
                    if (h.tipo.equals("OPERADOR")) operadores.add(h.valor);
                    else operandos.add(h);
                }
                if (operandos.isEmpty()) return "error";

                // Un solo operando → EXPRESION/TERMINO son solo envoltorios de la
                // gramática (no hay ningún + - * / real aplicándose), así que el
                // valor pasa hacia arriba tal cual, sea cadena, booleano o entero.
                if (operandos.size() == 1) {
                    return tipoDe(operandos.get(0));
                }

                // Con 2+ operandos sí hay un operador aritmético combinándolos,
                // y esos operadores solo están definidos para 'entero'.
                String tipoResultado = tipoDe(operandos.get(0));
                for (int i = 0; i < operadores.size(); i++) {
                    NodoArbol operandoSiguiente = operandos.get(i + 1);
                    String t = tipoDe(operandoSiguiente);
                    if (!tipoResultado.equals(t) && !tipoResultado.equals("error") && !t.equals("error")) {
                        registrarError("Operación aritmética entre tipos incompatibles: '" +
                                tipoResultado + "' y '" + t + "'", operandoSiguiente);
                        tipoResultado = "error";
                    }
                    // División entre cero detectable en tiempo de compilación:
                    // el divisor es la constante literal 0 (no una variable).
                    if (operadores.get(i).equals("/") && esLiteralCero(operandoSiguiente)) {
                        registrarError("División entre cero: el divisor es la constante '0'", operandoSiguiente);
                        tipoResultado = "error";
                    }
                }

                if (!tipoResultado.equals("entero") && !tipoResultado.equals("error")) {
                    registrarError("Los operadores aritméticos (+, -, *, /) solo son válidos entre " +
                            "valores de tipo 'entero' (se encontró '" + tipoResultado + "')", n);
                    return "error";
                }
                return tipoResultado;
            }

            default:
                // Nodo envoltorio inesperado — intenta bajar por su primer hijo
                if (!n.hijos.isEmpty()) return tipoDe(n.hijos.get(0));
                return "error";
        }
    }
}