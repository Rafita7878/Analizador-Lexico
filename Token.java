public class Token {
    public String nombre;  // categoría: PALABRA_RESERVADA, ARITMETICO, AGRUPACION, etc.
    public String tipo;    // token específico: INICIO, SUMA, PARENTESIS_ABIERTO, etc.
    public String lexema;  // texto original: inicio, +, (
    public int    linea;   // número de línea en el archivo fuente
    public int    ref;     // número de referencia del token

    // Constructor del token
    public Token(String nombre, String tipo, String lexema, int linea, int ref) {
        this.nombre = nombre;
        this.tipo   = tipo;
        this.lexema = lexema;
        this.linea  = linea;
        this.ref    = ref;
    }

    // Formato para mostrar en consola y en el archivo .tok
    @Override
    public String toString() {
        return String.format("Renglón: %d, Lexema: %-15s Token: %-5d %s",
                             linea, lexema, ref, tipo);
    }
}