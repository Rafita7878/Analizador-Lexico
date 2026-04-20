// Main.java
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
        String rutaArchivo  = "progfte.txt";
        String rutaDepurado = "progfte.dep";
        String rutaTabla    = "progfte.tab";
        String rutaTokens   = "progfte.tok";

        // ── Leer el archivo fuente ───────────────────────────────────────────
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

        // ── Mostrar resumen en consola ───────────────────────────────────────
        System.out.println("\n" + contenidoTok);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  GENERAR ARCHIVO .tok
    //  Contiene: encabezado, tabla de símbolos y lista completa de tokens
    // ────────────────────────────────────────────────────────────────────────
    private static String generarArchivoTok(Map<String, String[]> tabla,
                                             List<Token> tokens,
                                             List<String> errores) {
        StringBuilder sb = new StringBuilder();
        String separador  = "=".repeat(65) + "\n";
        String divisor    = "-".repeat(65) + "\n";

        // ── Encabezado del archivo ───────────────────────────────────────────
        sb.append(separador);
        sb.append("  ANALIZADOR LÉXICO — LENGUAJE RAFI\n");
        sb.append("  Archivo: progfte.tok\n");
        sb.append(separador);
        sb.append("\n");

        // ── SECCIÓN 1: Tabla de símbolos ─────────────────────────────────────
        sb.append(separador);
        sb.append("  SECCIÓN 1 — TABLA DE SÍMBOLOS\n");
        sb.append(separador);
        sb.append(String.format("%-5s | %-20s | %-12s | %-10s | %s%n",
                                "No.", "VARIABLE", "TIPO", "VALOR INIT", "LÍNEA"));
        sb.append(divisor);

        if (tabla.isEmpty()) {
            sb.append("  (No se declararon variables)\n");
        } else {
            int contador = 1;
            for (Map.Entry<String, String[]> entrada : tabla.entrySet()) {
                String nombre = entrada.getKey();
                String tipo   = entrada.getValue()[0];
                String valor  = entrada.getValue()[1];
                String linea  = entrada.getValue()[2];
                sb.append(String.format("%-5d | %-20s | %-12s | %-10s | %s%n",
                                        contador++, nombre, tipo, valor, linea));
            }
        }
        sb.append("\n");

        // ── SECCIÓN 2: Lista de tokens clasificados ──────────────────────────
        sb.append(separador);
        sb.append("  SECCIÓN 2 — LISTA DE TOKENS\n");
        sb.append(separador);
        sb.append(String.format("%-6s | %-22s | %-22s | %s%n",
                                "LÍNEA", "NOMBRE", "TIPO", "LEXEMA"));
        sb.append(divisor);

        // Agrupar tokens por categoría para mejor lectura
        //String categoriaActual = "";
        for (Token t : tokens) {
            sb.append(String.format("%-6d | %-22s | %-22s | %s%n",
                            t.linea, t.nombre, t.tipo, t.lexema));
        }
        sb.append("\n");

        // ── SECCIÓN 3: Resumen por categoría ─────────────────────────────────
        sb.append(separador);
        sb.append("  SECCIÓN 3 — RESUMEN\n");
        sb.append(separador);

        // Contar tokens por categoría
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (Token t : tokens) {
            conteo.put(t.nombre, conteo.getOrDefault(t.nombre, 0) + 1);
        }
        for (Map.Entry<String, Integer> entrada : conteo.entrySet()) {
            sb.append(String.format("  %-25s : %d token(s)%n",
                                    entrada.getKey(), entrada.getValue()));
        }
        sb.append(divisor);
        sb.append(String.format("  %-25s : %d%n", "TOTAL DE TOKENS",   tokens.size()));
        sb.append(String.format("  %-25s : %d%n", "TOTAL DE VARIABLES", tabla.size()));
        sb.append(String.format("  %-25s : %d%n", "TOTAL DE ERRORES",   errores.size()));
        sb.append("\n");

        // ── SECCIÓN 4: Errores léxicos ───────────────────────────────────────
        sb.append(separador);
        sb.append("  SECCIÓN 4 — ERRORES LÉXICOS\n");
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

    // ────────────────────────────────────────────────────────────────────────
    //  CONSTRUIR TABLA DE SÍMBOLOS
    // ────────────────────────────────────────────────────────────────────────
    private static Map<String, String[]> construirTablaSimbolos(List<Token> tokens) {
        Map<String, String[]> tabla = new LinkedHashMap<>();
        boolean dentroDecl = false;
        String tipoActual  = "";
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
                                               "' declarada más de una vez (línea " + actual.linea + ")");
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

    // ────────────────────────────────────────────────────────────────────────
    //  GENERAR CONTENIDO DEL ARCHIVO .tab
    // ────────────────────────────────────────────────────────────────────────
    private static String generarContenidoTabla(Map<String, String[]> tabla) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-5s | %-20s | %-12s | %-10s | %s%n",
                                "No.", "VARIABLE", "TIPO", "VALOR INIT", "LÍNEA"));
        sb.append("-".repeat(65) + "\n");

        int contador = 1;
        for (Map.Entry<String, String[]> entrada : tabla.entrySet()) {
            sb.append(String.format("%-5d | %-20s | %-12s | %-10s | %s%n",
                                    contador++,
                                    entrada.getKey(),
                                    entrada.getValue()[0],
                                    entrada.getValue()[1],
                                    entrada.getValue()[2]));
        }
        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  VALOR POR DEFECTO según tipo
    // ────────────────────────────────────────────────────────────────────────
    private static String valorPorDefecto(String tipo) {
        switch (tipo) {
            case "entero":   return "0";
            case "cadena":   return "\"\"";
            case "booleano": return "false";
            default:         return "indefinido";
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  MÉTODO DEPURADOR
    // ────────────────────────────────────────────────────────────────────────
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

            if (c == '\t' || c == '\r') {
                if (c == '\t') resultado.append(' ');
                i++;
                continue;
            }

            if (c == ' ') {
                if (resultado.length() > 0 &&
                    resultado.charAt(resultado.length() - 1) != ' ' &&
                    resultado.charAt(resultado.length() - 1) != '\n') {
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