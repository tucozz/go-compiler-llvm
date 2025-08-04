#!/bin/bash

# Script para testar análise semântica em todos os arquivos invalid_tests
# Autor: Gabriel
# Data: $(date)
#
# Uso:
#   ./test_semantic_invalid.sh                 # Testa todas as categorias
#   ./test_semantic_invalid.sh functions       # Testa apenas categoria 'functions'
#   make test_semantic_invalid_batch           # Via Makefile (todas as categorias)
#
# Categorias disponíveis:
#   arrays, declarations, expressions, functions, keywords, statements, syntax_errors

echo "==================================================================================="
echo "                SCRIPT DE TESTE DA ANÁLISE SEMÂNTICA - CASOS INVÁLIDOS"
echo "==================================================================================="
echo ""

# Contadores
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
NO_ERRORS=0

# Arrays para armazenar resultados
FAILED_FILES=()
NO_ERROR_FILES=()

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para compilar o projeto
compile_project() {
    echo -e "${BLUE}Compilando projeto...${NC}"
    make clean > /dev/null 2>&1
    make > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Erro na compilação do projeto!${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Projeto compilado com sucesso${NC}"
    echo ""
}

# Função para testar um único arquivo
test_file() {
    local file=$1
    local category=$(dirname "$file" | sed 's/invalid_tests\///')
    local filename=$(basename "$file")
    
    echo -n "Testando $category/$filename... "
    
    # Executar o compilador e capturar output (usando versão rápida)
    output=$(timeout 10s make run_compiler_fast FILE="$file" 2>&1)
    exit_code=$?
    
    # Verificar se houve timeout
    if [ $exit_code -eq 124 ]; then
        echo -e "${RED}TIMEOUT${NC}"
        FAILED_FILES+=("$file (timeout)")
        return 1
    fi
    
    # Para arquivos inválidos, esperamos erros sintáticos OU semânticos
    if echo "$output" | grep -q "❌ Erros sintáticos encontrados!"; then
        echo -e "${GREEN}PASSOU${NC} (erro sintático detectado como esperado)"
        return 0
    elif echo "$output" | grep -q "SEMANTIC ERROR"; then
        echo -e "${GREEN}PASSOU${NC} (erro semântico detectado como esperado)"
        return 0
    elif echo "$output" | grep -q "✅ Análise concluída!" && ! echo "$output" | grep -q "❌ Erros sintáticos encontrados!"; then
        echo -e "${RED}FALHA${NC} (deveria ter erro sintático ou semântico!)"
        NO_ERROR_FILES+=("$file")
        return 2
    elif [ $exit_code -ne 0 ]; then
        # Se houve erro de compilação/parsing, também consideramos correto
        echo -e "${GREEN}PASSOU${NC} (erro de compilação detectado)"
        return 0
    else
        echo -e "${RED}FALHA${NC} (análise não detectou problemas)"
        NO_ERROR_FILES+=("$file")
        return 2
    fi
}

# Função para testar uma categoria
test_category() {
    local category=$1
    echo -e "${BLUE}=== Testando categoria: $category ===${NC}"
    
    local category_passed=0
    local category_total=0
    
    # Encontrar todos os arquivos .go na categoria
    for file in invalid_tests/$category/*.go; do
        if [ -f "$file" ]; then
            ((TOTAL_TESTS++))
            ((category_total++))
            
            test_file "$file"
            result=$?
            
            if [ $result -eq 0 ]; then
                ((PASSED_TESTS++))
                ((category_passed++))
            elif [ $result -eq 2 ]; then
                ((NO_ERRORS++))
            else
                ((FAILED_TESTS++))
            fi
        fi
    done
    
    if [ $category_total -gt 0 ]; then
        local success_rate=$((category_passed * 100 / category_total))
        echo -e "${BLUE}Categoria $category: $category_passed/$category_total detectaram erros corretamente (${success_rate}%)${NC}"
    else
        echo -e "${YELLOW}Nenhum arquivo encontrado em $category${NC}"
    fi
    echo ""
}

# Função para mostrar relatório final
show_summary() {
    echo ""
    echo "==================================================================================="
    echo "                              RELATÓRIO FINAL"
    echo "==================================================================================="
    echo ""
    
    # Estatísticas gerais
    if [ $TOTAL_TESTS -gt 0 ]; then
        local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
        local no_error_rate=$((NO_ERRORS * 100 / TOTAL_TESTS))
        local failure_rate=$((FAILED_TESTS * 100 / TOTAL_TESTS))
        
        echo -e "${GREEN}✅ Testes que passaram (detectaram erros): $PASSED_TESTS/$TOTAL_TESTS (${success_rate}%)${NC}"
        echo -e "${RED}❌ Arquivos que NÃO detectaram erros: $NO_ERRORS/$TOTAL_TESTS (${no_error_rate}%)${NC}"
        echo -e "${YELLOW}⚠️  Falhas de execução: $FAILED_TESTS/$TOTAL_TESTS (${failure_rate}%)${NC}"
        echo ""
    fi
    
    # Listar arquivos com problemas
    if [ ${#FAILED_FILES[@]} -gt 0 ]; then
        echo -e "${YELLOW}Arquivos com falhas de execução:${NC}"
        for file in "${FAILED_FILES[@]}"; do
            echo "  - $file"
        done
        echo ""
    fi
    
    if [ ${#NO_ERROR_FILES[@]} -gt 0 ]; then
        echo -e "${RED}Arquivos que deveriam ter erros mas passaram:${NC}"
        for file in "${NO_ERROR_FILES[@]}"; do
            echo "  - $file"
        done
        echo ""
    fi
    
    # Conclusão
    if [ $NO_ERRORS -eq 0 ] && [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}🎉 Todos os arquivos inválidos foram detectados corretamente!${NC}"
    elif [ $NO_ERRORS -eq 0 ]; then
        echo -e "${YELLOW}⚠️  Alguns testes falharam na execução, mas nenhum arquivo inválido passou sem detectar erros.${NC}"
    else
        echo -e "${RED}⚠️  Alguns arquivos inválidos não foram detectados como tal!${NC}"
        echo -e "${RED}    Isso indica que a análise semântica precisa ser melhorada.${NC}"
    fi
    
    echo ""
    echo "Análise concluída em $(date)"
}

# Função principal
main() {
    local target_category="$1"
    
    if [ -n "$target_category" ]; then
        echo "Iniciando testes da análise semântica para categoria: $target_category"
    else
        echo "Iniciando testes da análise semântica para arquivos inválidos..."
    fi
    echo "Total de arquivos encontrados: $(find invalid_tests -name "*.go" | wc -l)"
    echo ""
    
    # Compilar projeto
    compile_project
    
    # Testar cada categoria
    categories=("arrays" "declarations" "expressions" "functions" "keywords" "scopes" "statements" "syntax_errors")
    
    for category in "${categories[@]}"; do
        # Se uma categoria específica foi solicitada, pular as outras
        if [ -n "$target_category" ] && [ "$category" != "$target_category" ]; then
            continue
        fi
        
        if [ -d "invalid_tests/$category" ]; then
            test_category "$category"
        fi
    done
    
    # Mostrar relatório final
    show_summary
}

# Verificar se estamos no diretório correto
if [ ! -d "invalid_tests" ]; then
    echo -e "${RED}❌ Diretório invalid_tests não encontrado!${NC}"
    echo "Execute este script a partir do diretório raiz do projeto."
    exit 1
fi

# Executar função principal
main "$1"
