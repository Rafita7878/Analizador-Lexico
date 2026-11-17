#!/usr/bin/env bash
# Compila el compilador RAFI + el arnés de pruebas en out/pruebas y ejecuta
# todas las pruebas de pruebas/*/progfte.txt.
#
#   pruebas/ejecutar.sh              todas las pruebas, resumen
#   pruebas/ejecutar.sh -v           con salidas lado a lado, bitácora y código
#   pruebas/ejecutar.sh ciclo -v     solo la prueba "ciclo"
#   pruebas/ejecutar.sh -copias      con la pasada "copias" desactivada
set -e
cd "$(dirname "$0")/.."
mkdir -p out/pruebas
javac -encoding UTF-8 -d out/pruebas *.java pruebas/Pruebas.java
java -cp out/pruebas Pruebas "$@"
