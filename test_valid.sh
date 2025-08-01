#!/bin/bash

# Script para testar todos os arquivos válidos
cd "$(dirname "$0")/grammar"

echo "=== Testing Valid Go Programs ==="
echo

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

success_count=0
total_count=0

# Função para testar um arquivo
test_file() {
    local file=$1
    local category=$2
    
    echo -n "Testing $category/$file... "
    total_count=$((total_count + 1))
    
    # Captura tanto stdout quanto stderr
    output=$(make run-tree FILE="../valid_tests/$category/$file" 2>&1)
    exit_code=$?
    
    # Verifica se há erros de parsing (mensagens como "line X:Y ...")
    if [ $exit_code -eq 0 ] && ! echo "$output" | grep -q "^line [0-9]*:[0-9]*"; then
        echo -e "${GREEN}PASS${NC}"
        success_count=$((success_count + 1))
    else
        echo -e "${RED}FAIL${NC}"
        echo "  Error details:"
        echo "$output" | head -3 | sed 's/^/  /'
    fi
}

# Testar todas as categorias
for category in declarations functions statements arrays literals scopes expressions type_checking operators control_flow; do
    echo "=== Testing $category ==="
    if [ -d "../valid_tests/$category" ]; then
        for file in ../valid_tests/$category/*.go; do
            if [ -f "$file" ]; then
                basename_file=$(basename "$file")
                test_file "$basename_file" "$category"
            fi
        done
    else
        echo "Directory not found: $category"
    fi
    echo
done

echo "=== Summary ==="
echo "Passed: $success_count/$total_count tests"

if [ $success_count -eq $total_count ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed.${NC}"
    exit 1
fi
