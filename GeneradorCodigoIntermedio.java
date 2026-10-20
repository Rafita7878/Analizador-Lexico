import java.util.*;

// GeneradorCodigoIntermedio.java
// Recorre el AST (el mismo que validó AnalizadorSemantico) y genera el
// código de tres direcciones del programa RAFI, guardado como una lista de
// cuádruplos. Solo se llama si no hay errores sintácticos ni semánticos.
//
// Sigue el mismo recorrido que Ejecutor.java, pero en lugar de CALCULAR
// valores, EMITE instrucciones:
//
//   Expresión        →  cada operador produce  t = x op y   (un temporal nuevo)
//   Menos unario     →  t = menos x
//   Asignación       →  x = t
//   si ... sino ...  →  ifFalse cond goto Lsino ... goto Lfin ... Lsino: ... Lfin:
//   mientras         →  Linicio: ifFalse cond goto Lfin ... goto Linicio  Lfin:
//   impdig/impcad    →  param x ; call impdig, 1
//   leerdig/leercad  →  param x ; call leerdig, 1
//
// Formas de instrucción usadas (lista del profesor, Aho pág. 364):
//   1. x = y op z      2. x = op y      3. x = y      4. goto L
//   5. ifFalse x goto L                 7. param / call
public class GeneradorCodigoIntermedio {

    private List<Cuadruplo> cuadruplos = new ArrayList<>();
    private int contadorTemporales = 1;
    private int contadorEtiquetas  = 1;

    // ── Punto de entrada ──────────────────────────────────────────────────────
    public List<Cuadruplo> generar(NodoArbol raiz) {
        cuadruplos.clear();
        contadorTemporales = 1;
        contadorEtiquetas  = 1;
        generarSentencias(buscarHijoTipo(raiz, "SENTENCIAS"));
        return cuadruplos;
    }

    public List<Cuadruplo> getCuadruplos() {
        return cuadruplos;
    }

    // ── Utilidades ────────────────────────────────────────────────────────────
    private String nuevoTemporal() { return "t" + (contadorTemporales++); }
    private String nuevaEtiqueta() { return "L" + (contadorEtiquetas++); }

    private void emitir(String op, String arg1, String arg2, String resultado) {
        cuadruplos.add(new Cuadruplo(op, arg1, arg2, resultado));
    }

    private void emitirEtiqueta(String etiqueta) {
        emitir("etiq", "", "", etiqueta);
    }

    private NodoArbol buscarHijoTipo(NodoArbol nodo, String tipo) {
        if (nodo == null) return null;
        for (NodoArbol h : nodo.hijos) {
            if (h.tipo.equals(tipo)) return h;
        }
        return null;
    }

    // ── Sentencias ────────────────────────────────────────────────────────────
    private void generarSentencias(NodoArbol contenedor) {
        if (contenedor == null) return;
        for (NodoArbol s : contenedor.hijos) generarSentencia(s);
    }

    private void generarSentencia(NodoArbol s) {
        switch (s.tipo) {
            case "ASIGNACION":  generarAsignacion(s);  break;
            case "CONDICIONAL": generarCondicional(s); break;
            case "CICLO":       generarCiclo(s);       break;
            case "IMPRESION":   generarImpresion(s);   break;
            case "LECTURA":     generarLectura(s);     break;
            default: break;
        }
    }

    // x := expr   →   (código de expr) ; x = direccion
    private void generarAsignacion(NodoArbol s) {
        NodoArbol idNodo   = buscarHijoTipo(s, "ID");
        NodoArbol exprNodo = buscarHijoTipo(s, "EXPR_LOGICA");
        if (idNodo == null || exprNodo == null) return;

        String direccion = generarExpresion(exprNodo);
        emitir("=", direccion, "", idNodo.valor);
    }

