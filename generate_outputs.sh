#!/bin/bash

# Caminho para a gramática
GRAMMAR="grammar/Go_Lexer.g"

# Diretório de testes
TEST_DIR="tests"

# Compila a gramática
make GRAMMAR=$GRAMMAR

# Encontra todos os arquivos .go dentro de tests/ e subdiretórios
find "$TEST_DIR" -type f -name "*.go" | while read -r FILE_PATH; do
    REL_PATH=$(realpath --relative-to=grammar "$FILE_PATH")
    DIR_PATH=$(dirname "$FILE_PATH")

    # Caminho completo do output no mesmo diretório do .go
    OUTPUT_FILE="$DIR_PATH/output.txt"

    echo "Executando: $FILE_PATH"

    # Executa o lexer e salva a saída no output.txt ao lado do .go
    make run GRAMMAR=$GRAMMAR FILE="../$REL_PATH" > "$OUTPUT_FILE" 2>&1 --no-print-directory 
done

echo "Todos os testes foram executados. Cada resultado foi salvo no respectivo diretório."
