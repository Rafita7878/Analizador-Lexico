public class Token {
    public String nombre;  // palabraReservada, aritmetico o agrupacion
    public String tipo;    //INICIO , PARENTESIS
    public String lexema; //inicio, +, (
    public int linea;

    public Token(String nombre, String tipo, String lexema, int linea) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
    }

    @Override
    public String toString() {
        return "Token{" +
                "nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", lexema='" + lexema + '\'' +
                ", linea=" + linea +
                '}';
    }
}