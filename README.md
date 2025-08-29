# Compilador para Subset da Linguagem Go

## 1. Visão Geral

Este projeto acadêmico apresenta um compilador para um subset da linguagem de programação Go. A ferramenta foi desenvolvida em Java, utilizando o ANTLR para a análise léxica e sintática, e é capaz de operar em dois modos distintos:

1.  **Modo Interpretador**: Executa o código-fonte diretamente, passo a passo, após a análise semântica. Ideal para testes rápidos e depuração.
2.  **Modo Compilador**: Gera código intermediário no formato LLVM IR (`.ll`), que pode ser posteriormente compilado para um executável nativo, transformando o código Go em um programa de máquina.

O compilador realiza as etapas clássicas de análise léxica, sintática e semântica, construindo uma Árvore Sintática Abstrata (AST) e utilizando tabelas de símbolos para garantir a correção do código.

---

## 2. Pré-requisitos

Para compilar e executar este projeto, os seguintes componentes são necessários:

* **Java Development Kit (JDK)**: Versão 11 ou superior.
* **ANTLR v4**: A ferramenta (`antlr-4.13.2-complete.jar` já inclusa no diretório `tools/`) e as bibliotecas de runtime.
* **GNU Make**: Para facilitar o processo de compilação do projeto.
* **LLVM e Clang**: Necessários para o modo compilador, para transformar o código `.ll` gerado em um executável.

---

## 3. Como Executar

O projeto utiliza um `Makefile` para simplificar a compilação e execução.

### 3.1. Compilando o Projeto

Antes de qualquer coisa, compile o código-fonte do compilador:

```bash
make
```

### 3.2. Modo Interpretador

Para analisar e executar um arquivo `.go` diretamente:

```bash
make rc FILE="caminho/para/seu/arquivo.go"
```

**Exemplo:**

```bash
make rc FILE="valid_tests/declarations/test1.go"
```

O terminal exibirá o relatório da análise semântica, a AST em formato DOT (para visualização) e, em seguida, a saída da execução do programa.

### 3.3. Modo Compilador (Go -> LLVM -> Executável)

Este é um processo de três etapas para transformar seu código `.go` em um programa executável.

**Passo 1: Gerar o arquivo LLVM IR (`.ll`)**

Use a flag `--llvm` para instruir o compilador a gerar o código intermediário.

```bash
make rc FILE="--llvm caminho/para/seu/arquivo.go"
```

**Exemplo:**
Suponha que você queira compilar `valid_tests/functions/test1.go`.

```bash
make rc FILE="--llvm valid_tests/functions/test1.go"
```

Isso criará um arquivo chamado `valid_tests/functions/test1.ll`.

**Passo 2: Compilar o `.ll` para Assembly (`.s`)**

Use o compilador estático do LLVM (`llc`) para converter o código intermediário em assembly nativo da sua máquina.

```bash
llc valid_tests/functions/test1.ll -o test1.s
```

**Passo 3: Criar o Executável**

Use o `clang` para montar e lincar o arquivo assembly, criando o executável final.

```bash
clang test1.s -o meu_programa
```

**Passo 4: Executar o Programa**

Agora você pode executar seu programa compilado!

```bash
./meu_programa
```

---

## 4. Testes Automatizados em Lote

Para validar a robustez do compilador, foi criado um script de teste que automatiza o workflow de compilação para todos os casos de teste.

### 4.1. Como Usar

O script `test_compiler.sh` percorre recursivamente um diretório, tenta compilar cada arquivo `.go` e reporta quais falharam.

**1. Dê permissão de execução ao script (apenas uma vez):**

```bash
chmod +x test_compiler.sh
```

**2. Execute o script, passando o diretório de testes:**

```bash
./test_compiler.sh valid_tests
```

O script exibirá uma mensagem de sucesso para cada arquivo compilado corretamente e, para os que falharem, mostrará em qual etapa o erro ocorreu (`make`, `llc` ou `clang`) e o log de erro correspondente.

**Exemplo de Saída:**

```
🚀 Iniciando o teste completo do compilador no diretório: valid_tests
============================================================
✅ Sucesso: valid_tests/declarations/test1.go
✅ Sucesso: valid_tests/declarations/test2.go

▶️  Testando arquivo: valid_tests/expressions/test3.go
   ❌ Falhou (Passo 2: llc): Erro ao gerar o arquivo assembly.
      --- Log de Erro ---
      llc: error: llc: valid_tests/expressions/test3.ll:10:1: error: expected instruction opcode
      entry:
      ^
      -------------------

============================================================
🏁 Testes concluídos!

Resumo: 80 de 81 testes passaram.

❌ Arquivos que falharam:
  - valid_tests/expressions/test3.go
```

---

## 5. Cobertura dos Casos de Teste

O projeto inclui um conjunto abrangente de casos de teste, divididos em `valid_tests` (código que deve compilar com sucesso) e `invalid_tests` (código que deve ser rejeitado pelo analisador semântico).

A cobertura inclui:

* **Declarações**: Testes para `var`, `const` e declarações curtas (`:=`).
* **Tipos**: Verificação de tipos numéricos (`int`, `float64`), `string` e `bool`.
* **Expressões**: Operações aritméticas, lógicas e de comparação.
* **Literais**: Validação de literais inteiros, de ponto flutuante, strings e booleanos.
* **Estruturas de Controle**: Testes para `if-else` e laços `for` (clássico, "while" e infinito).
* **Funções**: Declaração, chamadas, parâmetros e múltiplos retornos.
* **Escopo**: Verificação de escopo de variáveis em blocos, laços e funções.
* **Arrays**: Declaração, acesso a índices e atribuição.
* **Funções Built-in**: Testes extensivos para `println` e `scanln` com diferentes tipos de argumentos.
* **Checagem de Erros**: Um conjunto de testes em `invalid_tests` para garantir que o compilador detecta corretamente erros semânticos, como:
    * Redeclaração de variáveis.
    * Uso de variáveis não declaradas.
    * Incompatibilidade de tipos em atribuições e operações.
    * Número incorreto de argumentos em chamadas de função.
    * Uso de `break`/`continue` fora de laços.
