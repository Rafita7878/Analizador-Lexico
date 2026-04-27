# 🔍 Analizador Léxico — Lenguaje RAFI

Analizador léxico desarrollado en Java para el lenguaje de programación **RAFI**, creado como parte de la práctica de la materia de **Autómatas y Lenguajes Formales**. El analizador es capaz de leer un programa fuente escrito en el lenguaje RAFI, clasificar sus componentes en tokens, generar una tabla de símbolos, detectar errores léxicos y producir los archivos de salida requeridos.

---

## 📁 Estructura del proyecto

```
analizador-lexico/
│
├── Token.java              # Clase que representa un token
├── AnalizadorLexico.java   # Lógica principal del análisis léxico
└── Main.java               # Punto de entrada, orquesta todo el proceso
│
├── progfte.txt                 # Archivo de entrada (programa fuente en RAFI)
├── progfte.dep                 # Archivo generado: código depurado
├── progfte.tab                 # Archivo generado: tabla de símbolos
├── progfte.tok                 # Archivo generado: tabla de símbolos + lista de tokens
└── README.md
```

---

## 🚀 Cómo compilar y ejecutar

### Requisitos
- Java JDK 11 o superior

### Compilar
```bash
javac Token.java AnalizadorLexico.java Main.java
```

### Ejecutar
```bash
java Main
```

> El programa leerá automáticamente el archivo `progfte.txt` que debe estar en la misma carpeta donde se ejecuta el programa.

---

## 📝 El lenguaje RAFI

RAFI es un lenguaje de programación de propósito educativo. Un programa válido en RAFI tiene la siguiente estructura:

```
rafi NombreDelPrograma

decl
  entero a, b, c;
  cadena mensaje;
  booleano bandera;

inicio
  a := 5;
  b := 3;
  impcad("Hola mundo");
  leerdig(a);
  d := a * (b - c);
  si (a + b) entonces
    impdig(a);
  fin
  mientras (b) hacer
    b := b - 1;
  fin
fin
```

### Partes del programa

| Sección | Descripción |
|---|---|
| `rafi NombrePrograma` | Encabezado obligatorio que identifica el inicio del programa y su nombre |
| `decl` | Sección de declaración de variables, debe aparecer antes de `inicio` |
| `inicio ... fin` | Cuerpo del programa donde se escriben las instrucciones |

---

## 🗂️ Descripción de clases

### `Token.java`

Clase modelo que representa la unidad mínima reconocida por el analizador. Cada token captura cuatro datos del código fuente.

```java
public class Token {
    public String nombre;   // Categoría amplia del token
    public String tipo;     // Tipo específico del token
    public String lexema;   // Texto original encontrado en el código fuente
    public int    linea;    // Número de línea donde fue encontrado
}
```

#### Campos explicados

| Campo | Descripción | Ejemplo |
|---|---|---|
| `nombre` | Categoría general a la que pertenece el token | `PALABRA_RESERVADA` |
| `tipo` | Identificador específico del token dentro de su categoría | `INICIO` |
| `lexema` | El texto exacto tal como aparece en el código fuente | `inicio` |
| `linea` | Número de línea en el archivo fuente donde se encontró | `8` |

#### Ejemplo de tokens generados

| nombre | tipo | lexema | linea |
|---|---|---|---|
| `PALABRA_RESERVADA` | `PROG` | `rafi` | 1 |
| `IDENTIFICADOR` | `ID` | `MiPrograma` | 1 |
| `PALABRA_RESERVADA` | `DECL` | `decl` | 3 |
| `PALABRA_RESERVADA` | `TIPO` | `entero` | 4 |
| `IDENTIFICADOR` | `ID` | `a` | 4 |
| `PUNTUACION` | `COMA` | `,` | 4 |
| `PALABRA_RESERVADA` | `INICIO` | `inicio` | 8 |
| `ASIGNACION` | `ASIG` | `:=` | 9 |
| `CONSTANTE_ENTERA` | `CENT` | `5` | 9 |
| `ARITMETICO` | `SUMA` | `+` | 12 |
| `AGRUPACION` | `PARENTESIS_ABIERTO` | `(` | 12 |

---

### `AnalizadorLexico.java`

Es el núcleo del proyecto. Contiene los tres mapas de clasificación y el método `analizar()` que recorre el código fuente carácter a carácter.

#### Mapas de clasificación

##### Palabras reservadas — `RESERVADAS`

