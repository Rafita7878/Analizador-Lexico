import java.util.*;

public class AnalizadorLexico {

    // ── Palabras reservadas del lenguaje Rafi ────────────────────────────────
    // El mapa asocia cada palabra reservada con su tipo de token (ej. "PROG", "DECL", etc.)
    private static final Map<String, String> RESERVADAS = new LinkedHashMap<>();
    static {
        RESERVADAS.put("rafi",       "PROG");
        RESERVADAS.put("decl",       "DECL");
        RESERVADAS.put("entero",     "TIPO");
        RESERVADAS.put("cadena",     "TIPO");
        RESERVADAS.put("booleano",   "TIPO");
        RESERVADAS.put("inicio",     "INICIO");
        RESERVADAS.put("fin",        "FIN");
        RESERVADAS.put("si",         "SI");
        RESERVADAS.put("entonces",   "ENTONCES");
        RESERVADAS.put("sino",       "SINO");
        RESERVADAS.put("mientras",   "MIENTRAS");
        RESERVADAS.put("hacer",      "HACER");
        RESERVADAS.put("impdig",     "IMPDIG");
        RESERVADAS.put("impcad",     "IMPCAD");
        RESERVADAS.put("leerdig",    "LEERDIG");
        RESERVADAS.put("leercad",    "LEERCAD");
        RESERVADAS.put("verdadero", "VERDADERO");
        RESERVADAS.put("falso",     "FALSO");
        RESERVADAS.put("y",         "Y");
        RESERVADAS.put("o",         "O");
        RESERVADAS.put("no",        "NO");
    }

    // ── Operadores aritméticos ───────────────────────────────────────────────
    // El mapa asocia cada símbolo aritmético con su tipo de token (ej. "SUMA", "RESTA", etc.)
    private static final Map<Character, String> ARITMETICOS = new LinkedHashMap<>();
    static {
        ARITMETICOS.put('+', "SUMA");
        ARITMETICOS.put('-', "RESTA");
        ARITMETICOS.put('*', "MULTIPLICACION");
        ARITMETICOS.put('/', "DIVISION");
    }

    // ── Símbolos de agrupación ───────────────────────────────────────────────
    // El mapa asocia cada símbolo de agrupación con su tipo de token (ej. "PARENTESIS_ABIERTO", "PARENTESIS_CERRADO", etc.)
    private static final Map<Character, String> AGRUPACION = new LinkedHashMap<>();
    static {
        AGRUPACION.put('(', "PARENTESIS_ABIERTO");
        AGRUPACION.put(')', "PARENTESIS_CERRADO");
        AGRUPACION.put('{', "LLAVE_ABIERTA");
        AGRUPACION.put('}', "LLAVE_CERRADA");
        AGRUPACION.put('[', "CORCHETE_ABIERTO");
        AGRUPACION.put(']', "CORCHETE_CERRADO");
    }

    // ── Números de referencia para cada tipo de token ────────────────────────
    // El mapa asocia cada tipo de token con su número de referencia (ej. "PROG" → 100, "ID" → 300, etc.)
    private static final Map<String, Integer> REF_TOKEN = new LinkedHashMap<>();
    static {
        // Palabras reservadas
        REF_TOKEN.put("PROG",               100);
        REF_TOKEN.put("DECL",               101);
        REF_TOKEN.put("TIPO",               102);
        REF_TOKEN.put("INICIO",             103);
        REF_TOKEN.put("FIN",                104);
        REF_TOKEN.put("SI",                 105);
        REF_TOKEN.put("ENTONCES",           106);
        REF_TOKEN.put("SINO",               107);
        REF_TOKEN.put("MIENTRAS",           108);
        REF_TOKEN.put("HACER",              109);
        REF_TOKEN.put("IMPDIG",             110);
        REF_TOKEN.put("IMPCAD",             111);
        REF_TOKEN.put("LEERDIG",            112);
        REF_TOKEN.put("LEERCAD",            113);
        // Identificadores y constantes
        REF_TOKEN.put("ID",                 300);
        REF_TOKEN.put("CENT",               400);
        REF_TOKEN.put("CLIT",               500);
        // Aritméticos
        REF_TOKEN.put("SUMA",                10);
        REF_TOKEN.put("RESTA",               11);
        REF_TOKEN.put("MULTIPLICACION",      12);
        REF_TOKEN.put("DIVISION",            13);
        // Agrupación
        REF_TOKEN.put("PARENTESIS_ABIERTO",  75);
        REF_TOKEN.put("PARENTESIS_CERRADO",  76);
        REF_TOKEN.put("LLAVE_ABIERTA",       79);
        REF_TOKEN.put("LLAVE_CERRADA",       80);
        REF_TOKEN.put("CORCHETE_ABIERTO",    81);
        REF_TOKEN.put("CORCHETE_CERRADO",    82);
        // Puntuación y asignación
        REF_TOKEN.put("PC",                  92);
        REF_TOKEN.put("COMA",                91);
        REF_TOKEN.put("ASIG",                90);
        REF_TOKEN.put("VERDADERO",   120);
        REF_TOKEN.put("FALSO",       121);
        REF_TOKEN.put("Y",           122);
        REF_TOKEN.put("O",           123);
        REF_TOKEN.put("NO",          124);
        REF_TOKEN.put("MENOR",       130);
        REF_TOKEN.put("MAYOR",       131);
        REF_TOKEN.put("IGUAL",       132);
        REF_TOKEN.put("MENORIGUAL",  133);
        REF_TOKEN.put("MAYORIGUAL",  134);
        REF_TOKEN.put("DISTINTO",    135);
    }

