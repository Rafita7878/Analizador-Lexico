// Main.java
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        // ── Ruta del archivo fuente ──────────────────────────────────────────
        // Puedes cambiar esta ruta por la ubicación de tu archivo .txt
        String rutaArchivo = "progfte.txt";
        String rutaDepurado = "progfte.dep";


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

        // ── Tabla de tokens ──────────────────────────────────────────────────
        System.out.println("-----------------------------------------------------");
        System.out.println("  TABLA DE TOKENS — " + rutaArchivo);
        System.out.println("------------------------------------------------------");
        System.out.printf("%-8s | %-20s | %-22s | %s%n",
                          "LINEA", "NOMBRE", "TIPO", "LEXEMA");
        System.out.println("───────────────────────────────────────────────────────────────────");

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
    }
    

    //METODO PARA DEPURAR EL CODIGO FUENTE (ELIMINAR COMENTARIOS Y ESPACIOS INNECESARIOS)
    private static String depurar(String entrada) {
    }
}

