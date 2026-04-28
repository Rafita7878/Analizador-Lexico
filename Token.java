/*
    *Cada vez que el analizadorLexico reconoce una palabra reservada, identificador o
    *cualquier simbolo valido en el codigo fuente, se crea un token que almacena 
    *la informacion relevante de esa unidad.
 */
public class Token {
    // categoria general a la que pertenece el token.
    //
    public String nombre;  // categoría: PALABRA_RESERVADA, ARITMETICO, AGRUPACION, etc.
    // identificador especifico dentro de esa categoría.
    public String tipo;    // token específico: INICIO, SUMA, PARENTESIS_ABIERTO, etc.
    //texto exacto tal como aparece en el codigo del programa. Es el valor real que escribio el programador.
    public String lexema;  // texto original: inicio, +, (
    //Numero de linea del archivo fuente donde fue encontrado ese token.
    public int    linea;   // número de línea en el archivo fuente
    //numero de referencia numerica asignada a cada token para facilitar su manejo en el analizador sintactico.
    public int    ref;     // número de referencia del token

    // Constructor del token
    /**
     * Constructor del token.
     * Inicializa todos los campos con la información capturada por el AnalizadorLexico
     * en el momento en que reconoce una unidad válida del código fuente.
     *
     * @param nombre  Categoría general del token (ej. "PALABRA_RESERVADA")
     * @param tipo    Tipo específico del token   (ej. "INICIO")
     * @param lexema  Texto original del código   (ej. "inicio")
     * @param linea   Línea donde fue encontrado  (ej. 8)
     * @param ref     Número de referencia        (ej. 103)
     */
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