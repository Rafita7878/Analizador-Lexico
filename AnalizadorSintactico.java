import java.util.*;
public class AnalizadorSintactico {

    private List<Token> tokens;
    private int pos;
    private List<ErrorSintactico> errores;
    private Map<String, String[]> tablaSimbolos;

    // ── Flag para rastrear el último renglón donde se reportó un error ───────
    // Evita reportar más de un error por renglón
    private int ultimoRenglonConError = -1;

    public AnalizadorSintactico(List<Token> tokens, Map<String, String[]> tablaSimbolos) {
        this.tokens        = tokens;
        this.tablaSimbolos = tablaSimbolos;
        this.pos           = 0;
        this.errores       = new ArrayList<>();
    }

    // ── Registra un error SOLO si no se ha reportado uno en ese renglón ──────
    // Garantiza máximo un error por renglón, sin mensajes ambiguos
    private void registrarError(String mensaje, int linea) {
        if (linea != ultimoRenglonConError) {
            errores.add(new ErrorSintactico(mensaje, linea));
            ultimoRenglonConError = linea;
        }
    }

    // ── Devuelve true si el análisis tuvo al menos un error ──────────────────
    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    private Token tokenActual() {
        if (pos < tokens.size()) return tokens.get(pos);
        return new Token("EOF", "EOF", "", 0, -1);
    }

    private Token consumir() {
        Token t = tokenActual();
        pos++;
        return t;
    }

    private Token esperado(String tipo) {
        Token t = tokenActual();
        if (t.tipo.equals(tipo)) return consumir();
        registrarError("Se esperaba '" + tipo + "' pero se encontró '" + t.lexema + "'", t.linea);
        return t;
    }

    private boolean esTokenActual(String tipo) {
        return tokenActual().tipo.equals(tipo);
    }

    // ── Sincronizar: saltar tokens hasta el siguiente ; ───────────────────────
    private void sincronizar() {
        while (!esTokenActual("PC")  &&
               !esTokenActual("FIN") &&
               !esTokenActual("EOF")) {
            consumir();
        }
        if (esTokenActual("PC")) consumir();
    }

