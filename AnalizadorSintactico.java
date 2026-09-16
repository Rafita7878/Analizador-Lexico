import java.util.*;
public class AnalizadorSintactico {

    private List<Token> tokens;
    private int pos;
    private List<ErrorSintactico> errores;
    private Map<String, String[]> tablaSimbolos;

    private int ultimoRenglonConError = -1;

    public AnalizadorSintactico(List<Token> tokens, Map<String, String[]> tablaSimbolos) {
        this.tokens        = tokens;
        this.tablaSimbolos = tablaSimbolos;
        this.pos           = 0;
        this.errores       = new ArrayList<>();
    }

    private void registrarError(String mensaje, int linea, int columna) {
        if (linea != ultimoRenglonConError) {
            errores.add(new ErrorSintactico(mensaje, linea, columna));
            ultimoRenglonConError = linea;
        }
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    private Token tokenActual() {
        if (pos < tokens.size()) return tokens.get(pos);
        return new Token("EOF", "EOF", "", 0, 0, -1);
    }

    private Token consumir() {
        Token t = tokenActual();
        pos++;
        return t;
    }

    private Token esperado(String tipo) {
        Token t = tokenActual();
        if (t.tipo.equals(tipo)) return consumir();
        registrarError("Se esperaba '" + tipo + "' pero se encontró '" + t.lexema + "'", t.linea, t.columna);
        return t;
    }

    private boolean esTokenActual(String tipo) {
        return tokenActual().tipo.equals(tipo);
    }

    private void sincronizar() {
        while (!esTokenActual("PC")  &&
                !esTokenActual("FIN") &&
                !esTokenActual("EOF")) {
            consumir();
        }
        if (esTokenActual("PC")) consumir();
    }

    public NodoArbol analizar() {
        NodoArbol raiz = parsePrograma();
        if (!esTokenActual("EOF") && !esTokenActual("FIN")) {
            Token t = tokenActual();
            registrarError("Tokens inesperados al final del programa: '" + t.lexema + "'", t.linea, t.columna);
        }
        return raiz;
    }

    private NodoArbol parsePrograma() {
        NodoArbol nodo = new NodoArbol("PROGRAMA");

        Token tokRafi = esperado("PROG");
        nodo.agregarHijo(new NodoArbol("PROG", tokRafi));

        Token tokNombre = esperado("ID");
        nodo.agregarHijo(new NodoArbol("ID", tokNombre));

        if (esTokenActual("DECL")) {
            Token tokDecl = consumir();
            NodoArbol nodoDecl = new NodoArbol("DECLARACIONES");
            nodoDecl.agregarHijo(new NodoArbol("DECL", tokDecl));
            parseDeclaraciones(nodoDecl);
            nodo.agregarHijo(nodoDecl);
        }

        Token tokInicio = esperado("INICIO");
        nodo.agregarHijo(new NodoArbol("INICIO", tokInicio));

        NodoArbol nodoSentencias = new NodoArbol("SENTENCIAS");
        parseSentencias(nodoSentencias);
        nodo.agregarHijo(nodoSentencias);

        Token tokFin = esperado("FIN");
        nodo.agregarHijo(new NodoArbol("FIN", tokFin));

        return nodo;
    }

    private void parseDeclaraciones(NodoArbol padre) {
        while (esTokenActual("TIPO")) {
            NodoArbol nodoDecl = new NodoArbol("DECLARACION");

            Token tokTipo = consumir();
            nodoDecl.agregarHijo(new NodoArbol("TIPO", tokTipo));

            Token tokId = esperado("ID");
            nodoDecl.agregarHijo(new NodoArbol("ID", tokId));

            while (esTokenActual("COMA")) {
                consumir();
                Token tokIdExtra = esperado("ID");
                nodoDecl.agregarHijo(new NodoArbol("ID", tokIdExtra));
            }

            Token tokPC = esperado("PC");
            nodoDecl.agregarHijo(new NodoArbol("PC", tokPC));
            padre.agregarHijo(nodoDecl);
        }
    }

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
                registrarError("Sentencia no válida: '" + t.lexema +
                        "' no es una instrucción reconocida del lenguaje", t.linea, t.columna);
                sincronizar();
            }