```java
private static final Map<String, String> RESERVADAS = new LinkedHashMap<>();
```

Mapa que asocia cada palabra reservada del lenguaje RAFI con su tipo de token. Se usa `LinkedHashMap` para mantener el orden de inserción.

| Lexema | Token | Función en el lenguaje |
|---|---|---|
| `rafi` | `PROG` | Identifica el inicio del programa |
| `decl` | `DECL` | Inicia la sección de declaración de variables |
| `entero` | `TIPO` | Tipo de dato entero |
| `cadena` | `TIPO` | Tipo de dato cadena de texto |
| `booleano` | `TIPO` | Tipo de dato booleano |
| `inicio` | `INICIO` | Inicia el cuerpo del programa |
| `fin` | `FIN` | Cierra el cuerpo del programa o un bloque |
| `si` | `SI` | Condicional if |
| `entonces` | `ENTONCES` | Parte then del condicional |
| `sino` | `SINO` | Parte else del condicional |
| `mientras` | `MIENTRAS` | Inicio del ciclo while |
| `hacer` | `HACER` | Parte do del ciclo while |
| `impdig` | `IMPDIG` | Imprime un valor entero en pantalla |
| `impcad` | `IMPCAD` | Imprime una cadena en pantalla |
| `leerdig` | `LEERDIG` | Lee un entero desde teclado |
| `leercad` | `LEERCAD` | Lee una cadena desde teclado |

##### Operadores aritméticos — `ARITMETICOS`

```java
private static final Map<Character, String> ARITMETICOS = new LinkedHashMap<>();
```

Mapa que asocia cada símbolo aritmético con su tipo de token. Las claves son de tipo `char` porque son símbolos de un solo carácter.

| Símbolo | Token | Operación |
|---|---|---|
| `+` | `SUMA` | Adición |
| `-` | `RESTA` | Sustracción |
| `*` | `MULTIPLICACION` | Multiplicación |
| `/` | `DIVISION` | División |

##### Símbolos de agrupación — `AGRUPACION`

```java
private static final Map<Character, String> AGRUPACION = new LinkedHashMap<>();
```

Mapa para los símbolos que agrupan o delimitan expresiones y bloques.

| Símbolo | Token |
|---|---|
| `(` | `PARENTESIS_ABIERTO` |
| `)` | `PARENTESIS_CERRADO` |
| `{` | `LLAVE_ABIERTA` |
| `}` | `LLAVE_CERRADA` |
| `[` | `CORCHETE_ABIERTO` |
| `]` | `CORCHETE_CERRADO` |

#### Método `analizar(String entrada)`

Recibe el código fuente completo como un `String` y devuelve una `List<Token>` con todos los tokens reconocidos.

El método recorre el texto con un índice `i` que avanza carácter a carácter. En cada iteración evalúa el carácter actual `c` y determina a qué categoría pertenece siguiendo este orden de prioridad:

```
1. Salto de línea  →  incrementa el contador de línea
2. Espacios / tabs →  ignorar, no generar token
3. Comentario /*   →  consumir hasta */ sin generar token
4. Operador :=     →  lookahead de 2 chars → token ASIG
5. Letra           →  acumular identificador o palabra reservada
6. Dígito          →  acumular número entero → token CENT
7. Comilla "       →  acumular cadena literal → token CLIT
8. Operador arit.  →  consultar mapa ARITMETICOS → token correspondiente
9. Agrupación      →  consultar mapa AGRUPACION  → token correspondiente
10. Punto y coma ; →  token PC
11. Coma ,         →  token COMA
12. Otro           →  registrar error léxico con número de línea
```

##### Detalle del reconocimiento de cada categoría

**Comentarios `/* ... */`**

```java
if (c == '/' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '*') {
    i += 2; // saltar /*
    while (i + 1 < entrada.length()) {
        if (entrada.charAt(i) == '*' && entrada.charAt(i + 1) == '/') {
            i += 2; // saltar */
            break;
        }
        i++;
    }
    continue; // no genera token
}
```

Usa un _lookahead_ de un carácter para detectar la secuencia `/*`. Avanza sin generar ningún token hasta encontrar `*/`. Si el comentario contiene saltos de línea, el contador de líneas los registra correctamente.

**Operador de asignación `:=`**

```java
if (c == ':' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '=') {
    tokens.add(new Token("ASIGNACION", "ASIG", ":=", linea));
    i += 2;
    continue;
}
```

