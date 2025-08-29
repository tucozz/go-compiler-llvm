#!/bin/bash

# Script para testar o workflow completo do compilador Go->LLVM
# Uso: ./test_compiler.sh <diretorio_de_testes> [--verbose]
# Exemplo: ./test_compiler.sh valid_tests

# --- Validação dos Argumentos de Entrada ---
if [ -z "$1" ]; then
    echo "❌ Erro: Por favor, forneça o diretório raiz dos casos de teste."
    echo "Uso: $0 <diretorio_de_testes>"
    exit 1
fi

if [ ! -d "$1" ]; then
    echo "❌ Erro: O diretório '$1' não foi encontrado."
    exit 1
fi

TEST_DIR=$1
FAILED_TESTS=()
PASSED_COUNT=0
TOTAL_COUNT=0
VERBOSE=false

# Verifica se o modo verbose foi ativado
if [[ "$2" == "--verbose" || "$2" == "-v" ]]; then
    VERBOSE=true
    echo "🔍 Modo verbose ativado."
fi


# Cores para o output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # Sem Cor

# --- Início do Processo de Teste ---
echo -e "${YELLOW}🚀 Iniciando o teste completo do compilador no diretório: $TEST_DIR${NC}"
echo "============================================================"

# Usa um loop 'for' para evitar problemas com subshells do 'while read'
for GO_FILE in $(find "$TEST_DIR" -type f -name "*.go"); do
    ((TOTAL_COUNT++))
    echo -e "\n▶️  Testando arquivo: ${YELLOW}$GO_FILE${NC}"

    BASE_NAME=$(basename "$GO_FILE" .go)
    DIR_NAME=$(dirname "$GO_FILE")
    LL_FILE="$DIR_NAME/$BASE_NAME.ll"
    S_FILE="$DIR_NAME/$BASE_NAME.s"
    EXEC_FILE="$DIR_NAME/$BASE_NAME.out"
    LOG_FILE=$(mktemp) # Cria um arquivo de log temporário

    # --- Passo 1: Compilar .go para .ll ---
    echo "   1. Gerando LLVM IR (.ll)..."
    CMD_MAKE="make rcf FILE=\"--llvm $GO_FILE\""
    if [ "$VERBOSE" = true ]; then
        echo -e "      ${CYAN}Executando:${NC} $CMD_MAKE"
    fi
    
    # Executa o comando e captura toda a saída (stdout e stderr) para o log
    eval "$CMD_MAKE" > "$LOG_FILE" 2>&1
    
    if [ $? -ne 0 ]; then
        echo -e "   ${RED}Falhou:${NC} Erro ao executar 'make' para gerar o arquivo .ll."
        echo -e "      ${CYAN}--- Log de Erro ---${NC}"
        sed 's/^/      /' "$LOG_FILE" # Imprime o log indentado
        echo -e "      ${CYAN}-------------------${NC}"
        FAILED_TESTS+=("$GO_FILE")
        rm -f "$LOG_FILE" # Limpa o log
        continue 
    fi

    # --- Passo 2: Compilar .ll para .s (Assembly) ---
    echo "   2. Gerando Assembly (.s)..."
    CMD_LLC="llc \"$LL_FILE\" -o \"$S_FILE\""
     if [ "$VERBOSE" = true ]; then
        echo -e "      ${CYAN}Executando:${NC} $CMD_LLC"
    fi
    
    eval "$CMD_LLC" > "$LOG_FILE" 2>&1

    if [ $? -ne 0 ]; then
        echo -e "   ${RED}Falhou:${NC} Erro ao executar 'llc' para gerar o arquivo assembly."
        echo -e "      ${CYAN}--- Log de Erro ---${NC}"
        sed 's/^/      /' "$LOG_FILE"
        echo -e "      ${CYAN}-------------------${NC}"
        FAILED_TESTS+=("$GO_FILE")
        rm -f "$LL_FILE" "$LOG_FILE"
        continue
    fi

    # --- Passo 3: Criar o executável ---
    echo "   3. Gerando o executável..."
    CMD_CLANG="clang -no-pie \"$S_FILE\" -o \"$EXEC_FILE\""
    if [ "$VERBOSE" = true ]; then
        echo -e "      ${CYAN}Executando:${NC} $CMD_CLANG"
    fi
    
    eval "$CMD_CLANG" > "$LOG_FILE" 2>&1
    
    if [ $? -ne 0 ]; then
        echo -e "   ${RED}Falhou:${NC} Erro ao executar 'clang' para criar o executável."
        echo -e "      ${CYAN}--- Log de Erro ---${NC}"
        sed 's/^/      /' "$LOG_FILE"
        echo -e "      ${CYAN}-------------------${NC}"
        FAILED_TESTS+=("$GO_FILE")
        rm -f "$LL_FILE" "$S_FILE" "$LOG_FILE"
        continue
    fi

    # --- Sucesso ---
    echo -e "   ${GREEN}✅ Sucesso!${NC} Executável '$EXEC_FILE' criado."
    ((PASSED_COUNT++))

    # --- Limpeza ---
    rm -f "$LL_FILE" "$S_FILE" "$EXEC_FILE" "$LOG_FILE"
done

# --- Relatório Final ---
echo ""
echo "============================================================"
echo -e "${YELLOW}🏁 Testes concluídos!${NC}"
echo ""
echo -e "Resumo: ${GREEN}${PASSED_COUNT}${NC} de ${TOTAL_COUNT} testes passaram."

if [ ${#FAILED_TESTS[@]} -ne 0 ]; then
    echo -e "\n${RED}❌ Arquivos que falharam:${NC}"
    for FAILED in "${FAILED_TESTS[@]}"; do
        echo "  - $FAILED"
    done
    exit 1
else
    echo -e "\n${GREEN}🎉 Todos os testes foram compilados com sucesso!${NC}"
    exit 0
fi