            if (pos == posAntes) consumir();
        }
    }

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

    private NodoArbol parseAsignacion() {
        NodoArbol nodo = new NodoArbol("ASIGNACION");
        Token tokId = consumir();

        if (!tablaSimbolos.containsKey(tokId.lexema)) {
            registrarError("Variable '" + tokId.lexema + "' no está declarada en la sección decl",
                    tokId.linea, tokId.columna);
            sincronizar();
            return nodo;
        }
        nodo.agregarHijo(new NodoArbol("ID", tokId));

        if (!esTokenActual("ASIG")) {
            Token t = tokenActual();
            registrarError("Falta ':=' en la asignación de '" + tokId.lexema + "'", t.linea, t.columna);
            sincronizar();
            return nodo;
        }
        Token tokAsig = consumir();
        nodo.agregarHijo(new NodoArbol("ASIG", tokAsig));

        if (esTokenActual("PC")) {
            registrarError("La asignación de '" + tokId.lexema + "' no tiene valor (expresión vacía)",
                    tokId.linea, tokId.columna);
            consumir();
            return nodo;
        }

        NodoArbol expr = parseExpresion();
        nodo.agregarHijo(expr);

        if (!esTokenActual("PC")) {
            Token t = tokenActual();
            registrarError("Falta ';' al final de la asignación de '" + tokId.lexema + "'", t.linea, t.columna);
            sincronizar();
        } else {
            Token tokPC = consumir();
            nodo.agregarHijo(new NodoArbol("PC", tokPC));
        }

        return nodo;
    }

    private NodoArbol parseCondicional() {
        NodoArbol nodo = new NodoArbol("CONDICIONAL");
        Token tokSi = consumir();
        nodo.agregarHijo(new NodoArbol("SI", tokSi));

        if (!esTokenActual("PARENTESIS_ABIERTO")) {
            Token t = tokenActual();
            registrarError("Falta '(' después de 'si'", t.linea, t.columna);
        } else consumir();

        NodoArbol condicion = parseExpresion();
        nodo.agregarHijo(condicion);

        if (!esTokenActual("PARENTESIS_CERRADO")) {
            Token t = tokenActual();
            registrarError("Falta ')' para cerrar la condición del 'si'", t.linea, t.columna);
        } else consumir();

        if (!esTokenActual("ENTONCES")) {
            Token t = tokenActual();
            registrarError("Falta 'entonces' después de la condición del 'si'", t.linea, t.columna);
        } else {
            Token tokEntonces = consumir();
            nodo.agregarHijo(new NodoArbol("ENTONCES", tokEntonces));
        }

        NodoArbol sentenciasVerdad = new NodoArbol("SENTENCIAS_ENTONCES");
        parseSentencias(sentenciasVerdad);
        nodo.agregarHijo(sentenciasVerdad);

        if (esTokenActual("SINO")) {
            Token tokSino = consumir();
            nodo.agregarHijo(new NodoArbol("SINO", tokSino));
            NodoArbol sentenciasFalso = new NodoArbol("SENTENCIAS_SINO");
            parseSentencias(sentenciasFalso);
            nodo.agregarHijo(sentenciasFalso);
        }

        if (!esTokenActual("FIN")) {
            Token t = tokenActual();
            registrarError("Falta 'fin' para cerrar el bloque 'si'", t.linea, t.columna);
        } else {
            Token tokFin = consumir();
            nodo.agregarHijo(new NodoArbol("FIN", tokFin));
        }

        return nodo;
    }

    private NodoArbol parseCiclo() {
        NodoArbol nodo = new NodoArbol("CICLO");
        Token tokMientras = consumir();
        nodo.agregarHijo(new NodoArbol("MIENTRAS", tokMientras));

        if (!esTokenActual("PARENTESIS_ABIERTO")) {
            Token t = tokenActual();
            registrarError("Falta '(' después de 'mientras'", t.linea, t.columna);
        } else consumir();

        NodoArbol condicion = parseExpresion();
        nodo.agregarHijo(condicion);

        if (!esTokenActual("PARENTESIS_CERRADO")) {
            Token t = tokenActual();
            registrarError("Falta ')' para cerrar la condición del 'mientras'", t.linea, t.columna);
        } else consumir();

        if (!esTokenActual("HACER")) {
            Token t = tokenActual();
            registrarError("Falta 'hacer' después de la condición del 'mientras'", t.linea, t.columna);
        } else {
            Token tokHacer = consumir();
            nodo.agregarHijo(new NodoArbol("HACER", tokHacer));
        }

        NodoArbol cuerpo = new NodoArbol("SENTENCIAS_CICLO");
        parseSentencias(cuerpo);
        nodo.agregarHijo(cuerpo);

        if (!esTokenActual("FIN")) {
            Token t = tokenActual();
            registrarError("Falta 'fin' para cerrar el bloque 'mientras'", t.linea, t.columna);
        } else {
            Token tokFin = consumir();
            nodo.agregarHijo(new NodoArbol("FIN", tokFin));
        }

        return nodo;
    }

    private NodoArbol parseImpresion() {
        NodoArbol nodo = new NodoArbol("IMPRESION");
        Token tokImp = consumir();
        nodo.agregarHijo(new NodoArbol(tokImp.tipo, tokImp));

        if (!esTokenActual("PARENTESIS_ABIERTO")) {
            registrarError("Falta '(' después de '" + tokImp.lexema + "'", tokImp.linea, tokImp.columna);
            sincronizar();
            return nodo;
        }
        consumir();

        NodoArbol expr = parseExpresion();
        nodo.agregarHijo(expr);

        if (!esTokenActual("PARENTESIS_CERRADO")) {
            Token t = tokenActual();
            registrarError("Falta ')' para cerrar '" + tokImp.lexema + "'", t.linea, t.columna);
            sincronizar();
            return nodo;
        }
        consumir();

        if (!esTokenActual("PC")) {
            Token t = tokenActual();
            registrarError("Falta ';' después de '" + tokImp.lexema + "(...)'", t.linea, t.columna);
            sincronizar();
        } else consumir();

        return nodo;
    }

    private NodoArbol parseLectura() {
        NodoArbol nodo = new NodoArbol("LECTURA");
        Token tokLeer = consumir();
        nodo.agregarHijo(new NodoArbol(tokLeer.tipo, tokLeer));

        if (!esTokenActual("PARENTESIS_ABIERTO")) {
            registrarError("Falta '(' después de '" + tokLeer.lexema + "'", tokLeer.linea, tokLeer.columna);
            sincronizar();
            return nodo;
        }
        consumir();

        Token tokId = esperado("ID");
        if (!tablaSimbolos.containsKey(tokId.lexema)) {
            registrarError("Variable '" + tokId.lexema + "' no está declarada en la sección decl",
                    tokId.linea, tokId.columna);
        } else {
            nodo.agregarHijo(new NodoArbol("ID", tokId));
        }

        if (!esTokenActual("PARENTESIS_CERRADO")) {
            Token t = tokenActual();
            registrarError("Falta ')' para cerrar '" + tokLeer.lexema + "'", t.linea, t.columna);
            sincronizar();
            return nodo;
        }
        consumir();

        if (!esTokenActual("PC")) {
            Token t = tokenActual();
            registrarError("Falta ';' después de '" + tokLeer.lexema + "(...)'", t.linea, t.columna);
            sincronizar();
        } else consumir();

        return nodo;
    }

    // EXPRESION → EXPR_LOGICA
    private NodoArbol parseExpresion() {
        return parseExprLogica();
    }

    // EXPR_LOGICA → EXPR_RELACIONAL ((y | o) EXPR_RELACIONAL)*
    private NodoArbol parseExprLogica() {
        NodoArbol nodo = new NodoArbol("EXPR_LOGICA");
        nodo.agregarHijo(parseExprRelacional());

        while (esTokenActual("Y") || esTokenActual("O")) {
            Token tokOp = consumir();
            nodo.agregarHijo(new NodoArbol("OP_LOGICO", tokOp));

            if (esTokenActual("PC")  || esTokenActual("PARENTESIS_CERRADO") ||
                    esTokenActual("FIN") || esTokenActual("EOF")) {
                Token t = tokenActual();
                registrarError("Falta operando derecho después del operador '" +
                        tokOp.lexema + "'", t.linea, t.columna);
                return nodo;
            }

            nodo.agregarHijo(parseExprRelacional());
        }

        return nodo;
    }

    // EXPR_RELACIONAL → EXPR_ARITMETICA (OP_RELACIONAL EXPR_ARITMETICA)?
    private NodoArbol parseExprRelacional() {
        NodoArbol nodo = new NodoArbol("EXPR_RELACIONAL");
        nodo.agregarHijo(parseExprAritmetica());

        if (esTokenActual("MENOR")      || esTokenActual("MAYOR")      ||
                esTokenActual("IGUAL")      || esTokenActual("MENORIGUAL") ||
                esTokenActual("MAYORIGUAL") || esTokenActual("DISTINTO")) {
            Token tokOp = consumir();
            nodo.agregarHijo(new NodoArbol("OP_RELACIONAL", tokOp));

            if (esTokenActual("PC")  || esTokenActual("PARENTESIS_CERRADO") ||
                    esTokenActual("FIN") || esTokenActual("EOF")) {
                Token t = tokenActual();
                registrarError("Falta operando derecho después del operador '" +
                        tokOp.lexema + "'", t.linea, t.columna);
                return nodo;
            }

            nodo.agregarHijo(parseExprAritmetica());
        }

        return nodo;
    }

    // EXPR_ARITMETICA → TERMINO ((+ | -) TERMINO)*  (antes: parseExpresion)
    private NodoArbol parseExprAritmetica() {
        NodoArbol nodo = new NodoArbol("EXPRESION");

        if (esTokenActual("SUMA") || esTokenActual("RESTA")) {
            Token t = tokenActual();
            registrarError("Falta operando izquierdo antes del operador '" + t.lexema + "'", t.linea, t.columna);
            consumir();
            return nodo;
        }

        nodo.agregarHijo(parseTermino());

        while (esTokenActual("SUMA") || esTokenActual("RESTA")) {
            Token tokOp = consumir();
            nodo.agregarHijo(new NodoArbol("OPERADOR", tokOp));

            if (esTokenActual("SUMA")           || esTokenActual("RESTA") ||
                    esTokenActual("MULTIPLICACION") || esTokenActual("DIVISION")) {
                Token t = tokenActual();
                registrarError("Doble operador en expresión: '" + tokOp.lexema +
                        "' seguido de '" + t.lexema + "'", t.linea, t.columna);
                consumir();
                return nodo;
            }

            if (esTokenActual("PC")               || esTokenActual("PARENTESIS_CERRADO") ||
                    esTokenActual("FIN")              || esTokenActual("EOF")) {
                Token t = tokenActual();
                registrarError("Falta operando derecho después del operador '" +
                        tokOp.lexema + "'", t.linea, t.columna);
                return nodo;
            }

            nodo.agregarHijo(parseTermino());
        }

        return nodo;
    }

    private NodoArbol parseTermino() {
        NodoArbol nodo = new NodoArbol("TERMINO");
        nodo.agregarHijo(parseFactor());

        while (esTokenActual("MULTIPLICACION") || esTokenActual("DIVISION")) {
            Token tokOp = consumir();
            nodo.agregarHijo(new NodoArbol("OPERADOR", tokOp));

            if (esTokenActual("MULTIPLICACION") || esTokenActual("DIVISION") ||
                    esTokenActual("SUMA")           || esTokenActual("RESTA")) {
                Token t = tokenActual();
                registrarError("Doble operador en expresión: '" + tokOp.lexema +
                        "' seguido de '" + t.lexema + "'", t.linea, t.columna);
                consumir();
                return nodo;
            }

            if (esTokenActual("PC")               || esTokenActual("PARENTESIS_CERRADO") ||
                    esTokenActual("FIN")              || esTokenActual("EOF")) {
                Token t = tokenActual();
                registrarError("Falta operando derecho después del operador '" +
                        tokOp.lexema + "'", t.linea, t.columna);
                return nodo;
            }

            nodo.agregarHijo(parseFactor());
        }

        return nodo;
    }

    // FACTOR → ID | CENT | CLIT | verdadero | falso | no FACTOR | ( EXPRESION )
    private NodoArbol parseFactor() {
        Token t = tokenActual();

        if (t.nombre.equals("IDENTIFICADOR")) {
            consumir();
            if (!tablaSimbolos.containsKey(t.lexema)) {
                registrarError("Variable '" + t.lexema + "' no está declarada en la sección decl",
                        t.linea, t.columna);
            }
            return new NodoArbol("ID", t);
        }

        if (t.tipo.equals("CENT")) {
            consumir();
            return new NodoArbol("CENT", t);
        }

        if (t.tipo.equals("CLIT")) {
            consumir();
            return new NodoArbol("CLIT", t);
        }

        if (t.tipo.equals("VERDADERO") || t.tipo.equals("FALSO")) {
            consumir();
            return new NodoArbol(t.tipo, t);
        }

        if (t.tipo.equals("NO")) {
            consumir();
            NodoArbol nodo = new NodoArbol("NO", t);
            nodo.agregarHijo(parseFactor());
            return nodo;
        }

        if (t.tipo.equals("PARENTESIS_ABIERTO")) {
            consumir();
            NodoArbol expr = parseExpresion();
            if (!esTokenActual("PARENTESIS_CERRADO")) {
                registrarError("Paréntesis sin cerrar — falta ')' en la expresión", t.linea, t.columna);
            } else {
                consumir();
            }
            return expr;
        }

        registrarError("Se encontró '" + t.lexema + "' donde se esperaba un valor o variable",
                t.linea, t.columna);
        consumir();
        return new NodoArbol("ERROR", t);
    }

    public List<ErrorSintactico> getErrores() {
        return errores;
    }
}