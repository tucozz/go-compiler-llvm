# go-compiler-llvm

## Repositório para o trabalho da disciplina de Compiladores.
Nome dos integrantes:
Arthur Estefanato Lopes, Gabriel Nascimento Oliveira, Pedro Henrique Bravim Duarte.

## 1. Introdução

A melhor forma de aprender sobre compiladores é construindo um! Esse é o objetivo do trabalho prático da disciplina: um projeto de desenvolvimento de um compilador, realizado ao longo de todo o curso, permitindo o estudo do conteúdo de forma prática.

## 2. Requisitos mínimos do projeto e simplificações
Dado que os projetos envolvem criar compiladores para linguagens de programação (LPs) reais, é certo que será necessário realizar várias simplificações dos aspectos da linguagem fonte, visto que praticamente todas as LPs possuem uma grande quantidade de funcionalidades.

### Elementos mínimos que o compilador deve tratar corretamente:

- Operações aritméticas e de comparação básicas (`+`, `*`, `<`, `==`, etc).
- Comandos de atribuição.
- Execução de blocos sequenciais de código.
- Pelo menos uma estrutura de:
  - Escolha: `if-then-else`
  - Repetição: `while`, `for`, etc.
- Declaração e manipulação de tipos básicos:
  - `int`
  - `real`
  - `string`
  - `bool` (quando aplicável à LP)
- Declaração e manipulação de pelo menos um tipo composto:
  - Vetores
  - Listas (como em Python)
- Declaração e execução correta de chamadas de função com número fixo de parâmetros (não precisa suportar *varargs*).
- Sistema de tipos que trata adequadamente todos os tipos permitidos.
- Operações de entrada e saída (IO) básicas utilizando `stdin` e `stdout`, para permitir testes.

### Elementos que **não** precisam ser considerados no projeto (Objetivos extras):

- Compilação separada:
  - `imports`, módulos, etc.
  
- Operações bitwise:
  - Shifts (esquerda e direita)
  - Operações bit a bit (`&`, `|`, `^`, etc.)
  
- Chamadas de funções não convencionais:
  - Funções com `varargs`
  - Parâmetros com valores default
  - Chamadas com nomes de parâmetros
  - *Packing/unpacking*

- Anotações de programas:
  - `@decorators` em Python e Java

- Comentários estruturados:
  - Exemplo: JavaDoc e similares

- Tratamento de exceções

- Uso de asserções (`assert`)

- Pré-processamento e macros:
  - `#define`, `#include` (em C)
  - Construções similares em outras linguagens

- Concorrência ou paralelismo:
  - `async`, `yield` (Python)
  - `synchronized` (Java)

- Gerência avançada de memória:
  - *Garbage collection* e outros mecanismos complexos
