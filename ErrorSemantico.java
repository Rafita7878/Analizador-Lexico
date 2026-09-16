// ErrorSemantico.java
// Clase modelo para representar un error semántico encontrado durante el análisis.
// Guarda el mensaje del error, la línea y la columna donde ocurrió.

public class ErrorSemantico {

    // Descripción del error encontrado
    public String mensaje;

    // Línea del archivo fuente donde ocurrió el error
    public int linea;

    // Columna del archivo fuente donde ocurrió el error
    public int columna;

    public ErrorSemantico(String mensaje, int linea, int columna) {
        this.mensaje = mensaje;
        this.linea   = linea;
        this.columna = columna;
    }

    @Override
    public String toString() {
        return "Error semántico en renglón " + linea + ", columna " + columna + ": " + mensaje;
    }
}