Es el único operador de dos caracteres del lenguaje. Se detecta con _lookahead_: si el carácter actual es `:` y el siguiente es `=`, se genera el token `ASIG` y se avanzan dos posiciones.

**Palabras reservadas e identificadores**

```java
if (Character.isLetter(c)) {
    StringBuilder lexema = new StringBuilder();
    while (i < entrada.length() &&
           (Character.isLetterOrDigit(entrada.charAt(i)) || entrada.charAt(i) == '_')) {
        lexema.append(entrada.charAt(i));
        i++;
    }
    String palabra = lexema.toString();
    if (RESERVADAS.containsKey(palabra)) {
        tokens.add(new Token("PALABRA_RESERVADA", RESERVADAS.get(palabra), palabra, lineaInicio));
    } else {
        tokens.add(new Token("IDENTIFICADOR", "ID", palabra, lineaInicio));
    }
}
```

Cuando el carácter actual es una letra, acumula todos los caracteres alfanuméricos y guiones bajos en un `StringBuilder`. Al terminar, busca el lexema en el mapa `RESERVADAS`. Si lo encuentra, genera un token `PALABRA_RESERVADA`; si no, genera un token `IDENTIFICADOR`. El lenguaje RAFI distingue mayúsculas de minúsculas, por lo que `inicio` es una palabra reservada pero `Inicio` sería un identificador.

**Números enteros**

```java
if (Character.isDigit(c)) {
    StringBuilder lexema = new StringBuilder();
    while (i < entrada.length() && Character.isDigit(entrada.charAt(i))) {
        lexema.append(entrada.charAt(i));
        i++;
    }
    tokens.add(new Token("CONSTANTE_ENTERA", "CENT", lexema.toString(), lineaInicio));
}
```

Acumula dígitos consecutivos y genera un token `CENT` (constante entera). Solo reconoce enteros no negativos; los números negativos se representan con el operador `-` seguido de un número.

**Cadenas de texto**

```java
if (c == '"') {
    i++; // saltar comilla de apertura
    while (i < entrada.length() && entrada.charAt(i) != '"') {
        lexema.append(entrada.charAt(i));
        i++;
    }
    i++; // saltar comilla de cierre
    tokens.add(new Token("CADENA_LITERAL", "CLIT", "\"" + lexema + "\"", lineaInicio));
}
```

Al encontrar una comilla de apertura `"`, acumula todo el contenido hasta la siguiente comilla de cierre y genera un token `CLIT`.

#### Método `getErrores()`

```java
public List<String> getErrores() {
    return errores;
}
```

Devuelve la lista de errores léxicos encontrados durante el análisis. Cada error es un `String` con el formato:

```
ERROR LÉXICO en línea 7: carácter no reconocido '@'
```

---

### `Main.java`

Punto de entrada del programa. Orquesta todas las etapas: lectura, depuración, análisis, construcción de la tabla de símbolos y escritura de los archivos de salida.

#### Flujo de ejecución

```
1. Leer progfte.txt
       ↓
2. depurar()  →  escribir progfte.dep
       ↓
3. analizar() →  obtener List<Token>
       ↓
4. construirTablaSimbolos()  →  obtener Map de variables
       ↓
5. generarContenidoTabla()   →  escribir progfte.tab
       ↓
6. generarArchivoTok()       →  escribir progfte.tok
       ↓
7. Mostrar resumen en consola
```

#### Método `depurar(String entrada)`

Genera el archivo `progfte.dep` eliminando todo lo que no es código útil:

| Elemento eliminado | Comportamiento |
|---|---|
| Comentarios `/* ... */` | Se consumen completamente sin dejar rastro |
| Espacios múltiples consecutivos | Se reducen a un solo espacio |
| Tabuladores `\t` | Se reemplazan por un espacio simple |
| Retornos de carro `\r` | Se eliminan |
| Líneas vacías | Se eliminan, no se genera salto de línea |

El código depurado conserva la estructura línea por línea del programa original pero sin ningún elemento innecesario.

**Entrada (`progfte.txt`):**
```
rafi MiPrograma /* Encabezado */

decl
  entero   a,   b;   /* variables */

inicio
  a := 5;
fin
```

**Salida (`progfte.dep`):**
```
rafi MiPrograma
decl
entero a, b;
inicio
a := 5;
fin
```

#### Método `construirTablaSimbolos(List<Token> tokens)`

Recorre la lista de tokens buscando el patrón de declaración de variables:

