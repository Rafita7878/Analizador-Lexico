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

        // ── Ruta del archivo fuente ──────────────────────────────────────────
        // Puedes cambiar esta ruta por la ubicación de tu archivo .txt
        String rutaArchivo = "progfte.txt";
        String rutaDepurado = "progfte.dep";
        String rutaTabla = "progfte.tab";


        // ── Leer el archivo completo a un String ─────────────────────────────
        String codigoFuente;
        try {
            codigoFuente = Files.readString(Path.of(rutaArchivo));
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo leer el archivo '" + rutaArchivo + "'");
            System.out.println("Verifica que el archivo existe y está en la carpeta correcta.");
            return;
        }

        // Generar archivo depurado .dep
        String codigoDepurado = depurar(codigoFuente);
        try{
            Files.writeString(Path.of(rutaDepurado), codigoDepurado);
            System.out.println("Archivo depurado generado: " + rutaDepurado);
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo escribir el archivo depurado '" + rutaDepurado + "'");
            System.out.println("Verifica que tienes permisos de escritura en la carpeta.");
        }

        // ── Analizar el codigo fuente original ─────────────────────────────────────────────────────────
        AnalizadorLexico alex = new AnalizadorLexico();
        List<Token> tokens = alex.analizar(codigoFuente);

        //generar tabla de simbolos .tab
        Map<String, String[]> tablaSimbolos = construirTablaSimbolos(tokens);
        String contenidoTabla = generarArchivoTabla(tablaSimbolos); 
        try {
            Files.writeString(Path.of(rutaTabla), contenidoTabla);
            System.out.println("Tabla de símbolos generada: " + rutaTabla);
        } catch (IOException e) {
            System.out.println("ERROR: No se pudo escribir el archivo '" + rutaTabla + "'");
        }
        
        //imprimir tabla de simbolos en consola
        System.out.println("\n------------------------------------------------");
        System.out.println("  TABLA DE SIMBOLOS -> " + rutaArchivo);
        System.out.println("------------------------------------------------");
       System.out.print(contenidoTabla);
       
        // ── Tabla de tokens ──────────────────────────────────────────────────
        System.out.println("-----------------------------------------------------");
        System.out.println("  TABLA DE TOKENS -> " + rutaArchivo);
        System.out.println("------------------------------------------------------");
        System.out.printf("%-8s | %-20s | %-22s | %s%n",
                          "LINEA", "NOMBRE", "TIPO", "LEXEMA");
        System.out.println("------------------------------------------------------");

        for (Token t : tokens) {
            System.out.println(t);
        }

        // ── Errores léxicos ──────────────────────────────────────────────────
        System.out.println("\n-----------------------------------------------------------");
        List<String> errores = alex.getErrores();
        if (errores.isEmpty()) {
            System.out.println("Análisis completado sin errores léxicos.");
        } else {
            System.out.println("  ERRORES LÉXICOS ENCONTRADOS:");
            errores.forEach(System.out::println);
        }
        System.out.println("-----------------------------------------------------------");

        // ── Resumen ──────────────────────────────────────────────────────────
        System.out.println("\n  Total de tokens generados : " + tokens.size());
        System.out.println("  Total de errores léxicos  : " + errores.size());
        System.out.println("Total de variables declaradas : " + tablaSimbolos.size());
    }

    //METODO PARA CONSTRUIR LA TABLA DE SIMBOLOS A PARTIR DE LOS TOKENS ANALIZADOS
    private static Map<String, String[]> construirTablaSimbolos(List<Token> tokens) {
        // Clave   → nombre de la variable
        // Valor   → arreglo con [tipo, valorInicial, linea]
        Map<String, String[]> tabla = new LinkedHashMap<>();

        boolean dentroDecl = false; // true cuando estamos en la sección decl
        String tipoActual  = "";    // tipo que se está procesando (entero, cadena, booleano)
        int i = 0;

        while (i < tokens.size()) {
            Token t = tokens.get(i);

            // Detectar inicio de sección decl
            if (t.tipo.equals("DECL")) {
                dentroDecl = true;
                i++;
                continue;
            }

            // Detectar fin de sección decl cuando llega "inicio"
            if (t.tipo.equals("INICIO")) {
                dentroDecl = false;
                i++;
                continue;
            }

            // Solo procesar dentro de decl
            if (dentroDecl) {

                // Encontramos un tipo de dato → guardar y leer las variables que siguen
                if (t.tipo.equals("TIPO")) {
                    tipoActual = t.lexema; // entero, cadena o booleano
                    i++;

                    // Leer todos los identificadores hasta el punto y coma
                    while (i < tokens.size() && !tokens.get(i).tipo.equals("PC")) {
                        Token actual = tokens.get(i);

                        // Si es un identificador → agregarlo a la tabla
                        if (actual.nombre.equals("IDENTIFICADOR")) {
                            String nombre      = actual.lexema;
                            String valorInicial = valorPorDefecto(tipoActual);
                            String linea       = String.valueOf(actual.linea);

                            // Verificar si la variable ya fue declarada (error semántico)
                            if (tabla.containsKey(nombre)) {
                                System.out.println("ADVERTENCIA: Variable '" + nombre +
                                                   "' declarada más de una vez (línea " + linea + ")");
                            } else {
                                tabla.put(nombre, new String[]{tipoActual, valorInicial, linea});
                            }
                        }
                        // Las comas (COMA) simplemente se saltan
                        i++;
                    }
                    i++; // saltar el punto y coma
                    continue;
                }
            }

            i++;
        }

        return tabla;
    }

    //  GENERAR CONTENIDO DEL ARCHIVO .tab A PARTIR DE LA TABLA DE SÍMBOLOS
    private static String generarArchivoTabla(Map<String, String[]> tabla) {
        StringBuilder sb = new StringBuilder();

        // Encabezado de la tabla
        sb.append(String.format("%-5s | %-20s | %-12s | %-10s | %s%n",
                                "No.", "NOMBRE VARIABLE", "TIPO", "VALOR", "LÍNEA"));
        sb.append("-----------------------------------------------------------\n");

        // Filas de variables
        int contador = 1;
        for (Map.Entry<String, String[]> entrada : tabla.entrySet()) {
            String nombre = entrada.getKey();
            String tipo   = entrada.getValue()[0];
            String valor  = entrada.getValue()[1];
            String linea  = entrada.getValue()[2];

            sb.append(String.format("%-5d | %-20s | %-12s | %-10s | %s%n",
                                    contador++, nombre, tipo, valor, linea));
        }

        return sb.toString();
    }

    //  VALOR POR DEFECTO según el tipo 
    //  entero   -> 0
    //  cadena   -> ""  (cadena vacía)
    //  booleano -> false
    private static String valorPorDefecto(String tipo) {
        switch (tipo) {
            case "entero":   return "0";
            case "cadena":   return "\"\"";
            case "booleano": return "false";
            default:         return "indefinido";
        }
    }

    //METODO PARA DEPURAR EL CODIGO FUENTE (ELIMINAR COMENTARIOS Y ESPACIOS INNECESARIOS)
    private static String depurar(String entrada) {
        // Usamos StringBuilder para construir el código depurado de manera eficiente
        StringBuilder resultado = new StringBuilder();
        int i = 0;

        // Recorrer cada carácter del código fuente
        while ( i < entrada.length()) {
            char c = entrada.charAt(i);

            // Eliminar comentarios /* ... */
            if (c == '/' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '*') {
                i += 2; // Saltar "/*"
                while (i + 1 < entrada.length()) {
                    if (entrada.charAt(i) == '*' && entrada.charAt(i + 1) == '/') {
                        i += 2; // Saltar "*/"
                        break;
                    }
                    i++;
                }
                continue;
            }

            //conservar saltos de linea para mantener la estructura
            if (c == '\n'){
                // Solo agregar el salto si la línea actual no está vacía
                String lineaActual = resultado.toString();
                int ultimoSalto   = lineaActual.lastIndexOf('\n');
                // Obtener el contenido de la última línea después del último salto de línea
                String ultimaLinea = (ultimoSalto == -1)
                        ? lineaActual.trim()// Si no hay saltos, toda la cadena es la última línea
                        : lineaActual.substring(ultimoSalto + 1).trim();// Si hay saltos, obtener la última línea después del último salto

                if (!ultimaLinea.isEmpty()) {
                    resultado.append('\n');
                }
                i++;
                continue;
            }

            //Eliminar tabuladores o retornos de carro
            if(c == '\t' || c == '\r'){
                // Reemplazar tabulador por un espacio simple
                if (c == '\t') resultado.append(' ');
                i++;
                continue;
            }

             // Eliminar espacios múltiples consecutivos -> dejar solo uno
            if (c == ' ') {
                // Verificar si el último carácter agregado ya es un espacio
                if (resultado.length() > 0 &&
                    resultado.charAt(resultado.length() - 1) != ' ' &&
                    resultado.charAt(resultado.length() - 1) != '\n') {
                    resultado.append(' ');
                }
                i++;
                continue;
            }

            //cualquiera otro carácter se agrega al resultado
            resultado.append(c);
            i++;
        }

        return resultado.toString().trim(); // Eliminar espacios al inicio y al final
    }

}

