#!/bin/bash

# Script para testar geração de código LLVM em todos os arquivos valid_tests
# Autor: Gabriel
# Data: $(date)
#
# Uso:
#   ./test_llvm_valid.sh                           # Testa todas as categorias
#   ./test_llvm_valid.sh functions                 # Testa apenas categoria 'functions'
#   ./test_llvm_valid.sh --force-compile           # Força recompilação e testa tudo
#   ./test_llvm_valid.sh --force-compile arrays    # Força recompilação e testa arrays
#   make test_llvm_batch                           # Via Makefile (todas as categorias)
#
# Categorias disponíveis:
#   arrays, control_flow, declarations, expressions, functions,
#   literals, operators, scopes, statements, type_checking, variables
#
# Este script testa a geração de código LLVM usando a flag --llvm
# Arquivos que falham podem ser devido a:
# - Recursos não implementados no compilador
# - Erros na gramática ou analisador semântico
# - Problemas específicos no gerador de código LLVM
#
# Otimizações:
# - Compila o projeto apenas uma vez no início
# - Usa execução rápida (make rcf) para testes subsequentes
# - Pula compilação se o projeto já estiver compilado

echo "==================================================================================="
echo "                  SCRIPT DE TESTE DA GERAÇÃO DE CÓDIGO LLVM"
echo "==================================================================================="
echo ""

# Contadores
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SEMANTIC_ERRORS=0
LLVM_GENERATION_ERRORS=0

# Arrays para armazenar resultados
FAILED_FILES=()
SEMANTIC_ERROR_FILES=()
LLVM_ERROR_FILES=()

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

# Função para verificar se o projeto está compilado
check_compilation() {
    if [ ! -d "bin" ] || [ ! -f "bin/compiler/Main.class" ]; then
        echo -e "${YELLOW}⚠️  Projeto não está compilado. Compilando...${NC}"
        compile_project
        return 0
    else
        echo -e "${GREEN}✅ Projeto já está compilado${NC}"
        return 1
    fi
}

# Função para executar teste rápido (assume projeto já compilado)
run_test_fast() {
    local file=$1
    local output=$(timeout 15s make rcf FILE="--llvm $file" 2>&1)
    local exit_code=$?
    echo "$output"
    return $exit_code
}

# Função para testar um único arquivo
test_file() {
    local file=$1
    local category=$(dirname "$file" | sed 's/valid_tests\///')
    local filename=$(basename "$file")

    echo -n "Testando $category/$filename... "

    # Executar o compilador com flag LLVM usando execução rápida
    output=$(run_test_fast "$file")
    exit_code=$?

    # Verificar se houve timeout
    if [ $exit_code -eq 124 ]; then
        echo -e "${RED}TIMEOUT${NC}"
        FAILED_FILES+=("$file (timeout)")
        return 1
    fi

    # Verificar se houve erro de compilação/execução
    if [ $exit_code -ne 0 ]; then
        echo -e "${RED}FALHA${NC} (exit code: $exit_code)"
        FAILED_FILES+=("$file")
        return 1
    fi

    # Verificar se houve erros semânticos
    if echo "$output" | grep -q "RESULTADO: Compilação falhou devido a erros semânticos"; then
        echo -e "${YELLOW}ERRO SEMÂNTICO${NC}"
        SEMANTIC_ERROR_FILES+=("$file")
        return 2
    fi

    # Verificar se houve erros sintáticos
    if echo "$output" | grep -q "RESULTADO: Compilação falhou devido a erros sintáticos"; then
        echo -e "${RED}ERRO SINTÁTICO${NC}"
        FAILED_FILES+=("$file")
        return 1
    fi

    # Verificar se houve erros na geração de LLVM
    if echo "$output" | grep -q "RESULTADO: Processo concluído com sucesso!"; then
        # Verificar se o código LLVM foi gerado
        if echo "$output" | grep -q "INÍCIO DO CÓDIGO LLVM IR"; then
            echo -e "${GREEN}PASSOU${NC}"
            return 0
        else
            echo -e "${RED}ERRO LLVM${NC} (código não gerado)"
            LLVM_ERROR_FILES+=("$file")
            return 3
        fi
    else
        echo -e "${RED}FALHA${NC} (processo não concluído)"
        FAILED_FILES+=("$file")
        return 1
    fi
}

