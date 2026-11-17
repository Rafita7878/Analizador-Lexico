// CambioOptimizacion.java
// Registro de UN cambio hecho por una pasada de optimización: qué pasada fue,
// en qué vuelta y bloque, la instrucción antes y después, y el motivo.
// Si la instrucción se eliminó, 'despues' queda como "(eliminada)";
// si se insertó, 'antes' queda como "(nueva)".
public class CambioOptimizacion {

    public int    vuelta;
    public String pasada;
    public int    bloque;     // número de bloque básico, -1 si no aplica
    public String antes;
    public String despues;
    public String motivo;

    public CambioOptimizacion(int vuelta, String pasada, int bloque,
                              String antes, String despues, String motivo) {
        this.vuelta  = vuelta;
        this.pasada  = pasada;
        this.bloque  = bloque;
        this.antes   = antes;
        this.despues = despues;
        this.motivo  = motivo;
    }

    @Override
    public String toString() {
        String b = (bloque >= 0) ? "B" + bloque : "--";
        return String.format("[v%d %-10s %-3s] %-28s -> %-28s (%s)",
                vuelta, pasada, b, antes.trim(), despues.trim(), motivo);
    }
}
