// ErrorEjecucion.java
// Excepción controlada para errores que solo pueden detectarse en tiempo de
// ejecución (por ejemplo, una división entre cero cuyo divisor es una
// variable cuyo valor real solo se conoce al correr el programa, no al
// analizarlo). Se usa para que Ejecutor pueda detener la ejecución de forma
// limpia, en vez de que Java "truene" con una excepción cruda sin control.
public class ErrorEjecucion extends RuntimeException {

    public int linea;
    public int columna;

    public ErrorEjecucion(String mensaje, int linea, int columna) {
        super(mensaje);
        this.linea = linea;
        this.columna = columna;
    }

    @Override
    public String toString() {
        return "Error en tiempo de ejecución en renglón " + linea + ", columna " + columna + ": " + getMessage();
    }
}
