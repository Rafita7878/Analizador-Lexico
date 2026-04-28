// Main.java
/*
    *Clase principal del analizador léxico, responsable de:
    * coordinar todas las etapas de analisis en el siguiente orden
    * 1. Lee el archivo fuente progfte.txt
    * 2. Genera el archivo depurado progfte.dep
    * 3. Analiza el código fuente y genera la lista de tokens
    * 4. Construye la tabla de símbolos con las variables declaradas
    * 5. Genera el archivo de tabla de símbolos progfte.tab
    * 6. Genera el archivo completo de tokens progfte.tok
    * 7. Muestra el resultado en consola
 */
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        // ── Rutas de archivos ────────────────────────────────────────────────
        String rutaArchivo  = "progfte.txt";//archivo fuente original
        String rutaDepurado = "progfte.dep";//archivo fuente depurado (sin comentarios ni espacios innecesarios)
        String rutaTabla    = "progfte.tab";//archivo con la tabla de símbolos (variables declaradas)
        String rutaTokens   = "progfte.tok";//archivo completo con tabla de símbolos + lista de tokens + resumen + errores

        // ── Leer el archivo fuente ───────────────────────────────────────────
        // Si el archivo no existe o no se puede leer, se muestra un mensaje de error y se detiene la ejecución
        String codigoFuente;
        try {
            codigoFuente = Files.readString(Path.of(rutaArchivo));
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo leer el archivo '" + rutaArchivo + "'");
            System.out.println("Verifica que el archivo existe y está en la carpeta correcta.");
            return;
        }

        // ── Generar archivo depurado .dep ────────────────────────────────────
        // Se llama a depurar() antes del análisis para limpiar el código fuente.
        // El archivo .dep es el programa sin comentarios, espacios múltiples
        // ni líneas vacías — la versión lista para las fases siguientes.
        String codigoDepurado = depurar(codigoFuente);
        try {
            Files.writeString(Path.of(rutaDepurado), codigoDepurado);
            System.out.println("Archivo depurado generado  : " + rutaDepurado);
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo escribir el archivo '" + rutaDepurado + "'");
        }

        // ── Analizar el código fuente ────────────────────────────────────────
        // AnalizadorLexico.analizar() recorre el código carácter a carácter
        // y devuelve una lista de objetos Token con toda la información
        // de cada unidad léxica reconocida: nombre, tipo, lexema, línea y ref.
        AnalizadorLexico alex = new AnalizadorLexico();
        List<Token> tokens = alex.analizar(codigoFuente);

        // ── Construir tabla de símbolos ──────────────────────────────────────
        // Se recorre la lista de tokens buscando el patrón de declaración:
        // DECL → TIPO → ID, ID, ID ; → hasta encontrar INICIO.
        // Cada variable encontrada se registra con su tipo, valor inicial y línea.
        Map<String, String[]> tablaSimbolos = construirTablaSimbolos(tokens);

        // ── Generar archivo .tab ─────────────────────────────────────────────
        // Contiene únicamente las variables declaradas en la sección decl,
        // con su número de referencia (REF=300 para todos los identificadores).
        String contenidoTabla = generarContenidoTabla(tablaSimbolos);
        try {
            Files.writeString(Path.of(rutaTabla), contenidoTabla);
            System.out.println("Tabla de símbolos generada : " + rutaTabla);
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo escribir el archivo '" + rutaTabla + "'");
        }

        // ── Generar archivo .tok (tabla de símbolos + lista de tokens) ───────
        //se llama al metodo generarArchivoTok() que construye el contenido completo del archivo .tok
        // incluyendo la tabla de símbolos, la lista de tokens con formato detallado, un resumen por categoría y los errores léxicos encontrados.
        String contenidoTok = generarArchivoTok(tablaSimbolos, tokens, alex.getErrores());
        try {
            Files.writeString(Path.of(rutaTokens), contenidoTok);
            System.out.println("Archivo de tokens generado : " + rutaTokens);
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo escribir el archivo '" + rutaTokens + "'");
        }

        // ── Mostrar resumen en consola ───────────────────────────────────────
        System.out.println("\n" + contenidoTok);
    }

        // GENERAR CONTENIDO DEL ARCHIVO .tok
        /*
        * Genera el contenido completo del archivo progfte.tok con el siguiente formato:
        * ── Encabezado con título y nombre del archivo
        * ── Lista de tokens encontrados con formato tabular:
        *   Num | Token | Lexema | Referencia | Linea
        * ── Resumen por categoría de tokens (no implementado en esta versión)
        * ── Sección de errores léxicos encontrados durante el análisis
        * El método recibe la tabla de símbolos, la lista de tokens y la lista de errores léxicos para incluir toda esta información en el archivo .tok.
     */ 
   private static String generarArchivoTok(Map<String, String[]> tabla,
                                         List<Token> tokens,
                                         List<String> errores) {
    StringBuilder sb = new StringBuilder();
    String separador = "=".repeat(65) + "\n";
    String divisor   = "-".repeat(65) + "\n";

    // ── Encabezado del archivo ───────────────────────────────────────────────
    sb.append(separador);
    sb.append("  ANALIZADOR LÉXICO — LENGUAJE RAFI\n");
    sb.append("  Archivo: progfte.tok\n");
    sb.append(separador);
    sb.append("\n");

    // ── LISTA DE TOKENS ───────────────────────────────────────────────────────
    // Muestra cada token encontrado en el código fuente con el formato:
    // Num | Token | Lexema | Referencia | Linea
    // donde:
    //   Num        → número consecutivo del token en la lista
    //   Token      → tipo específico del token (INICIO, SUMA, ID, etc.)
    //   Lexema     → texto original del código fuente
    //   Referencia → número de referencia numérica del token
    //   Linea      → renglón del archivo fuente donde fue encontrado
    sb.append(String.format(" %-5s | %-22s | %-20s | %-10s | %s%n",
                            "NUM", "TOKEN", "LEXEMA", "REFERENCIA", "LINEA"));
    sb.append(divisor);

    int contador = 1;
    for (Token t : tokens) {
        sb.append(String.format(" %-5d | %-22s | %-20s | %-10d | %d%n",
                                contador++, t.tipo, t.lexema, t.ref, t.linea));
    }
    sb.append("\n");

    // ── ERRORES LÉXICOS ───────────────────────────────────────────────────────
    // Lista todos los símbolos no reconocidos encontrados durante el análisis.
    // El analizador no se detiene al encontrar errores, los acumula todos
    // para reportarlos juntos al final facilitando la corrección del código.
    sb.append(separador);
    sb.append("  ERRORES LÉXICOS\n");
    sb.append(separador);
    if (errores.isEmpty()) {
        sb.append("  Sin errores léxicos.\n");
    } else {
        for (String error : errores) {
            sb.append("  " + error + "\n");
        }
    }
    sb.append(separador);

    return sb.toString();
}

    //  CONSTRUIR TABLA DE SÍMBOLOS
    /*
     * Construye la tabla de símbolos recorriendo la lista de tokens.
     * Busca el patrón de declaración de variables del lenguaje RAFI:
     *   decl → tipo nombreVar1, nombreVar2 ; → tipo nombreVar3 ; → inicio
      * Cada variable se almacena en un Map con:
     *   Clave   → nombre de la variable (lexema del identificador)
     *   Valor   → arreglo String[] con tres posiciones:
     *               [0] tipo de dato    ("entero", "cadena", "booleano")
     *               [1] valor inicial   ("0", '""', "false")
     *               [2] número de línea donde fue declarada
    */
    private static Map<String, String[]> construirTablaSimbolos(List<Token> tokens) {
        Map<String, String[]> tabla = new LinkedHashMap<>();
        boolean dentroDecl = false;
        String tipoActual  = "";
        int i = 0;

        while (i < tokens.size()) {
            Token t = tokens.get(i);
            // Al encontrar DECL se activa la bandera para empezar a registrar variables
            if (t.tipo.equals("DECL")) {
                dentroDecl = true;
                i++;
                continue;
            }

            // Al encontrar INICIO se desactiva la bandera — ya no hay más declaraciones
            if (t.tipo.equals("INICIO")) {
                dentroDecl = false;
                i++;
                continue;
            }

            // Dentro de decl: al encontrar un TIPO se leen todos los IDs hasta el ";"
            if (dentroDecl && t.tipo.equals("TIPO")) {
                tipoActual = t.lexema; //guarda entero, cadena, booleano
                i++;

                // Avanzar hasta el punto y coma registrando cada identificador encontrado
                while (i < tokens.size() && !tokens.get(i).tipo.equals("PC")) {
                    Token actual = tokens.get(i);
                    if (actual.nombre.equals("IDENTIFICADOR")) {
                        String nombre = actual.lexema;
                        if (tabla.containsKey(nombre)) {
                            System.out.println("ADVERTENCIA: Variable '" + nombre +
                                               "' declarada mas de una vez (línea " + actual.linea + ")");
                        } else {
                            tabla.put(nombre, new String[]{
                                tipoActual,
                                valorPorDefecto(tipoActual),
                                String.valueOf(actual.linea)
                            });
                        }
                    }
                    i++;
                }
                i++; // saltar el punto y coma
                continue;
            }

            i++;
        }

        return tabla;
    }

    //  GENERAR CONTENIDO DEL ARCHIVO .tab
    /*
    * Genera el contenido del archivo progfte.tab.
     *
     * Produce una tabla formateada con todas las variables declaradas
     * en la sección decl del programa fuente. Incluye la columna REF
     * con el número de referencia de los identificadores (siempre 300).
     *
     */
    private static String generarContenidoTabla(Map<String, String[]> tabla) {
        StringBuilder sb = new StringBuilder();

        // Encabezado con columna REF incluida
        sb.append(String.format("%-5s | %-20s | %-12s | %-10s | %-6s | %s%n",
                            "No.", "VARIABLE", "TIPO", "VALOR INIT", "REF", "LINEA"));
        sb.append("-".repeat(70) + "\n");

        int contador = 1;
        for (Map.Entry<String, String[]> entrada : tabla.entrySet()) {
            sb.append(String.format("%-5d | %-20s | %-12s | %-10s | %-6d | %s%n",
                                contador++,
                                entrada.getKey(),//nombre de la variable
                                entrada.getValue()[0],//tipo: entero, cadena, booleano
                                entrada.getValue()[1],//valor inicial 0,"", false
                                300, //Ref siempre sera 300 para los identificadores
                                entrada.getValue()[2]));//linea de declaracion
        }
        return sb.toString();
    }

    //  VALOR POR DEFECTO según tipo
     /*
     * Devuelve el valor inicial por defecto para un tipo de dato dado.
     * Esto se usa al construir la tabla de símbolos para asignar un valor
     * inicial a cada variable declarada, aunque el lenguaje RAFI no lo requiera explícitamente.
     *
     * Para tipos no reconocidos, devuelve "indefinido" como indicador de error.
     */
    private static String valorPorDefecto(String tipo) {
        switch (tipo) {
            case "entero":   return "0";
            case "cadena":   return "\"\"";
            case "booleano": return "false";
            default:         return "indefinido";
        }
    }

    //  MÉTODO DEPURADOR
    /*
     * Recorre el código fuente carácter a carácter y construye una versión
     * limpia eliminando todo lo que no forma parte de la lógica del programa.
     * Reglas de depuración aplicadas:
     *   Comentarios /* ... *\/  → se eliminan completamente sin dejar rastro
     *   Tabuladores \t          → se reemplazan por un espacio simple
     *   Retornos de carro \r    → se eliminan
     *   Espacios múltiples      → se reducen a un solo espacio
     *   Líneas vacías           → se eliminan, no se genera salto de línea
     */
    private static String depurar(String entrada) {
        StringBuilder resultado = new StringBuilder();
        int i = 0;

        while (i < entrada.length()) {
            char c = entrada.charAt(i);
            //detectar el inicio de un comentario y saltar todo su contenido hasta el cierre del mismo
            if (c == '/' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < entrada.length()) {
                    if (entrada.charAt(i) == '*' && entrada.charAt(i + 1) == '/') {
                        i += 2;
                        break;
                    }
                    i++;
                }
                continue;
            }

            //salto de linea donde solo se conserva si la linea actual tiene contenido
            //esto elimina las lineas vacias del archivo depurado.
            if (c == '\n') {
                String lineaActual = resultado.toString();
                int ultimoSalto    = lineaActual.lastIndexOf('\n');
                String ultimaLinea = (ultimoSalto == -1)
                        ? lineaActual.trim()
                        : lineaActual.substring(ultimoSalto + 1).trim();
                if (!ultimaLinea.isEmpty()) {
                    resultado.append('\n');
                }
                i++;
                continue;
            }

            //tabulador -> reemmplaza por un espacio simple para mantener la legibilidad del codigo
            if (c == '\t' || c == '\r') {
                if (c == '\t') resultado.append(' ');
                i++;
                continue;
            }

            // Espacio → solo agregar si el carácter anterior no era ya un espacio
            // Esto colapsa múltiples espacios consecutivos en uno solo
            if (c == ' ') {
                if (resultado.length() > 0 &&
                    resultado.charAt(resultado.length() - 1) != ' ' &&
                    resultado.charAt(resultado.length() - 1) != '\n') {
                    resultado.append(' ');
                }
                i++;
                continue;
            }
            // Cualquier otro carácter se copia tal cual al resultado depurado
            resultado.append(c);
            i++;
        }
        // trim() elimina espacios o saltos residuales al inicio y al final del resultado
        return resultado.toString().trim();
    }
}