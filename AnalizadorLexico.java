import java.util.*;

public class AnalizadorLexico {

    // ── Palabras reservadas del lenguaje 
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

    //Operadores aritméticos
    private static final Map<Character, String> ARITMETICOS = new LinkedHashMap<>();
    static {
        ARITMETICOS.put('+', "SUMA");
        ARITMETICOS.put('-', "RESTA");
        ARITMETICOS.put('*', "MULTIPLICACION");
        ARITMETICOS.put('/', "DIVISION");
    }

    // Símbolos de agrupación
    private static final Map<Character, String> AGRUPACION = new LinkedHashMap<>();
    static {
        AGRUPACION.put('(', "PARENTESIS_ABIERTO");
        AGRUPACION.put(')', "PARENTESIS_CERRADO");
        AGRUPACION.put('{', "LLAVE_ABIERTA");
        AGRUPACION.put('}', "LLAVE_CERRADA");
        AGRUPACION.put('[', "CORCHETE_ABIERTO");
        AGRUPACION.put(']', "CORCHETE_CERRADO");
    }

    //  Lista de errores léxicos encontrados 
    private final List<String> errores = new ArrayList<>();

    //  MÉTODO PRINCIPAL: recibe el código fuente y devuelve la lista de tokens
    public List<Token> analizar(String entrada) {
        List<Token> tokens = new ArrayList<>();
        int i     = 0;
        int linea = 1;

        while (i < entrada.length()) {
            char c = entrada.charAt(i);

            // 1. Salto de línea
            if (c == '\n') {
                linea++;
                i++;
                continue;
            }

            // 2. Espacios, tabuladores y retorno de carro -> ignorar
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

            // 4. Operador de asignación := 
            if (c == ':' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '=') {
                tokens.add(new Token("ASIGNACION", "ASIG", ":=", linea));
                i += 2;
                continue;
            }

            // 5. Palabras reservadas e identificadores
            if (Character.isLetter(c)) {
                StringBuilder lexema = new StringBuilder();
                int lineaInicio = linea;
                while (i < entrada.length() &&
                       (Character.isLetterOrDigit(entrada.charAt(i)) || entrada.charAt(i) == '_')) {
                    lexema.append(entrada.charAt(i));
                    i++;
                }
                String palabra = lexema.toString();
                if (RESERVADAS.containsKey(palabra)) {
                    tokens.add(new Token("PALABRA_RESERVADA", RESERVADAS.get(palabra), palabra, lineaInicio));
                } else {
                    tokens.add(new Token("IDENTIFICADOR", "ID", palabra, lineaInicio));
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
                tokens.add(new Token("CONSTANTE_ENTERA", "CENT", lexema.toString(), lineaInicio));
                continue;
            }

            // 7. Cadenas de texto entre comillas "..."
            if (c == '"') {
                StringBuilder lexema = new StringBuilder();
                int lineaInicio = linea;
                i++; // saltar la comilla de apertura
                while (i < entrada.length() && entrada.charAt(i) != '"') {
                    if (entrada.charAt(i) == '\n') linea++;
                    lexema.append(entrada.charAt(i));
                    i++;
                }
                i++; // saltar la comilla de cierre
                tokens.add(new Token("CADENA_LITERAL", "CLIT", "\"" + lexema + "\"", lineaInicio));
                continue;
            }

            // 8. Operadores aritméticos
            if (ARITMETICOS.containsKey(c)) {
                tokens.add(new Token("ARITMETICO", ARITMETICOS.get(c), String.valueOf(c), linea));
                i++;
                continue;
            }

            // 9. Símbolos de agrupación
            if (AGRUPACION.containsKey(c)) {
                tokens.add(new Token("AGRUPACION", AGRUPACION.get(c), String.valueOf(c), linea));
                i++;
                continue;
            }

            // 10. Punto y coma (delimitador de sentencias)
            if (c == ';') {
                tokens.add(new Token("PUNTUACION", "PC", ";", linea));
                i++;
                continue;
            }

            // 11. Coma (separador de variables en declaraciones)
            if (c == ',') {
                tokens.add(new Token("PUNTUACION", "COMA", ",", linea));
                i++;
                continue;
            }

            // 12. Carácter no reconocido → error léxico con número de línea
            errores.add("ERROR LÉXICO en línea " + linea +
                        ": carácter no reconocido '" + c + "'");
            i++;
        }

        return tokens;
    }

    // Devuelve los errores encontrados durante el análisis
    public List<String> getErrores() {
        return errores;
    }
}