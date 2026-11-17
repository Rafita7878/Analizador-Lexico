import java.util.List;

// Pasada.java
// Una optimización independiente sobre la lista de cuádruplos.
// Cada pasada modifica la lista "en su lugar" y registra cada cambio que hace
// en el contexto (ctx.registrar). Devuelve true si cambió algo, para que el
// Optimizador sepa si debe dar otra vuelta (hasta llegar a un punto fijo).
public interface Pasada {

    // Nombre corto usado en la línea de comandos para desactivarla (ej. "cse")
    String nombre();

    // Descripción legible para los reportes (ej. "Subexpresiones comunes")
    String descripcion();

    boolean aplicar(List<Cuadruplo> codigo, ContextoOptimizacion ctx);
}