    // ────────────────────────────────────────────────────────────────────────
    public NodoArbol analizar() {
        NodoArbol raiz = parsePrograma();
        if (!esTokenActual("EOF") && !esTokenActual("FIN")) {
            Token t = tokenActual();
            registrarError("Tokens inesperados al final del programa: '" + t.lexema + "'", t.linea);
        }
        return raiz;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  PROGRAMA → rafi ID decl DECLARACIONES inicio SENTENCIAS fin
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parsePrograma() {
        NodoArbol nodo = new NodoArbol("PROGRAMA");

        Token tokRafi = esperado("PROG");
        nodo.agregarHijo(new NodoArbol("PROG", tokRafi.lexema));

        Token tokNombre = esperado("ID");
        nodo.agregarHijo(new NodoArbol("ID", tokNombre.lexema));

        if (esTokenActual("DECL")) {
            Token tokDecl = consumir();
            NodoArbol nodoDecl = new NodoArbol("DECLARACIONES");
            nodoDecl.agregarHijo(new NodoArbol("DECL", tokDecl.lexema));
            parseDeclaraciones(nodoDecl);
            nodo.agregarHijo(nodoDecl);
        }

        Token tokInicio = esperado("INICIO");
        nodo.agregarHijo(new NodoArbol("INICIO", tokInicio.lexema));

        NodoArbol nodoSentencias = new NodoArbol("SENTENCIAS");
        parseSentencias(nodoSentencias);
        nodo.agregarHijo(nodoSentencias);

        Token tokFin = esperado("FIN");
        nodo.agregarHijo(new NodoArbol("FIN", tokFin.lexema));

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  DECLARACIONES → (TIPO ID (, ID)* ;)*
    // ────────────────────────────────────────────────────────────────────────
    private void parseDeclaraciones(NodoArbol padre) {
        while (esTokenActual("TIPO")) {
            NodoArbol nodoDecl = new NodoArbol("DECLARACION");

            Token tokTipo = consumir();
            nodoDecl.agregarHijo(new NodoArbol("TIPO", tokTipo.lexema));

            Token tokId = esperado("ID");
            nodoDecl.agregarHijo(new NodoArbol("ID", tokId.lexema));

            while (esTokenActual("COMA")) {
                consumir();
                Token tokIdExtra = esperado("ID");
                nodoDecl.agregarHijo(new NodoArbol("ID", tokIdExtra.lexema));
            }

            esperado("PC");
            nodoDecl.agregarHijo(new NodoArbol("PC", ";"));
            padre.agregarHijo(nodoDecl);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SENTENCIAS
    // ────────────────────────────────────────────────────────────────────────
    private void parseSentencias(NodoArbol padre) {
        while (!esTokenActual("FIN")  &&
               !esTokenActual("SINO") &&
               !esTokenActual("EOF")) {

            int posAntes = pos;
            NodoArbol sentencia = parseSentencia();

            if (sentencia != null) {
                padre.agregarHijo(sentencia);
            } else {
                Token t = tokenActual();
                // Un solo error por renglón con mensaje claro
                registrarError("Sentencia no válida: '" + t.lexema +
                               "' no es una instrucción reconocida del lenguaje", t.linea);
                sincronizar();
            }

            if (pos == posAntes) consumir();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SENTENCIA
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseSentencia() {
        Token t = tokenActual();

        if (t.nombre.equals("IDENTIFICADOR")) return parseAsignacion();
        if (t.tipo.equals("SI"))              return parseCondicional();
        if (t.tipo.equals("MIENTRAS"))        return parseCiclo();
        if (t.tipo.equals("IMPDIG") ||
            t.tipo.equals("IMPCAD"))          return parseImpresion();
        if (t.tipo.equals("LEERDIG") ||
            t.tipo.equals("LEERCAD"))         return parseLectura();

        return null;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  ASIGNACION → ID := EXPRESION ;
    //  Un solo error por renglón — si falla sincroniza y sigue
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseAsignacion() {
        NodoArbol nodo = new NodoArbol("ASIGNACION");
        Token tokId = consumir();

        // Variable no declarada → un error, saltar toda la sentencia
        if (!tablaSimbolos.containsKey(tokId.lexema)) {
            registrarError("Variable '" + tokId.lexema + "' no está declarada en la sección decl",
                           tokId.linea);
            sincronizar();
            return nodo;
        }
        nodo.agregarHijo(new NodoArbol("ID", tokId.lexema));

        // Falta := → un error, saltar toda la sentencia
        if (!esTokenActual("ASIG")) {
            Token t = tokenActual();
            registrarError("Falta ':=' en la asignación de '" + tokId.lexema + "'", t.linea);
            sincronizar();
            return nodo;
        }
        consumir();
        nodo.agregarHijo(new NodoArbol("ASIG", ":="));

        // Expresión vacía → un error, saltar
        if (esTokenActual("PC")) {
            registrarError("La asignación de '" + tokId.lexema + "' no tiene valor (expresión vacía)",
                           tokId.linea);
            consumir();
            return nodo;
        }

        NodoArbol expr = parseExpresion();
        nodo.agregarHijo(expr);

        // Falta ; al final
        if (!esTokenActual("PC")) {
            Token t = tokenActual();
            registrarError("Falta ';' al final de la asignación de '" + tokId.lexema + "'", t.linea);
            sincronizar();
        } else {
            consumir();
            nodo.agregarHijo(new NodoArbol("PC", ";"));
        }

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CONDICIONAL → si ( EXPRESION ) entonces SENTENCIAS [sino SENTENCIAS] fin
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseCondicional() {
        NodoArbol nodo = new NodoArbol("CONDICIONAL");
        consumir();
        nodo.agregarHijo(new NodoArbol("SI", "si"));

        if (!esTokenActual("PARENTESIS_ABIERTO")) {
            registrarError("Falta '(' después de 'si'", tokenActual().linea);
        } else consumir();

        NodoArbol condicion = parseExpresion();
        nodo.agregarHijo(condicion);

        if (!esTokenActual("PARENTESIS_CERRADO")) {
            registrarError("Falta ')' para cerrar la condición del 'si'", tokenActual().linea);
        } else consumir();

        if (!esTokenActual("ENTONCES")) {
            registrarError("Falta 'entonces' después de la condición del 'si'", tokenActual().linea);
        } else {
            consumir();
            nodo.agregarHijo(new NodoArbol("ENTONCES", "entonces"));
        }

        NodoArbol sentenciasVerdad = new NodoArbol("SENTENCIAS_ENTONCES");
        parseSentencias(sentenciasVerdad);
        nodo.agregarHijo(sentenciasVerdad);

        if (esTokenActual("SINO")) {
            consumir();
            nodo.agregarHijo(new NodoArbol("SINO", "sino"));
            NodoArbol sentenciasFalso = new NodoArbol("SENTENCIAS_SINO");
            parseSentencias(sentenciasFalso);
            nodo.agregarHijo(sentenciasFalso);
        }

        if (!esTokenActual("FIN")) {
            registrarError("Falta 'fin' para cerrar el bloque 'si'", tokenActual().linea);
        } else {
            consumir();
            nodo.agregarHijo(new NodoArbol("FIN", "fin"));
        }

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CICLO → mientras ( EXPRESION ) hacer SENTENCIAS fin
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseCiclo() {
        NodoArbol nodo = new NodoArbol("CICLO");
        consumir();
        nodo.agregarHijo(new NodoArbol("MIENTRAS", "mientras"));

        if (!esTokenActual("PARENTESIS_ABIERTO")) {
            registrarError("Falta '(' después de 'mientras'", tokenActual().linea);
        } else consumir();

        NodoArbol condicion = parseExpresion();
        nodo.agregarHijo(condicion);

        if (!esTokenActual("PARENTESIS_CERRADO")) {
            registrarError("Falta ')' para cerrar la condición del 'mientras'", tokenActual().linea);
        } else consumir();

        if (!esTokenActual("HACER")) {
            registrarError("Falta 'hacer' después de la condición del 'mientras'", tokenActual().linea);
        } else {
            consumir();
            nodo.agregarHijo(new NodoArbol("HACER", "hacer"));
        }

        NodoArbol cuerpo = new NodoArbol("SENTENCIAS_CICLO");
        parseSentencias(cuerpo);
        nodo.agregarHijo(cuerpo);

        if (!esTokenActual("FIN")) {
            registrarError("Falta 'fin' para cerrar el bloque 'mientras'", tokenActual().linea);
        } else {
            consumir();
            nodo.agregarHijo(new NodoArbol("FIN", "fin"));
        }

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  IMPRESION → (impdig | impcad) ( EXPRESION ) ;
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseImpresion() {
        NodoArbol nodo = new NodoArbol("IMPRESION");
        Token tokImp = consumir();
        nodo.agregarHijo(new NodoArbol(tokImp.tipo, tokImp.lexema));

        if (!esTokenActual("PARENTESIS_ABIERTO")) {
            registrarError("Falta '(' después de '" + tokImp.lexema + "'", tokImp.linea);
            sincronizar();
            return nodo;
        }
        consumir();

        NodoArbol expr = parseExpresion();
        nodo.agregarHijo(expr);

        if (!esTokenActual("PARENTESIS_CERRADO")) {
            registrarError("Falta ')' para cerrar '" + tokImp.lexema + "'", tokenActual().linea);
            sincronizar();
            return nodo;
        }
        consumir();

        if (!esTokenActual("PC")) {
            registrarError("Falta ';' después de '" + tokImp.lexema + "(...)'", tokenActual().linea);
            sincronizar();
        } else consumir();

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  LECTURA → (leerdig | leercad) ( ID ) ;
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseLectura() {
        NodoArbol nodo = new NodoArbol("LECTURA");
        Token tokLeer = consumir();
        nodo.agregarHijo(new NodoArbol(tokLeer.tipo, tokLeer.lexema));

        if (!esTokenActual("PARENTESIS_ABIERTO")) {
            registrarError("Falta '(' después de '" + tokLeer.lexema + "'", tokLeer.linea);
            sincronizar();
            return nodo;
        }
        consumir();

        Token tokId = esperado("ID");
        if (!tablaSimbolos.containsKey(tokId.lexema)) {
            registrarError("Variable '" + tokId.lexema + "' no está declarada en la sección decl",
                           tokId.linea);
        } else {
            nodo.agregarHijo(new NodoArbol("ID", tokId.lexema));
        }

        if (!esTokenActual("PARENTESIS_CERRADO")) {
            registrarError("Falta ')' para cerrar '" + tokLeer.lexema + "'", tokenActual().linea);
            sincronizar();
            return nodo;
        }
        consumir();

        if (!esTokenActual("PC")) {
            registrarError("Falta ';' después de '" + tokLeer.lexema + "(...)'", tokenActual().linea);
            sincronizar();
        } else consumir();

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  EXPRESION → TERMINO ((+ | -) TERMINO)*
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseExpresion() {
        NodoArbol nodo = new NodoArbol("EXPRESION");

        // Operando izquierdo ausente
        if (esTokenActual("SUMA") || esTokenActual("RESTA")) {
            Token t = tokenActual();
            registrarError("Falta operando izquierdo antes del operador '" + t.lexema + "'", t.linea);
            consumir();
            return nodo;
        }

        nodo.agregarHijo(parseTermino());

        while (esTokenActual("SUMA") || esTokenActual("RESTA")) {
            Token tokOp = consumir();
            nodo.agregarHijo(new NodoArbol("OPERADOR", tokOp.lexema));

            // Doble operador
            if (esTokenActual("SUMA")           || esTokenActual("RESTA") ||
                esTokenActual("MULTIPLICACION") || esTokenActual("DIVISION")) {
                Token t = tokenActual();
                registrarError("Doble operador en expresión: '" + tokOp.lexema +
                               "' seguido de '" + t.lexema + "'", t.linea);
                consumir();
                return nodo;
            }

            // Operando derecho ausente
            if (esTokenActual("PC")               || esTokenActual("PARENTESIS_CERRADO") ||
                esTokenActual("FIN")              || esTokenActual("EOF")) {
                Token t = tokenActual();
                registrarError("Falta operando derecho después del operador '" +
                               tokOp.lexema + "'", t.linea);
                return nodo;
            }

            nodo.agregarHijo(parseTermino());
        }

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  TERMINO → FACTOR ((* | /) FACTOR)*
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseTermino() {
        NodoArbol nodo = new NodoArbol("TERMINO");
        nodo.agregarHijo(parseFactor());

        while (esTokenActual("MULTIPLICACION") || esTokenActual("DIVISION")) {
            Token tokOp = consumir();
            nodo.agregarHijo(new NodoArbol("OPERADOR", tokOp.lexema));

            // Doble operador
            if (esTokenActual("MULTIPLICACION") || esTokenActual("DIVISION") ||
                esTokenActual("SUMA")           || esTokenActual("RESTA")) {
                Token t = tokenActual();
                registrarError("Doble operador en expresión: '" + tokOp.lexema +
                               "' seguido de '" + t.lexema + "'", t.linea);
                consumir();
                return nodo;
            }

            // Operando derecho ausente
            if (esTokenActual("PC")               || esTokenActual("PARENTESIS_CERRADO") ||
                esTokenActual("FIN")              || esTokenActual("EOF")) {
                Token t = tokenActual();
                registrarError("Falta operando derecho después del operador '" +
                               tokOp.lexema + "'", t.linea);
                return nodo;
            }

            nodo.agregarHijo(parseFactor());
        }

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  FACTOR → ID | CENT | CLIT | ( EXPRESION )
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseFactor() {
        Token t = tokenActual();

        if (t.nombre.equals("IDENTIFICADOR")) {
            consumir();
            if (!tablaSimbolos.containsKey(t.lexema)) {
                registrarError("Variable '" + t.lexema + "' no está declarada en la sección decl",
                               t.linea);
            }
            return new NodoArbol("ID", t.lexema);
        }

        if (t.tipo.equals("CENT")) {
            consumir();
            return new NodoArbol("CENT", t.lexema);
        }

        if (t.tipo.equals("CLIT")) {
            consumir();
            return new NodoArbol("CLIT", t.lexema);
        }

        if (t.tipo.equals("PARENTESIS_ABIERTO")) {
            consumir();
            NodoArbol expr = parseExpresion();
            if (!esTokenActual("PARENTESIS_CERRADO")) {
                registrarError("Paréntesis sin cerrar — falta ')' en la expresión", t.linea);
            } else {
                consumir();
            }
            return expr;
        }

        // Nada válido encontrado
        registrarError("Se encontró '" + t.lexema + "' donde se esperaba un valor o variable",
                       t.linea);
        consumir();
        return new NodoArbol("ERROR", t.lexema);
    }

    public List<ErrorSintactico> getErrores() {
        return errores;
    }
}