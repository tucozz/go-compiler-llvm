#!/bin/bash

# Script para testar todos os arquivos inválidos (que devem gerar erros)
cd "$(dirname "$0")/grammar"

echo "=== Testing Invalid Go Programs (should FAIL) ==="
echo

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

success_count=0
total_count=0

# Função para testar um arquivo inválido
test_invalid_file() {
    local file=$1
    local category=$2
    
    echo -n "Testing $category/$file... "
    total_count=$((total_count + 1))
    
    # Captura tanto stdout quanto stderr
    output=$(make -f ParserMakefile run-tree FILE="../invalid_tests/$category/$file" 2>&1)
    exit_code=$?
    
    # Para arquivos inválidos, esperamos ERRO (exit_code != 0 OU mensagens de erro)
    if [ $exit_code -ne 0 ] || echo "$output" | grep -q "^line [0-9]*:[0-9]*"; then
        echo -e "${GREEN}FAIL (as expected)${NC}"
        success_count=$((success_count + 1))
    else
        echo -e "${RED}PASS (unexpected!)${NC}"
        echo "  This file should have failed but didn't:"
        echo "$output" | head -2 | sed 's/^/  /'
    fi
}

# Testar todas as categorias de arquivos inválidos
for category in syntax_errors declarations functions statements expressions arrays keywords; do
    echo "=== Testing $category ==="
    if [ -d "../invalid_tests/$category" ]; then
        for file in ../invalid_tests/$category/*.go; do
            if [ -f "$file" ]; then
                basename_file=$(basename "$file")
                test_invalid_file "$basename_file" "$category"
            fi
        done
    else
        echo "Directory not found: $category"
    fi
    echo
done

echo "=== Summary ==="
echo "Failed as expected: $success_count/$total_count tests"

if [ $success_count -eq $total_count ]; then
    echo -e "${GREEN}All invalid tests failed correctly!${NC}"
    exit 0
else
    failed_count=$((total_count - success_count))
    echo -e "${YELLOW}Warning: $failed_count tests passed when they should have failed.${NC}"
    exit 1
fi