    // si (cond) entonces S1 [sino S2] fin
    //
    //   Sin sino:                Con sino:
    //     t = cond                 t = cond
    //     ifFalse t goto L1        ifFalse t goto L1
    //     S1                       S1
    //   L1:                        goto L2
    //                            L1:
    //                              S2
    //                            L2:
    private void generarCondicional(NodoArbol s) {
        NodoArbol cond     = buscarHijoTipo(s, "EXPR_LOGICA");
        NodoArbol entonces = buscarHijoTipo(s, "SENTENCIAS_ENTONCES");
        NodoArbol sino     = buscarHijoTipo(s, "SENTENCIAS_SINO");

        String direccionCond = generarExpresion(cond);
        String etiqSino = nuevaEtiqueta();

        emitir("ifFalse", direccionCond, "", etiqSino);
        generarSentencias(entonces);

        if (sino != null) {
            String etiqFin = nuevaEtiqueta();
            emitir("goto", "", "", etiqFin);
            emitirEtiqueta(etiqSino);
            generarSentencias(sino);
            emitirEtiqueta(etiqFin);
        } else {
            emitirEtiqueta(etiqSino);
        }
    }

    // mientras (cond) hacer S fin
    //
    //   L1:
    //     t = cond
    //     ifFalse t goto L2
    //     S
    //     goto L1
    //   L2:
    private void generarCiclo(NodoArbol s) {
        NodoArbol cond   = buscarHijoTipo(s, "EXPR_LOGICA");
        NodoArbol cuerpo = buscarHijoTipo(s, "SENTENCIAS_CICLO");

        String etiqInicio = nuevaEtiqueta();
        String etiqFin    = nuevaEtiqueta();

        emitirEtiqueta(etiqInicio);
        String direccionCond = generarExpresion(cond);
        emitir("ifFalse", direccionCond, "", etiqFin);
        generarSentencias(cuerpo);
        emitir("goto", "", "", etiqInicio);
        emitirEtiqueta(etiqFin);
    }

    // impdig(expr) / impcad(expr)  →  param dir ; call impdig, 1
    private void generarImpresion(NodoArbol s) {
        if (s.hijos.isEmpty()) return;
        String instruccion = s.hijos.get(0).valor; // "impdig" o "impcad"
        NodoArbol exprNodo = buscarHijoTipo(s, "EXPR_LOGICA");
        if (exprNodo == null) return;

        String direccion = generarExpresion(exprNodo);
        emitir("param", direccion, "", "");
        emitir("call", instruccion, "1", "");
    }

    // leerdig(x) / leercad(x)  →  param x ; call leerdig, 1
    private void generarLectura(NodoArbol s) {
        if (s.hijos.isEmpty()) return;
        String instruccion = s.hijos.get(0).valor; // "leerdig" o "leercad"
        NodoArbol idNodo   = buscarHijoTipo(s, "ID");
        if (idNodo == null) return;

        emitir("param", idNodo.valor, "", "");
        emitir("call", instruccion, "1", "");
    }

