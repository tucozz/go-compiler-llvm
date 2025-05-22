#!/bin/bash

# Diretório base dos testes
TEST_DIR="tests"

# Flag de erro
HAS_DIFFS=0

# Procura todos os arquivos output.txt
find "$TEST_DIR" -type f -name "output.txt" | while read -r OUTPUT_FILE; do
    DIR_PATH=$(dirname "$OUTPUT_FILE")
    EXPECTED_FILE="$DIR_PATH/test_output.txt"

    if [[ -f "$EXPECTED_FILE" ]]; then
        # Compara os arquivos
        DIFF_OUTPUT=$(diff -u "$EXPECTED_FILE" "$OUTPUT_FILE")
        if [[ $? -ne 0 ]]; then
            echo "ERRO: Diferenças encontradas em: $DIR_PATH"
            echo "$DIFF_OUTPUT"
            HAS_DIFFS=1
        fi
    fi
done

# Saída final
if [[ $HAS_DIFFS -eq 0 ]]; then
    exit 0
else
    exit 1
fi
