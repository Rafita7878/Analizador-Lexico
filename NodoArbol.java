// NodoArbol.java
// Representa un nodo del árbol de análisis sintáctico.
// Cada nodo tiene un tipo (qué es: PROGRAMA, DECLARACION, ASIGNACION, etc.),
// un valor (el lexema si es hoja), y una lista de hijos que forman la estructura del árbol.

import java.util.ArrayList;
import java.util.List;

public class NodoArbol {

    // Tipo del nodo — describe qué representa en la gramática
    // Ejemplos: "PROGRAMA", "DECLARACION", "ASIGNACION", "EXPRESION", "TERMINO"
    public String tipo;

    // Valor del nodo — solo se usa en los nodos hoja (tokens reales)
    // Ejemplo: "inicio", "a", ":=", "5"
    public String valor;

    // Hijos del nodo — subárboles que cuelgan de este nodo
    public List<NodoArbol> hijos;

    // Ubicación del nodo en el archivo fuente (línea y columna).
    // En nodos hoja viene del Token de origen; en nodos intermedios
    // se hereda automáticamente del primer hijo (ver agregarHijo).
    public int linea = -1;
    public int columna = -1;

    // Constructor para nodos intermedios (tienen hijos, no tienen valor)
    public NodoArbol(String tipo) {
        this.tipo   = tipo;
        this.valor  = "";
        this.hijos  = new ArrayList<>();
    }

    // Constructor para nodos hoja (tienen valor, no tienen hijos)
    public NodoArbol(String tipo, String valor) {
        this.tipo   = tipo;
        this.valor  = valor;
        this.hijos  = new ArrayList<>();
    }

    // Constructor para nodos hoja creados directamente desde un Token —
    // captura línea y columna automáticamente desde el token de origen.
    public NodoArbol(String tipo, Token tok) {
        this.tipo    = tipo;
        this.valor   = tok.lexema;
        this.hijos   = new ArrayList<>();
        this.linea   = tok.linea;
        this.columna = tok.columna;
    }

    // Agrega un nodo hijo a este nodo.
    // Si este nodo todavía no tiene ubicación propia (nodo intermedio creado
    // con new NodoArbol("TIPO")), hereda línea/columna del primer hijo que
    // se le agregue — así todos los nodos del árbol quedan ubicables sin
    // tener que asignarlo a mano en cada método del parser.
    public void agregarHijo(NodoArbol hijo) {
        if (this.linea == -1 && hijo != null && hijo.linea != -1) {
            this.linea   = hijo.linea;
            this.columna = hijo.columna;
        }
        hijos.add(hijo);
    }

    // Imprime el árbol en consola con indentación para visualizarlo
    // El parámetro sangria aumenta con cada nivel del árbol
    public void imprimir(String sangria) {
        if (!valor.isEmpty()) {
            System.out.println(sangria + "[" + tipo + ": " + valor + "]");
        } else {
            System.out.println(sangria + "<" + tipo + ">");
        }
        for (NodoArbol hijo : hijos) {
            hijo.imprimir(sangria + "  ");
        }
    }

    // Genera el árbol como String para guardarlo en archivo
    public String aTexto(String sangria) {
        StringBuilder sb = new StringBuilder();
        if (!valor.isEmpty()) {
            sb.append(sangria).append("[").append(tipo).append(": ").append(valor).append("]\n");
        } else {
            sb.append(sangria).append("<").append(tipo).append(">\n");
        }
        for (NodoArbol hijo : hijos) {
            sb.append(hijo.aTexto(sangria + "  "));
        }
        return sb.toString();
    }
}