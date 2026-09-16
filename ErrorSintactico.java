// ErrorSintactico.java
// Clase modelo para representar un error sintáctico encontrado durante el análisis.
// Guarda el mensaje del error, la línea y la columna donde ocurrió.

public class ErrorSintactico {

    // Descripción del error encontrado
    public String mensaje;

    // Línea del archivo fuente donde ocurrió el error
    public int linea;

    // Columna del archivo fuente donde ocurrió el error
    public int columna;

    public ErrorSintactico(String mensaje, int linea, int columna) {
        this.mensaje = mensaje;
        this.linea   = linea;
        this.columna = columna;
    }

    @Override
    public String toString() {
        return "Error sintáctico en renglón " + linea + ", columna " + columna + ": " + mensaje;
    }
}