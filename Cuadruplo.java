// Cuadruplo.java
// Representa UNA instrucción de código de tres direcciones en forma de cuádruplo:
//
//      (op, arg1, arg2, resultado)
//
// Ejemplos:
//      t1 = b * t0          →  ( *       , b  , t0 , t1 )
//      t0 = menos c         →  ( menos   , c  ,    , t0 )
//      a  = t4              →  ( =       , t4 ,    , a  )
//      ifFalse t1 goto L1   →  ( ifFalse , t1 ,    , L1 )
//      goto L2              →  ( goto    ,    ,    , L2 )
//      L1:                  →  ( etiq    ,    ,    , L1 )
//
// Los campos que no se usan se guardan como cadena vacía "".
public class Cuadruplo {

    public String op;
    public String arg1;
    public String arg2;
    public String resultado;

    public Cuadruplo(String op, String arg1, String arg2, String resultado) {
        this.op        = op;
        this.arg1      = arg1;
        this.arg2      = arg2;
        this.resultado = resultado;
    }

    // Convierte el cuádruplo a su forma de código de tres direcciones (texto legible)
    public String aTresDirecciones() {
        switch (op) {
            case "etiq":    return resultado + ":";
            case "goto":    return "    goto " + resultado;
            case "ifFalse": return "    ifFalse " + arg1 + " goto " + resultado;
            case "=":       return "    " + resultado + " = " + arg1;
            case "menos":
            case "no":      return "    " + resultado + " = " + op + " " + arg1;
            case "param":   return "    param " + arg1;
            case "call":    return "    call " + arg1 + ", " + arg2;
            default:        // operación binaria: + - * / < > == <= >= <> y o
                return "    " + resultado + " = " + arg1 + " " + op + " " + arg2;
        }
    }
}