    // ── Expresiones ───────────────────────────────────────────────────────────
    // Genera el código de una expresión y devuelve su DIRECCIÓN:
    // un nombre (a), una constante (5) o un temporal (t3).
    //
    // Los nodos envoltorio de la gramática (EXPR_LOGICA → EXPR_RELACIONAL →
    // EXPRESION → TERMINO) que tienen un solo hijo NO generan temporales:
    // simplemente pasan la dirección de su hijo hacia arriba.
    private String generarExpresion(NodoArbol n) {
        if (n == null) return "";

        switch (n.tipo) {

            // Hojas: la dirección es el propio nombre o constante
            case "ID":
            case "CENT":
            case "CLIT":
                return n.valor;

            case "VERDADERO": return "verdadero";
            case "FALSO":     return "falso";

            // Operadores unarios:  t = menos x   /   t = no x
            case "MENOS":
            case "NO": {
                String operando = generarExpresion(n.hijos.get(0));
                String t = nuevoTemporal();
                emitir(n.tipo.equals("MENOS") ? "menos" : "no", operando, "", t);
                return t;
            }

            // a y b o c  →  t1 = a y b ; t2 = t1 o c   (asociatividad izquierda)
            case "EXPR_LOGICA": {
                if (n.hijos.size() == 1) return generarExpresion(n.hijos.get(0));
                String izq = generarExpresion(n.hijos.get(0));
                for (int i = 1; i < n.hijos.size(); i += 2) {
                    String op  = n.hijos.get(i).valor;           // "y" u "o"
                    String der = generarExpresion(n.hijos.get(i + 1));
                    String t   = nuevoTemporal();
                    emitir(op, izq, der, t);
                    izq = t;
                }
                return izq;
            }

            // a < b  →  t = a < b
            case "EXPR_RELACIONAL": {
                if (n.hijos.size() == 1) return generarExpresion(n.hijos.get(0));
                String izq = generarExpresion(n.hijos.get(0));
                String der = generarExpresion(n.hijos.get(2));
                String op  = n.hijos.get(1).valor;
                // En RAFI la igualdad es '=', pero '=' ya es la copia en el cuádruplo,
                // así que la comparación se escribe '==' para no confundirlas.
                if (op.equals("=")) op = "==";
                String t = nuevoTemporal();
                emitir(op, izq, der, t);
                return t;
            }

            // a + b - c  /  a * b / c  →  un temporal por cada operador, de izquierda a derecha
            case "EXPRESION":
            case "TERMINO": {
                List<NodoArbol> operandos  = new ArrayList<>();
                List<String>    operadores = new ArrayList<>();
                for (NodoArbol h : n.hijos) {
                    if (h.tipo.equals("OPERADOR")) operadores.add(h.valor);
                    else operandos.add(h);
                }
                if (operandos.isEmpty()) return "";

                String izq = generarExpresion(operandos.get(0));
                for (int i = 0; i < operadores.size(); i++) {
                    String der = generarExpresion(operandos.get(i + 1));
                    String t   = nuevoTemporal();
                    emitir(operadores.get(i), izq, der, t);
                    izq = t;
                }
                return izq;
            }

            default:
                if (!n.hijos.isEmpty()) return generarExpresion(n.hijos.get(0));
                return "";
        }
    }

    // ── Salida: código de tres direcciones + tabla de cuádruplos ──────────────
    public String aTexto() {
        StringBuilder sb = new StringBuilder();
        String separador = "=".repeat(65) + "\n";
        String divisor   = "-".repeat(65) + "\n";

        sb.append(separador);
        sb.append("  GENERACION DE CODIGO INTERMEDIO — LENGUAJE RAFI\n");
        sb.append("  Archivo: progfte.ci\n");
        sb.append(separador).append("\n");

        // ── Código de tres direcciones ──
        sb.append(separador);
        sb.append("  CODIGO DE TRES DIRECCIONES\n");
        sb.append(separador);
        for (Cuadruplo c : cuadruplos) {
            sb.append(c.aTresDirecciones()).append("\n");
        }
        sb.append("\n");

        // ── Tabla de cuádruplos ──
        sb.append(separador);
        sb.append("  CUADRUPLOS\n");
        sb.append(separador);
        sb.append(String.format(" %-4s | %-9s | %-12s | %-12s | %s%n",
                "No.", "OP", "ARG1", "ARG2", "RESULTADO"));
        sb.append(divisor);
        for (int i = 0; i < cuadruplos.size(); i++) {
            Cuadruplo c = cuadruplos.get(i);
            sb.append(String.format(" %-4d | %-9s | %-12s | %-12s | %s%n",
                    i, c.op, c.arg1, c.arg2, c.resultado));
        }
        sb.append(divisor);
        sb.append("  Total de cuadruplos: ").append(cuadruplos.size()).append("\n");
        sb.append("  Temporales usados  : ").append(contadorTemporales - 1).append("\n");
        sb.append("  Etiquetas usadas   : ").append(contadorEtiquetas - 1).append("\n");

        return sb.toString();
    }
}