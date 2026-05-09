// ErrorSintactico.java
// Clase modelo para representar un error sintáctico encontrado durante el análisis.
// Guarda el mensaje del error y el número de línea donde ocurrió.

public class ErrorSintactico {

    // Descripción del error encontrado
    public String mensaje;

    // Línea del archivo fuente donde ocurrió el error
    public int linea;

    public ErrorSintactico(String mensaje, int linea) {
        this.mensaje = mensaje;
        this.linea   = linea;
    }

    @Override
    public String toString() {
        return "Error sintáctico en renglón " + linea + ": " + mensaje;
    }
}