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

    while (i < entrada.length()) {
        char c = entrada.charAt(i);

        // 1. Salto de línea → el contador de lineas aumenta y se ignora el carácter
        if (c == '\n') {
            linea++;
            i++;
            continue;
        }

        // 2. Espacios, tabuladores y retorno de carro → ignorar
        if (c == ' ' || c == '\t' || c == '\r') {
            i++;
            continue;
        }

        // 3. Comentarios /* ... */ → consumir sin generar token
        if (c == '/' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '*') {
            i += 2;
            while (i + 1 < entrada.length()) {
                if (entrada.charAt(i) == '\n') linea++;
                if (entrada.charAt(i) == '*' && entrada.charAt(i + 1) == '/') {
                    i += 2;
                    break;
                }
                i++;
            }
            continue;
        }

        // 4. Operador de asignación := (símbolo doble, requiere lookahead)
        if (c == ':' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '=') {
            tokens.add(new Token("ASIGNACION", "ASIG", ":=", linea,
                                 getRef("ASIG")));
            i += 2;
            continue;
        }

        // ────────────────────────────────────────────────────────────────────
        // 5. Palabras reservadas e identificadores
        // CORRECCIÓN: si dentro de la palabra aparece un símbolo inválido como
        // @, #, $ etc., se acumula TODO como un solo lexema inválido y se
        // reporta un único error — la palabra NO se divide en dos tokens.
        // ────────────────────────────────────────────────────────────────────
        if (Character.isLetter(c)) {
            StringBuilder lexema    = new StringBuilder();
            boolean       tieneError = false; // bandera: ¿encontramos símbolo inválido dentro?
            int           lineaInicio = linea;

            // Acumular mientras sea letra, dígito, guión bajo O símbolo no reconocido
            // que NO sea un separador (espacio, salto de línea, operador conocido, etc.)
            while (i < entrada.length()) {
                char actual = entrada.charAt(i);

                // Condiciones de corte — caracteres que SÍ terminan la palabra
                if (actual == ' '  || actual == '\t' || actual == '\r' ||
                    actual == '\n' || actual == ';'  || actual == ','  ||
                    actual == '('  || actual == ')'  || actual == '{'  ||
                    actual == '}'  || actual == '['  || actual == ']'  ||
                    actual == '+'  || actual == '-'  || actual == '*'  ||
                    actual == '/'  || actual == ':'  || actual == '"') {
                    break;
                }

                // Si el carácter no es letra, dígito ni guión bajo → es inválido
                // pero está DENTRO de una palabra, así que lo acumulamos y marcamos error
                if (!Character.isLetterOrDigit(actual) && actual != '_') {
                    tieneError = true;
                }

                lexema.append(actual);
                i++;
            }

            String palabra = lexema.toString();

            if (tieneError) {
                // La palabra contiene un símbolo inválido → error léxico, un solo token
                errores.add("Renglón: " + lineaInicio +
                            ", Identificador inválido '" + palabra +
                            "' contiene símbolo(s) no permitido(s)");
                tokens.add(new Token("ERROR", "ID_INVALIDO", palabra, lineaInicio, -1));
            } else if (RESERVADAS.containsKey(palabra)) {
                String tipo = RESERVADAS.get(palabra);
                tokens.add(new Token("PALABRA_RESERVADA", tipo, palabra, lineaInicio,
                                     getRef(tipo)));
            } else {
                tokens.add(new Token("IDENTIFICADOR", "ID", palabra, lineaInicio,
                                     getRef("ID")));
            }
            continue;
        }

        // 6. Números enteros
        if (Character.isDigit(c)) {
            StringBuilder lexema = new StringBuilder();
            int lineaInicio = linea;
            while (i < entrada.length() && Character.isDigit(entrada.charAt(i))) {
                lexema.append(entrada.charAt(i));
                i++;
            }
            tokens.add(new Token("CONSTANTE_ENTERA", "CENT", lexema.toString(), lineaInicio,
                                 getRef("CENT")));
            continue;
        }

        // ────────────────────────────────────────────────────────────────────
        // 7. Cadenas de texto entre comillas "..."
        // CORRECCIÓN: si la cadena nunca se cierra (llega al final del archivo
        // o encuentra un salto de línea sin comilla de cierre), se reporta
        // un error léxico indicando que la cadena no fue cerrada.
        // ────────────────────────────────────────────────────────────────────
        if (c == '"') {
            StringBuilder lexema     = new StringBuilder();
            int           lineaInicio = linea;
            boolean       cerrada     = false;
            i++; // saltar comilla de apertura

            while (i < entrada.length()) {
                char actual = entrada.charAt(i);

                if (actual == '"') {
                    // Comilla de cierre encontrada → cadena válida
                    cerrada = true;
                    i++;
                    break;
                }

                if (actual == '\n') {
                    // Salto de línea sin cerrar → la cadena es inválida
                    // Se detiene aquí y se reporta el error
                    linea++;
                    break;
                }

                lexema.append(actual);
                i++;
            }

            if (cerrada) {
                // Cadena correctamente cerrada → token normal
                tokens.add(new Token("CADENA_LITERAL", "CLIT",
                                     "\"" + lexema + "\"", lineaInicio,
                                     getRef("CLIT")));
            } else {
                // Cadena nunca cerrada → error léxico
                errores.add("Renglón: " + lineaInicio +
                            ", Cadena de texto no cerrada: \"" + lexema +
                            "  (falta comilla de cierre \")");
                tokens.add(new Token("ERROR", "CLIT_INVALIDA",
                                     "\"" + lexema, lineaInicio, -1));
            }
            continue;
        }

        // 8. Operadores aritméticos
        if (ARITMETICOS.containsKey(c)) {
            String tipo = ARITMETICOS.get(c);
            tokens.add(new Token("ARITMETICO", tipo, String.valueOf(c), linea,
                                 getRef(tipo)));
            i++;
            continue;
        }

        // 9. Símbolos de agrupación
        if (AGRUPACION.containsKey(c)) {
            String tipo = AGRUPACION.get(c);
            tokens.add(new Token("AGRUPACION", tipo, String.valueOf(c), linea,
                                 getRef(tipo)));
            i++;
            continue;
        }

        // 10. Punto y coma
        if (c == ';') {
            tokens.add(new Token("PUNTUACION", "PC", ";", linea,
                                 getRef("PC")));
            i++;
            continue;
        }

        // 11. Coma
        if (c == ',') {
            tokens.add(new Token("PUNTUACION", "COMA", ",", linea,
                                 getRef("COMA")));
            i++;
            continue;
        }

        // 12. Carácter no reconocido → error léxico con número de línea
        errores.add("Renglón: " + linea +
                    ", Símbolo no identificado '" + c + "' (posible error léxico)");
        i++;
    }

    return tokens;
}

    // Devuelve los errores encontrados durante el análisis
    public List<String> getErrores() {
        return errores;
    }
}