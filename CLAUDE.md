# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto
Compilador en Java del lenguaje **RAFI** (Lenguajes y Autómatas 2). Código, comentarios y salidas están en español; mantener ese idioma.

## Compilar y ejecutar
No hay Maven/Gradle; todas las clases están en el paquete por defecto en la raíz (por eso las clases nuevas también van en la raíz: un paquete con nombre no puede importar `Cuadruplo`). Requiere JDK 11+ (proyecto IntelliJ configurado con JDK 21).

```bash
javac *.java
java Main
```

`Main` siempre lee `progfte.txt` del directorio actual (ruta fija). Para probar otro programa, edita `progfte.txt`. Si el programa usa `leerdig`/`leercad`, la ejecución pide datos por teclado (stdin).
Los `.class` y las salidas `progfte.{dep,tab,tok,ast,sem,ci,opt}` están en `.gitignore`. Argumentos de `Main`: `-<pasada>` desactiva una pasada de optimización, `-todas` las desactiva todas. Si existe `progfte.in` (un valor por línea), se usa como entrada para verificar la optimización.

## Pruebas
    pruebas/ejecutar.sh              # todas las pruebas
    pruebas/ejecutar.sh ciclo -v     # una prueba, con salidas lado a lado y bitácora

Cada subcarpeta de `pruebas/` tiene `progfte.txt` y `progfte.in`. `pruebas/Pruebas.java` compara `Ejecutor` vs `InterpreteCuadruplos` (código original) y original vs optimizado; termina con código 1 si hay diferencias. Compila en `out/pruebas` (ignorado por git).

## Pipeline (orquestado en `Main.main`)
1. `depurar()` → `progfte.dep` (sin comentarios `/* */` ni espacios extra)
2. `AnalizadorLexico.analizar()` → `List<Token>`; `Main.construirTablaSimbolos()` → `progfte.tab`; tokens + errores léxicos → `progfte.tok`
3. `AnalizadorSintactico` (descendente recursivo) → AST de `NodoArbol` → `progfte.ast`
4. `AnalizadorSemantico` (solo si no hay errores sintácticos) recorre el AST y construye **su propia** tabla de símbolos → `progfte.sem`
5. `GeneradorCodigoIntermedio` (solo sin errores sintácticos ni semánticos) → `progfte.ci`
6. `Ejecutor` interpreta el AST con valores reales; errores de tiempo de ejecución (p. ej. división entre 0) se lanzan como `ErrorEjecucion`

`AnalizadorSemantico`, `GeneradorCodigoIntermedio` y `Ejecutor` recorren el mismo AST usando los mismos nombres de nodo como strings (`SENTENCIAS`, `ASIGNACION`, `CONDICIONAL`, `CICLO`, `IMPRESION`, `LECTURA`, `EXPR_LOGICA`, `EXPR_RELACIONAL`, `EXPRESION`, `TERMINO`, `OPERADOR`, `ID`, `CENT`, `CLIT`, `MENOS`, `NO`, `SENTENCIAS_ENTONCES/SINO/CICLO`, …). Si cambias la forma del árbol en `AnalizadorSintactico`, actualiza los tres recorridos.

## Código intermedio
- **`Cuadruplo`** (`Cuadruplo.java`): campos públicos `String op, arg1, arg2, resultado`; los no usados son `""`. `aTresDirecciones()` lo convierte a texto de tres direcciones.
- **Generación**: `GeneradorCodigoIntermedio.generar(NodoArbol raiz)` llena `List<Cuadruplo> cuadruplos` mediante `emitir(...)`. `generarExpresion()` devuelve la "dirección" (nombre, constante o temporal) y crea un temporal `tN` por operador; los nodos envoltorio con un solo hijo no generan temporal. El `=` relacional de RAFI se emite como `==` para no confundirlo con la copia.
- **Etiquetas y saltos** (contadores `tN`/`LN` empiezan en 1, vía `nuevoTemporal()`/`nuevaEtiqueta()`):
  - Etiqueta: `("etiq", "", "", "L1")` → `L1:`
  - Salto incondicional: `("goto", "", "", "L2")`
  - Salto condicional: solo existe `ifFalse`: `("ifFalse", t3, "", "L1")` → `ifFalse t3 goto L1` (no hay `if ... goto`)
  - `si`: `ifFalse cond goto Lsino … [goto Lfin; Lsino: …] Lfin:`
  - `mientras`: `Linicio: cond; ifFalse cond goto Lfin … goto Linicio; Lfin:`
  - E/S: `("param", x, "", "")` + `("call", "impdig"|"impcad"|"leerdig"|"leercad", "1", "")`
- **No hay triplos implementados** todavía, solo cuádruplos y código de tres direcciones.

## Salida
No hay GUI: todo es consola + archivos. `GeneradorCodigoIntermedio.aTexto()` arma el texto (sección de tres direcciones, tabla de cuádruplos numerada desde 0, totales de cuádruplos/temporales/etiquetas); `Main` lo imprime en consola y lo escribe en `progfte.ci`.
