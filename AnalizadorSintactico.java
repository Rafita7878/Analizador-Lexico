// AnalizadorSintactico.java
// Analizador sintáctico descendente recursivo para el lenguaje RAFI.
//
// Recibe la lista de tokens generada por el AnalizadorLexico y verifica
// que la estructura del programa cumpla con la gramática del lenguaje RAFI.
//
// Gramática que implementa:
//   PROGRAMA     → rafi ID decl DECLARACIONES inicio SENTENCIAS fin
//   DECLARACIONES→ DECLARACION*
//   DECLARACION  → TIPO ID (, ID)* ;
//   SENTENCIAS   → SENTENCIA*
//   SENTENCIA    → ASIGNACION | CONDICIONAL | CICLO | IMPRESION | LECTURA
//   ASIGNACION   → ID := EXPRESION ;
//   CONDICIONAL  → si ( EXPRESION ) entonces SENTENCIAS [sino SENTENCIAS] fin
//   CICLO        → mientras ( EXPRESION ) hacer SENTENCIAS fin
//   EXPRESION    → TERMINO ((+ | -) TERMINO)*
//   TERMINO      → FACTOR ((* | /) FACTOR)*
//   FACTOR       → ID | CENT | CLIT | ( EXPRESION )

import java.util.*;

public class AnalizadorSintactico {

    // ── Lista de tokens recibida del analizador léxico ───────────────────────
    private List<Token> tokens;

    // ── Índice del token actual que se está analizando ───────────────────────
    // Avanza conforme se consumen tokens válidos
    private int pos;

    // ── Lista de errores sintácticos encontrados ─────────────────────────────
    private List<ErrorSintactico> errores;

    // ── Tabla de símbolos recibida del analizador léxico ─────────────────────
    // Se usa para verificar que las variables estén declaradas antes de usarse
    private Map<String, String[]> tablaSimbolos;

    // Constructor — recibe tokens y tabla de símbolos del analizador léxico
    public AnalizadorSintactico(List<Token> tokens, Map<String, String[]> tablaSimbolos) {
        this.tokens       = tokens;
        this.tablaSimbolos = tablaSimbolos;
        this.pos          = 0;
        this.errores      = new ArrayList<>();
    }

    // ── Devuelve el token actual sin avanzar (lookahead) ─────────────────────
    private Token tokenActual() {
        if (pos < tokens.size()) return tokens.get(pos);
        // Token centinela al llegar al final — evita NullPointerException
        return new Token("EOF", "EOF", "", 0, -1);
    }

    // ── Avanza al siguiente token ────────────────────────────────────────────
    private Token consumir() {
        Token t = tokenActual();
        pos++;
        return t;
    }

    // ── Verifica que el token actual sea del tipo esperado y avanza ──────────
    // Si no coincide, registra un error sintáctico y NO avanza
    // (modo pánico simple: el análisis intenta continuar)
    private Token esperado(String tipo) {
        Token t = tokenActual();
        if (t.tipo.equals(tipo)) {
            return consumir();
        }
        errores.add(new ErrorSintactico(
            "Se esperaba '" + tipo + "' pero se encontró '" + t.lexema + "'", t.linea));
        return t;
    }