# Função para testar uma categoria
test_category() {
    local category=$1
    echo -e "${BLUE}=== Testando categoria: $category ===${NC}"

    local category_passed=0
    local category_total=0

    # Encontrar todos os arquivos .go na categoria
    for file in valid_tests/$category/*.go; do
        if [ -f "$file" ]; then
            ((TOTAL_TESTS++))
            ((category_total++))

            test_file "$file"
            result=$?

            if [ $result -eq 0 ]; then
                ((PASSED_TESTS++))
                ((category_passed++))
            elif [ $result -eq 2 ]; then
                ((SEMANTIC_ERRORS++))
            elif [ $result -eq 3 ]; then
                ((LLVM_GENERATION_ERRORS++))
            else
                ((FAILED_TESTS++))
            fi
        fi
    done

    if [ $category_total -gt 0 ]; then
        local success_rate=$((category_passed * 100 / category_total))
        echo -e "${BLUE}Categoria $category: $category_passed/$category_total passaram (${success_rate}%)${NC}"
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
        local semantic_rate=$((SEMANTIC_ERRORS * 100 / TOTAL_TESTS))
        local llvm_error_rate=$((LLVM_GENERATION_ERRORS * 100 / TOTAL_TESTS))
        local failure_rate=$((FAILED_TESTS * 100 / TOTAL_TESTS))

        echo -e "${GREEN}✅ Testes que passaram: $PASSED_TESTS/$TOTAL_TESTS (${success_rate}%)${NC}"
        echo -e "${YELLOW}⚠️  Erros semânticos: $SEMANTIC_ERRORS/$TOTAL_TESTS (${semantic_rate}%)${NC}"
        echo -e "${RED}🔧 Erros na geração LLVM: $LLVM_GENERATION_ERRORS/$TOTAL_TESTS (${llvm_error_rate}%)${NC}"
        echo -e "${RED}❌ Falhas de compilação: $FAILED_TESTS/$TOTAL_TESTS (${failure_rate}%)${NC}"
        echo ""
    fi

    # Listar arquivos com problemas
    if [ ${#FAILED_FILES[@]} -gt 0 ]; then
        echo -e "${RED}Arquivos com falhas de compilação:${NC}"
        for file in "${FAILED_FILES[@]}"; do
            echo "  - $file"
        done
        echo ""
    fi

    if [ ${#SEMANTIC_ERROR_FILES[@]} -gt 0 ]; then
        echo -e "${YELLOW}Arquivos com erros semânticos:${NC}"
        for file in "${SEMANTIC_ERROR_FILES[@]}"; do
            echo "  - $file"
        done
        echo ""
    fi

    if [ ${#LLVM_ERROR_FILES[@]} -gt 0 ]; then
        echo -e "${RED}Arquivos com erros na geração de LLVM:${NC}"
        for file in "${LLVM_ERROR_FILES[@]}"; do
            echo "  - $file"
        done
        echo ""
    fi

    # Conclusão
    if [ $FAILED_TESTS -eq 0 ] && [ $LLVM_GENERATION_ERRORS -eq 0 ]; then
        echo -e "${GREEN}🎉 Todos os testes passaram! Código LLVM gerado com sucesso!${NC}"
    elif [ $LLVM_GENERATION_ERRORS -eq 0 ]; then
        echo -e "${YELLOW}⚠️  Alguns testes falharam, mas geração de LLVM funcionou onde esperado.${NC}"
    else
        echo -e "${RED}⚠️  Problemas na geração de código LLVM detectados.${NC}"
    fi

    echo ""
    echo "Análise concluída em $(date)"
}

# Função principal
main() {
    local target_category="$1"
    local force_compile=false
    
    # Verificar se foi passado --force-compile
    if [ "$1" = "--force-compile" ]; then
        force_compile=true
        target_category="$2"
    fi

    if [ -n "$target_category" ]; then
        echo "Iniciando testes da geração LLVM para categoria: $target_category"
    else
        echo "Iniciando testes da geração LLVM..."
    fi
    echo "Total de arquivos encontrados: $(find valid_tests -name "*.go" | wc -l)"
    echo ""

    # Compilar projeto ou verificar compilação
    if [ "$force_compile" = true ]; then
        compile_project
    else
        check_compilation
    fi

    # Testar cada categoria
    categories=("arrays" "control_flow" "declarations" "expressions" "functions"
                "literals" "operators" "scopes" "statements" "type_checking" "variables")

    for category in "${categories[@]}"; do
        # Se uma categoria específica foi solicitada, pular as outras
        if [ -n "$target_category" ] && [ "$category" != "$target_category" ]; then
            continue
        fi

        if [ -d "valid_tests/$category" ]; then
            test_category "$category"
        fi
    done

    # Mostrar relatório final
    show_summary
}

# Verificar se estamos no diretório correto
if [ ! -d "valid_tests" ]; then
    echo -e "${RED}❌ Diretório valid_tests não encontrado!${NC}"
    echo "Execute este script a partir do diretório raiz do projeto."
    exit 1
fi

# Executar função principal
main "$1"
