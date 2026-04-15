import java.util.Map;
import java.util.LinkedHashMap;

public class AnalizadorLexico {
    //palabras reservadas del lenguaje Rafi
    //se utiliza un LinkedHashMap para mantener el orden de inserción de las palabras reservadas
    //la clave es la palabra reservada y el valor es el tipo de token correspondiente
    private static final Map<String, String> RESERVADAS = new LinkedHashMap<>();
    static {
        RESERVADAS.put("inicio", "INICIO");
        RESERVADAS.put("fin", "FIN");
        RESERVADAS.put("si", "SI");
        RESERVADAS.put("entonces", "ENTONCES");
        RESERVADAS.put("sino", "SINO");
        //RESERVADAS.put("mientras", "MIENTRAS");
        RESERVADAS.put("hacer", "HACER");
        RESERVADAS.put("impdig", "IMPDIG");
        RESERVADAS.put("impcad", "IMPCAD");
        RESERVADAS.put("leerdig", "LEERDIG");
        RESERVADAS.put("leercad", "LEERCAD");
    }

    //operadores aritméticos del lenguaje Rafi
    private static final Map<Character, String> ARITMETICOS = new LinkedHashMap<>();
    static{
        ARITMETICOS.put('+', "SUMA");
        ARITMETICOS.put('-', "RESTA");
        ARITMETICOS.put('*', "MULTIPLICACION");
        ARITMETICOS.put('/', "DIVISION");
    }
}