```
DECL → TIPO → ID, ID, ID ; → TIPO → ID ; → ... → INICIO
```

Solo registra variables que aparecen dentro de la sección `decl`, es decir, entre los tokens `DECL` e `INICIO`. Para cada variable detectada guarda tres datos:

| Dato | Descripción |
|---|---|
| Tipo | `entero`, `cadena` o `booleano` |
| Valor inicial | Según especificación: `0`, `""` o `false` |
| Línea | Línea del archivo fuente donde fue declarada |

Si una variable aparece declarada más de una vez, el método imprime una advertencia en consola pero no detiene el análisis.

Los valores iniciales asignados automáticamente son:

| Tipo | Valor inicial |
|---|---|
| `entero` | `0` |
| `cadena` | `""` |
| `booleano` | `false` |

#### Método `generarArchivoTok(...)`

Genera el archivo `progfte.tok` dividido en cuatro secciones:

| Sección | Contenido |
|---|---|
| Sección 1 | Tabla de símbolos: variables declaradas con tipo, valor inicial y línea |
| Sección 2 | Lista completa de tokens con línea, nombre, tipo y lexema |
| Sección 3 | Resumen con conteo de tokens por categoría, totales de tokens, variables y errores |
| Sección 4 | Lista de errores léxicos, o el mensaje `Sin errores léxicos` si el programa es correcto |

---

## 📄 Archivos generados

### `progfte.dep` — Código depurado

Versión limpia del programa fuente sin comentarios, espacios múltiples ni líneas vacías. Útil para las siguientes fases del compilador.

### `progfte.tab` — Tabla de símbolos

```
No.   | VARIABLE             | TIPO         | VALOR INIT | LÍNEA
-----------------------------------------------------------------
1     | a                    | entero       | 0          | 4
2     | b                    | entero       | 0          | 4
3     | mensaje              | cadena       | ""         | 5
4     | bandera              | booleano     | false      | 6
```

### `progfte.tok` — Tabla de símbolos y lista de tokens

```
=================================================================
  ANALIZADOR LÉXICO — LENGUAJE RAFI
  Archivo: progfte.tok
=================================================================

=================================================================
  SECCIÓN 1 — TABLA DE SÍMBOLOS
=================================================================
No.   | VARIABLE             | TIPO         | VALOR INIT | LÍNEA
-----------------------------------------------------------------
1     | a                    | entero       | 0          | 4

=================================================================
  SECCIÓN 2 — LISTA DE TOKENS
=================================================================
LÍNEA  | NOMBRE                | TIPO                   | LEXEMA
-----------------------------------------------------------------
1      | PALABRA_RESERVADA     | PROG                   | rafi
1      | IDENTIFICADOR         | ID                     | MiPrograma
...

=================================================================
  SECCIÓN 3 — RESUMEN
=================================================================
  PALABRA_RESERVADA         : 8 token(s)
  IDENTIFICADOR             : 5 token(s)
  ARITMETICO                : 3 token(s)
  AGRUPACION                : 4 token(s)
-----------------------------------------------------------------
  TOTAL DE TOKENS           : 20
  TOTAL DE VARIABLES        : 2
  TOTAL DE ERRORES          : 0

=================================================================
  SECCIÓN 4 — ERRORES LÉXICOS
=================================================================
  Sin errores léxicos.
=================================================================
```

---

## ⚠️ Manejo de errores léxicos

Cuando el analizador encuentra un carácter no reconocido, genera un error con el número de línea exacto:

```
ERROR LÉXICO en línea 7: carácter no reconocido '@'
```

El analizador **no se detiene** al encontrar un error; continúa procesando el resto del archivo para reportar todos los errores en una sola ejecución.

---

## 🔤 Expresiones regulares utilizadas

| Token | Expresión regular | Descripción |
|---|---|---|
| Identificador | `letra (letra \| digito \| _)*` | Inicia con letra, seguida de letras, dígitos o guión bajo |
| Constante entera | `digito+` | Una o más cifras decimales |
| Cadena literal | `" [^"]* "` | Cualquier texto entre comillas dobles |
| Operador `:=` | `: =` | Dos caracteres exactos en secuencia |
| Comentario | `/ * [^*]* * /` | Todo entre `/*` y `*/` |

---

## 👨‍💻 Autor
Rafael Cervantes de la Cruz
Alexis Alejandro Solis Santos

Desarrollado como práctica de la materia **Autómatas y Lenguajes Formales**.
