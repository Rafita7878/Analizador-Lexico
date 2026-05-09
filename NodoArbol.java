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

    // Agrega un nodo hijo a este nodo
    public void agregarHijo(NodoArbol hijo) {
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