    // Devuelve el número de referencia de un tipo de token
    public int getRef(String tipo) {
        return REF_TOKEN.getOrDefault(tipo, -1);
    }

    // ── Lista de errores léxicos encontrados ────────────────────────────────
    private final List<String> errores = new ArrayList<>();

    // ────────────────────────────────────────────────────────────────────────
    //  MÉTODO PRINCIPAL: recibe el código fuente y devuelve la lista de tokens
    // ────────────────────────────────────────────────────────────────────────
    public List<Token> analizar(String entrada) {
        List<Token> tokens = new ArrayList<>();
        int i     = 0;
        int linea = 1;
        int columna = 1;

        while (i < entrada.length()) {
            char c = entrada.charAt(i);

            // 1. Salto de línea
            if (c == '\n') {
                    linea++;
                columna = 1;     // reinicia columna en cada renglón nuevo
                i++;
                continue;
            }

            // 2. Espacios, tabuladores y retorno de carro
            if (c == ' ' || c == '\t' || c == '\r') {
                i++; columna++;
                continue;
            }

            // 3. Comentarios /* ... */
            if (c == '/' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '*') {
                i += 2; columna += 2;
                while (i + 1 < entrada.length()) {
                    if (entrada.charAt(i) == '\n') { linea++; columna = 1; }
                    else columna++;
                    if (entrada.charAt(i) == '*' && entrada.charAt(i + 1) == '/') {
                        i += 2; columna += 2;
                        break;
                    }
                    i++;
                }
                continue;
            }

            // 4. Operador de asignación :=
            if (c == ':' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '=') {
                tokens.add(new Token("ASIGNACION", "ASIG", ":=", linea, columna, getRef("ASIG")));
                i += 2; columna += 2;
                continue;
            }

            // 4.5 Relacionales
            if (c == '<' || c == '>' || c == '=') {
                char sig = (i + 1 < entrada.length()) ? entrada.charAt(i + 1) : ' ';
                String lex; String tipo; int avance;
                if (c == '<' && sig == '=') { lex = "<="; tipo = "MENORIGUAL"; avance = 2; }
                else if (c == '>' && sig == '=') { lex = ">="; tipo = "MAYORIGUAL"; avance = 2; }
                else if (c == '<' && sig == '>') { lex = "<>"; tipo = "DISTINTO"; avance = 2; }
                else { lex = String.valueOf(c); tipo = c=='<' ? "MENOR" : (c=='>' ? "MAYOR" : "IGUAL"); avance = 1; }
                tokens.add(new Token("RELACIONAL", tipo, lex, linea, columna, getRef(tipo)));
                i += avance; columna += avance;
                continue;
            }

            // 5. Palabras reservadas e identificadores
            if (Character.isLetter(c)) {
                StringBuilder lexema     = new StringBuilder();
                boolean       tieneError = false;
                int           lineaInicio   = linea;
                int           columnaInicio = columna;   // NUEVO

                while (i < entrada.length()) {
                    char actual = entrada.charAt(i);
                    if (actual == ' '  || actual == '\t' || actual == '\r' ||
                            actual == '\n' || actual == ';'  || actual == ','  ||
                            actual == '('  || actual == ')'  || actual == '{'  ||
                            actual == '}'  || actual == '['  || actual == ']'  ||
                            actual == '+'  || actual == '-'  || actual == '*'  ||
                            actual == '/'  || actual == ':'  || actual == '"') {
                        break;
                    }
                    if (!Character.isLetterOrDigit(actual) && actual != '_') {
                        tieneError = true;
                    }
                    lexema.append(actual);
                    i++; columna++;   // NUEVO
                }

                String palabra = lexema.toString();

                if (tieneError) {
                    errores.add("Renglón: " + lineaInicio +
                            ", Identificador inválido '" + palabra +
                            "' contiene símbolo(s) no permitido(s)");
                    tokens.add(new Token("ERROR", "ID_INVALIDO", palabra, lineaInicio, columnaInicio, -1));
                } else if (RESERVADAS.containsKey(palabra)) {
                    String tipo = RESERVADAS.get(palabra);
                    tokens.add(new Token("PALABRA_RESERVADA", tipo, palabra, lineaInicio, columnaInicio, getRef(tipo)));
                } else {
                    tokens.add(new Token("IDENTIFICADOR", "ID", palabra, lineaInicio, columnaInicio, getRef("ID")));
                }
                continue;
            }

            // 6. Números enteros
            if (Character.isDigit(c)) {
                StringBuilder lexema = new StringBuilder();
                int lineaInicio   = linea;
                int columnaInicio = columna;   // NUEVO
                while (i < entrada.length() && Character.isDigit(entrada.charAt(i))) {
                    lexema.append(entrada.charAt(i));
                    i++; columna++;
                }
                tokens.add(new Token("CONSTANTE_ENTERA", "CENT", lexema.toString(), lineaInicio, columnaInicio, getRef("CENT")));
                continue;
            }

            // 7. Cadenas de texto entre comillas "..."
            if (c == '"') {
                StringBuilder lexema     = new StringBuilder();
                int           lineaInicio   = linea;
                int           columnaInicio = columna;   // NUEVO
                boolean       cerrada     = false;
                i++; columna++; // saltar comilla de apertura

                while (i < entrada.length()) {
                    char actual = entrada.charAt(i);
                    if (actual == '"') { cerrada = true; i++; columna++; break; }
                    if (actual == '\n') { linea++; columna = 1; break; }
                    lexema.append(actual);
                    i++; columna++;
                }

                if (cerrada) {
                    tokens.add(new Token("CADENA_LITERAL", "CLIT",
                            "\"" + lexema + "\"", lineaInicio, columnaInicio, getRef("CLIT")));
                } else {
                    errores.add("Renglón: " + lineaInicio +
                            ", Cadena de texto no cerrada: \"" + lexema +
                            "  (falta comilla de cierre \")");
                    tokens.add(new Token("ERROR", "CLIT_INVALIDA",
                            "\"" + lexema, lineaInicio, columnaInicio, -1));
                }
                continue;
            }

            // 8. Operadores aritméticos
            if (ARITMETICOS.containsKey(c)) {
                String tipo = ARITMETICOS.get(c);
                tokens.add(new Token("ARITMETICO", tipo, String.valueOf(c), linea, columna, getRef(tipo)));
                i++; columna++;
                continue;
            }

            // 9. Símbolos de agrupación
            if (AGRUPACION.containsKey(c)) {
                String tipo = AGRUPACION.get(c);
                tokens.add(new Token("AGRUPACION", tipo, String.valueOf(c), linea, columna, getRef(tipo)));
                i++; columna++;
                continue;
            }

            // 10. Punto y coma
            if (c == ';') {
                tokens.add(new Token("PUNTUACION", "PC", ";", linea, columna, getRef("PC")));
                i++; columna++;
                continue;
            }

            // 11. Coma
            if (c == ',') {
                tokens.add(new Token("PUNTUACION", "COMA", ",", linea, columna, getRef("COMA")));
                i++; columna++;
                continue;
            }

            // 12. Carácter no reconocido
            errores.add("Renglón: " + linea +
                    ", Símbolo no identificado '" + c + "' (posible error léxico)");
            i++; columna++;
        }

        return tokens;
    }

    // Devuelve los errores encontrados durante el análisis
    public List<String> getErrores() {
        return errores;
    }
}