
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
        String rutaArchivo = "progfte.txt";// archivo fuente original
        String rutaDepurado = "progfte.dep";// archivo fuente depurado (sin comentarios ni espacios innecesarios)
        String rutaTabla = "progfte.tab";// archivo con la tabla de símbolos (variables declaradas)
        String rutaTokens = "progfte.tok";// archivo completo con tabla de símbolos + lista de tokens + resumen +
        // errores

        // ── Leer el archivo fuente ───────────────────────────────────────────
        // Si el archivo no existe o no se puede leer, se muestra un mensaje de error y
        // se detiene la ejecución
        String codigoFuente;
        try {
            codigoFuente = Files.readString(Path.of(rutaArchivo));
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo leer el archivo '" + rutaArchivo + "'");
            System.out.println("Verifica que el archivo existe y está en la carpeta correcta.");
            return;
        }

        // ── Generar archivo depurado .dep ────────────────────────────────────
        String codigoDepurado = depurar(codigoFuente);
        try {
            Files.writeString(Path.of(rutaDepurado), codigoDepurado);
            System.out.println("Archivo depurado generado  : " + rutaDepurado);
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo escribir el archivo '" + rutaDepurado + "'");
        }

        // ── Analizar el código fuente ────────────────────────────────────────
        AnalizadorLexico alex = new AnalizadorLexico();
        List<Token> tokens = alex.analizar(codigoFuente);

        // ── Construir tabla de símbolos ──────────────────────────────────────
        Map<String, String[]> tablaSimbolos = construirTablaSimbolos(tokens);

        // ── Generar archivo .tab ─────────────────────────────────────────────
        String contenidoTabla = generarContenidoTabla(tablaSimbolos);
        try {
            Files.writeString(Path.of(rutaTabla), contenidoTabla);
            System.out.println("Tabla de símbolos generada : " + rutaTabla);
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo escribir el archivo '" + rutaTabla + "'");
        }

        // ── Generar archivo .tok (tabla de símbolos + lista de tokens) ───────
        String contenidoTok = generarArchivoTok(tablaSimbolos, tokens, alex.getErrores());
        try {
            Files.writeString(Path.of(rutaTokens), contenidoTok);
            System.out.println("Archivo de tokens generado : " + rutaTokens);
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo escribir el archivo '" + rutaTokens + "'");
        }

        // ── Ejecutar el analizador sintáctico ────────────────────────────────────
        AnalizadorSintactico sint = new AnalizadorSintactico(tokens, tablaSimbolos);
        NodoArbol arbol = sint.analizar();
        List<ErrorSintactico> erroresSint = sint.getErrores();

        // ── Árbol SOLO si no hay errores sintácticos ──────────────────────────────
        if (!sint.tieneErrores()) {
            System.out.println("\n=== ÁRBOL DE ANÁLISIS SINTÁCTICO ===");
            arbol.imprimir("");
            try {
                Files.writeString(Path.of("progfte.ast"), arbol.aTexto(""));
                System.out.println("Arbol generado: progfte.ast");
            } catch (IOException e) {
                System.out.println("ERROR: No se pudo escribir el arbol");
            }
        } else {
            System.out.println("\n=== ARBOL NO GENERADO — el programa contiene errores sintacticos ===");
        }

        // ── Mostrar errores sintácticos ───────────────────────────────────────────
        System.out.println("\n=== ERRORES SINTACTICOS ===");
        if (erroresSint.isEmpty()) {
            System.out.println("Sin errores sintacticos.");
        } else {
            erroresSint.forEach(System.out::println);
        }

        // ── Ejecutar el analizador semántico — SOLO si no hay errores sintácticos ──
        List<ErrorSemantico> erroresSem = new ArrayList<>();
        if (!sint.tieneErrores()) {
            AnalizadorSemantico sem = new AnalizadorSemantico();
            sem.analizar(arbol);
            erroresSem = sem.getErrores();
        }

        System.out.println("\n=== ERRORES SEMANTICOS ===");
        if (sint.tieneErrores()) {
            System.out.println("No se ejecutó el análisis semántico — corrige primero los errores sintácticos.");
        } else if (erroresSem.isEmpty()) {
            System.out.println("Sin errores semanticos.");
        } else {
            erroresSem.forEach(System.out::println);
        }

        // ── Generar archivo progfte.sem con el resultado del análisis semántico ──
        StringBuilder sbSem = new StringBuilder();
        sbSem.append("=".repeat(65)).append("\n");
        sbSem.append("  ANALIZADOR SEMANTICO — LENGUAJE RAFI\n");
        sbSem.append("=".repeat(65)).append("\n\n");
        if (sint.tieneErrores()) {
            sbSem.append("No se ejecutó el análisis semántico — hay errores sintácticos previos.\n");
        } else if (erroresSem.isEmpty()) {
            sbSem.append("Sin errores semanticos. El programa es semánticamente correcto.\n");
        } else {
            for (ErrorSemantico e : erroresSem) sbSem.append(e.toString()).append("\n");
        }
        try {
            Files.writeString(Path.of("progfte.sem"), sbSem.toString());
            System.out.println("Archivo semántico generado : progfte.sem");
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo escribir el archivo 'progfte.sem'");
        }

        // ── Mostrar resumen en consola ───────────────────────────────────────
        System.out.println("\n" + contenidoTok);
    }

    // GENERAR CONTENIDO DEL ARCHIVO .tok
    private static String generarArchivoTok(Map<String, String[]> tabla,
                                            List<Token> tokens,
                                            List<String> errores) {
        StringBuilder sb = new StringBuilder();
        String separador = "=".repeat(65) + "\n";
        String divisor = "-".repeat(65) + "\n";

        sb.append(separador);
        sb.append("  ANALIZADOR LÉXICO — LENGUAJE RAFI\n");
        sb.append("  Archivo: progfte.tok\n");
        sb.append(separador);
        sb.append("\n");

        sb.append(String.format(" %-5s | %-22s | %-20s | %-10s | %s%n",
                "NUM", "TOKEN", "LEXEMA", "REFERENCIA", "LINEA"));
        sb.append(divisor);

        int contador = 1;
        for (Token t : tokens) {
            sb.append(String.format(" %-5d | %-22s | %-20s | %-10d | %d%n",
                    contador++, t.tipo, t.lexema, t.ref, t.linea));
        }
        sb.append("\n");

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

    // CONSTRUIR TABLA DE SÍMBOLOS
    private static Map<String, String[]> construirTablaSimbolos(List<Token> tokens) {
        Map<String, String[]> tabla = new LinkedHashMap<>();
        boolean dentroDecl = false;
        String tipoActual = "";
        int i = 0;

        while (i < tokens.size()) {
            Token t = tokens.get(i);
            if (t.tipo.equals("DECL")) {
                dentroDecl = true;
                i++;
                continue;
            }

            if (t.tipo.equals("INICIO")) {
                dentroDecl = false;
                i++;
                continue;
            }

            if (dentroDecl && t.tipo.equals("TIPO")) {
                tipoActual = t.lexema;
                i++;

                while (i < tokens.size() && !tokens.get(i).tipo.equals("PC")) {
                    Token actual = tokens.get(i);
                    if (actual.nombre.equals("IDENTIFICADOR")) {
                        String nombre = actual.lexema;
                        if (tabla.containsKey(nombre)) {
                            System.out.println("ADVERTENCIA: Variable '" + nombre +
                                    "' declarada mas de una vez (línea " + actual.linea + ")");
                        } else {
                            tabla.put(nombre, new String[] {
                                    tipoActual,
                                    valorPorDefecto(tipoActual),
                                    String.valueOf(actual.linea)
                            });
                        }
                    }
                    i++;
                }
                i++;
                continue;
            }

            i++;
        }

        return tabla;
    }

    // GENERAR CONTENIDO DEL ARCHIVO .tab
    private static String generarContenidoTabla(Map<String, String[]> tabla) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%-5s | %-20s | %-12s | %-10s | %-6s | %s%n",
                "No.", "VARIABLE", "TIPO", "VALOR INIT", "REF", "LINEA"));
        sb.append("-".repeat(70) + "\n");

        int contador = 1;
        for (Map.Entry<String, String[]> entrada : tabla.entrySet()) {
            sb.append(String.format("%-5d | %-20s | %-12s | %-10s | %-6d | %s%n",
                    contador++,
                    entrada.getKey(),
                    entrada.getValue()[0],
                    entrada.getValue()[1],
                    300,
                    entrada.getValue()[2]));
        }
        return sb.toString();
    }

    // VALOR POR DEFECTO según tipo
    private static String valorPorDefecto(String tipo) {
        switch (tipo) {
            case "entero":
                return "0";
            case "cadena":
                return "\"\"";
            case "booleano":
                return "false";
            default:
                return "indefinido";
        }
    }

    // MÉTODO DEPURADOR
    private static String depurar(String entrada) {
        StringBuilder resultado = new StringBuilder();
        int i = 0;

        while (i < entrada.length()) {
            char c = entrada.charAt(i);

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

            if (c == '\n' || c == '\t' || c == '\r') {
                if (resultado.length() > 0 &&
                        resultado.charAt(resultado.length() - 1) != ' ') {
                    resultado.append(' ');
                }
                i++;
                continue;
            }

            if (c == ' ') {
                if (resultado.length() > 0 &&
                        resultado.charAt(resultado.length() - 1) != ' ') {
                    resultado.append(' ');
                }
                i++;
                continue;
            }

            resultado.append(c);
            i++;
        }

        return resultado.toString().trim();
    }
}