    // ── Verifica si el token actual es del tipo indicado (sin consumir) ──────
    private boolean esTokenActual(String tipo) {
        return tokenActual().tipo.equals(tipo);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  MÉTODO PRINCIPAL — inicia el análisis y devuelve la raíz del árbol
    // ────────────────────────────────────────────────────────────────────────
    public NodoArbol analizar() {
        NodoArbol raiz = parsePrograma();
        // Si quedaron tokens sin consumir después del fin, es un error
        if (!esTokenActual("EOF") && !esTokenActual("FIN")) {
            Token t = tokenActual();
            errores.add(new ErrorSintactico(
                "Tokens inesperados después del fin del programa: '" + t.lexema + "'", t.linea));
        }
        return raiz;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  PROGRAMA → rafi ID decl DECLARACIONES inicio SENTENCIAS fin
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parsePrograma() {
        NodoArbol nodo = new NodoArbol("PROGRAMA");

        // Encabezado: rafi NombrePrograma
        Token tokRafi = esperado("PROG");
        nodo.agregarHijo(new NodoArbol("PROG", tokRafi.lexema));

        Token tokNombre = esperado("ID");
        nodo.agregarHijo(new NodoArbol("ID", tokNombre.lexema));

        // Sección de declaraciones
        if (esTokenActual("DECL")) {
            Token tokDecl = consumir();
            NodoArbol nodoDecl = new NodoArbol("DECLARACIONES");
            nodoDecl.agregarHijo(new NodoArbol("DECL", tokDecl.lexema));
            parseDeclaraciones(nodoDecl);
            nodo.agregarHijo(nodoDecl);
        }

        // Cuerpo del programa
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
        // Mientras el token actual sea un tipo de dato, hay declaraciones
        while (esTokenActual("TIPO")) {
            NodoArbol nodoDecl = new NodoArbol("DECLARACION");

            Token tokTipo = consumir();
            nodoDecl.agregarHijo(new NodoArbol("TIPO", tokTipo.lexema));

            // Leer primer identificador obligatorio
            Token tokId = esperado("ID");
            nodoDecl.agregarHijo(new NodoArbol("ID", tokId.lexema));

            // Leer identificadores adicionales separados por coma
            while (esTokenActual("COMA")) {
                consumir(); // consumir la coma
                Token tokIdExtra = esperado("ID");
                nodoDecl.agregarHijo(new NodoArbol("ID", tokIdExtra.lexema));
            }

            // Punto y coma al final de la declaración
            esperado("PC");
            nodoDecl.agregarHijo(new NodoArbol("PC", ";"));

            padre.agregarHijo(nodoDecl);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SENTENCIAS → SENTENCIA* (mientras no sea fin, sino, EOF)
    // ────────────────────────────────────────────────────────────────────────
    private void parseSentencias(NodoArbol padre) {
        // Continuar mientras haya sentencias válidas
        while (!esTokenActual("FIN")  &&
               !esTokenActual("SINO") &&
               !esTokenActual("EOF")) {

            NodoArbol sentencia = parseSentencia();
            if (sentencia != null) {
                padre.agregarHijo(sentencia);
            } else {
                // Token inesperado — avanzar para evitar bucle infinito
                Token t = consumir();
                errores.add(new ErrorSintactico(
                    "Sentencia no reconocida: '" + t.lexema + "'", t.linea));
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SENTENCIA — decide qué tipo de sentencia es según el token actual
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

        return null; // token no reconocido como inicio de sentencia
    }

    // ────────────────────────────────────────────────────────────────────────
    //  ASIGNACION → ID := EXPRESION ;
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseAsignacion() {
        NodoArbol nodo = new NodoArbol("ASIGNACION");

        Token tokId = consumir(); // consumir el ID

        // Verificar que la variable esté declarada en la tabla de símbolos
        if (!tablaSimbolos.containsKey(tokId.lexema)) {
            errores.add(new ErrorSintactico(
                "Variable '" + tokId.lexema + "' no declarada", tokId.linea));
        }
        nodo.agregarHijo(new NodoArbol("ID", tokId.lexema));

        esperado("ASIG");
        nodo.agregarHijo(new NodoArbol("ASIG", ":="));

        // Verificar que haya una expresión válida después del :=
        if (esTokenActual("PC")) {
            errores.add(new ErrorSintactico(
                "Se esperaba una expresión después de ':=' en línea " + tokId.linea,
                tokId.linea));
        } else {
            NodoArbol expr = parseExpresion();
            nodo.agregarHijo(expr);
        }

        esperado("PC");
        nodo.agregarHijo(new NodoArbol("PC", ";"));

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CONDICIONAL → si ( EXPRESION ) entonces SENTENCIAS [sino SENTENCIAS] fin
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseCondicional() {
        NodoArbol nodo = new NodoArbol("CONDICIONAL");

        consumir(); // consumir 'si'
        nodo.agregarHijo(new NodoArbol("SI", "si"));

        esperado("PARENTESIS_ABIERTO");
        NodoArbol condicion = parseExpresion();
        nodo.agregarHijo(condicion);
        esperado("PARENTESIS_CERRADO");

        esperado("ENTONCES");
        nodo.agregarHijo(new NodoArbol("ENTONCES", "entonces"));

        NodoArbol sentenciasVerdad = new NodoArbol("SENTENCIAS_ENTONCES");
        parseSentencias(sentenciasVerdad);
        nodo.agregarHijo(sentenciasVerdad);

        // Rama sino (opcional)
        if (esTokenActual("SINO")) {
            consumir();
            nodo.agregarHijo(new NodoArbol("SINO", "sino"));
            NodoArbol sentenciasFalso = new NodoArbol("SENTENCIAS_SINO");
            parseSentencias(sentenciasFalso);
            nodo.agregarHijo(sentenciasFalso);
        }

        esperado("FIN");
        nodo.agregarHijo(new NodoArbol("FIN", "fin"));

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CICLO → mientras ( EXPRESION ) hacer SENTENCIAS fin
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseCiclo() {
        NodoArbol nodo = new NodoArbol("CICLO");

        consumir(); // consumir 'mientras'
        nodo.agregarHijo(new NodoArbol("MIENTRAS", "mientras"));

        esperado("PARENTESIS_ABIERTO");
        NodoArbol condicion = parseExpresion();
        nodo.agregarHijo(condicion);
        esperado("PARENTESIS_CERRADO");

        esperado("HACER");
        nodo.agregarHijo(new NodoArbol("HACER", "hacer"));

        NodoArbol cuerpo = new NodoArbol("SENTENCIAS_CICLO");
        parseSentencias(cuerpo);
        nodo.agregarHijo(cuerpo);

        esperado("FIN");
        nodo.agregarHijo(new NodoArbol("FIN", "fin"));

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  IMPRESION → (impdig | impcad) ( EXPRESION ) ;
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseImpresion() {
        NodoArbol nodo = new NodoArbol("IMPRESION");

        Token tokImp = consumir();
        nodo.agregarHijo(new NodoArbol(tokImp.tipo, tokImp.lexema));

        // Verificar paréntesis abierto
        if (!esTokenActual("PARENTESIS_ABIERTO")) {
            errores.add(new ErrorSintactico(
                "Se esperaba '(' después de '" + tokImp.lexema + "'", tokImp.linea));
        } else {
            consumir();
        }

        NodoArbol expr = parseExpresion();
        nodo.agregarHijo(expr);

        // Verificar paréntesis cerrado
        if (!esTokenActual("PARENTESIS_CERRADO")) {
            errores.add(new ErrorSintactico(
                "Paréntesis de cierre ')' faltante en '" + tokImp.lexema + "'", tokImp.linea));
        } else {
            consumir();
        }

        esperado("PC");
        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  LECTURA → (leerdig | leercad) ( ID ) ;
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseLectura() {
        NodoArbol nodo = new NodoArbol("LECTURA");

        Token tokLeer = consumir();
        nodo.agregarHijo(new NodoArbol(tokLeer.tipo, tokLeer.lexema));

        esperado("PARENTESIS_ABIERTO");

        Token tokId = esperado("ID");
        // Verificar que la variable esté declarada
        if (!tablaSimbolos.containsKey(tokId.lexema)) {
            errores.add(new ErrorSintactico(
                "Variable '" + tokId.lexema + "' no declarada", tokId.linea));
        }
        nodo.agregarHijo(new NodoArbol("ID", tokId.lexema));

        esperado("PARENTESIS_CERRADO");
        esperado("PC");
        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  EXPRESION → TERMINO ((+ | -) TERMINO)*
    //  Maneja: doble operador, ausencia de operandos
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseExpresion() {
        NodoArbol nodo = new NodoArbol("EXPRESION");

        // Verificar ausencia de operando al inicio
        if (esTokenActual("SUMA") || esTokenActual("RESTA")) {
            Token t = tokenActual();
            errores.add(new ErrorSintactico(
                "Ausencia de operando izquierdo antes de '" + t.lexema + "'", t.linea));
            consumir(); // recuperación: saltamos el operador huérfano
        }

        nodo.agregarHijo(parseTermino());

        // Mientras haya + o - seguimos construyendo la expresión
        while (esTokenActual("SUMA") || esTokenActual("RESTA")) {
            Token tokOp = consumir();
            nodo.agregarHijo(new NodoArbol("OPERADOR", tokOp.lexema));

            // Verificar doble operador: + + o + -
            if (esTokenActual("SUMA") || esTokenActual("RESTA") ||
                esTokenActual("MULTIPLICACION") || esTokenActual("DIVISION")) {
                Token t = tokenActual();
                errores.add(new ErrorSintactico(
                    "Doble operador: '" + tokOp.lexema + "' seguido de '" + t.lexema + "'",
                    t.linea));
                consumir(); // recuperación
            }

            // Verificar ausencia de operando derecho
            if (esTokenActual("PC") || esTokenActual("PARENTESIS_CERRADO") || esTokenActual("EOF")) {
                Token t = tokenActual();
                errores.add(new ErrorSintactico(
                    "Ausencia de operando derecho después de '" + tokOp.lexema + "'", t.linea));
            } else {
                nodo.agregarHijo(parseTermino());
            }
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

            // Verificar doble operador
            if (esTokenActual("MULTIPLICACION") || esTokenActual("DIVISION") ||
                esTokenActual("SUMA") || esTokenActual("RESTA")) {
                Token t = tokenActual();
                errores.add(new ErrorSintactico(
                    "Doble operador: '" + tokOp.lexema + "' seguido de '" + t.lexema + "'",
                    t.linea));
                consumir();
            }

            // Verificar ausencia de operando derecho
            if (esTokenActual("PC") || esTokenActual("PARENTESIS_CERRADO") || esTokenActual("EOF")) {
                Token t = tokenActual();
                errores.add(new ErrorSintactico(
                    "Ausencia de operando derecho después de '" + tokOp.lexema + "'", t.linea));
            } else {
                nodo.agregarHijo(parseFactor());
            }
        }

        return nodo;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  FACTOR → ID | CENT | CLIT | ( EXPRESION )
    //  Un factor es la unidad mínima de una expresión
    // ────────────────────────────────────────────────────────────────────────
    private NodoArbol parseFactor() {
        Token t = tokenActual();

        // Identificador
        if (t.nombre.equals("IDENTIFICADOR")) {
            consumir();
            if (!tablaSimbolos.containsKey(t.lexema)) {
                errores.add(new ErrorSintactico(
                    "Variable '" + t.lexema + "' usada pero no declarada", t.linea));
            }
            return new NodoArbol("ID", t.lexema);
        }

        // Constante entera
        if (t.tipo.equals("CENT")) {
            consumir();
            return new NodoArbol("CENT", t.lexema);
        }

        // Cadena literal
        if (t.tipo.equals("CLIT")) {
            consumir();
            return new NodoArbol("CLIT", t.lexema);
        }

        // Expresión entre paréntesis — verificar balanceo
        if (t.tipo.equals("PARENTESIS_ABIERTO")) {
            consumir(); // consumir (
            NodoArbol expr = parseExpresion();

            // Verificar paréntesis de cierre
            if (!esTokenActual("PARENTESIS_CERRADO")) {
                errores.add(new ErrorSintactico(
                    "Paréntesis ')' no balanceado — falta cierre", t.linea));
            } else {
                consumir(); // consumir )
            }
            return expr;
        }

        // Ningún factor válido encontrado
        errores.add(new ErrorSintactico(
            "Se esperaba un operando pero se encontró '" + t.lexema + "'", t.linea));
        consumir(); // recuperación
        return new NodoArbol("ERROR", t.lexema);
    }

    // Devuelve la lista de errores sintácticos encontrados
    public List<ErrorSintactico> getErrores() {
        return errores;
    }
}