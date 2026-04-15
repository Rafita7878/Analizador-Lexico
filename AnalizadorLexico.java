import java.util.Map;

public class AnalizadorLexico {
    //palabras reservadas del lenguaje Rafi
    private static final Map<String, String> RESERVADAS = new LinkedHasMap<>();
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

}
