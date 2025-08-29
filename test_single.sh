#!/bin/bash

# Script para testar um único arquivo Go->LLVM
# Uso: ./test_single.sh <arquivo.go>

if [ -z "$1" ]; then
    echo "❌ Erro: Por favor, forneça o arquivo de teste."
    echo "Uso: $0 <arquivo.go>"
    exit 1
fi

if [ ! -f "$1" ]; then
    echo "❌ Erro: O arquivo '$1' não foi encontrado."
    exit 1
fi

GO_FILE=$1
BASE_NAME=$(basename "$GO_FILE" .go)
DIR_NAME=$(dirname "$GO_FILE")
LL_FILE="$DIR_NAME/$BASE_NAME.ll"
S_FILE="$DIR_NAME/$BASE_NAME.s"
EXEC_FILE="$DIR_NAME/$BASE_NAME.out"

echo "🚀 Testando arquivo: $GO_FILE"

# Passo 1: Gerar LLVM IR
echo "1. Gerando LLVM IR (.ll)..."
make rc FILE="--llvm $GO_FILE"
if [ $? -ne 0 ]; then
    echo "❌ Falhou na geração do LLVM IR"
    exit 1
fi

# Passo 2: Gerar Assembly
echo "2. Gerando Assembly (.s)..."
llc "$LL_FILE" -o "$S_FILE"
if [ $? -ne 0 ]; then
    echo "❌ Falhou na geração do Assembly"
    exit 1
fi

# Passo 3: Gerar executável
echo "3. Gerando executável..."
clang -no-pie "$S_FILE" -o "$EXEC_FILE"
if [ $? -ne 0 ]; then
    echo "❌ Falhou na geração do executável"
    exit 1
fi

echo "✅ Sucesso! Executável gerado: $EXEC_FILE"

# Limpeza
rm -f "$S_FILE"
