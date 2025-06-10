# ... (restante do Makefile)

# Diretório para os arquivos gerados
GEN_DIR := $(dir $(GRAMMAR))
# Alterar GEN_PATH para incluir o nome da gramática base, pois o ANTLR criará subdiretórios
# para o lexer e parser dentro de um diretório com o nome da gramática.
GEN_PATH := $(GEN_DIR)$(GRAMMAR_NAME_WITHOUT_EXT)/

# Nome base da gramática (sem path e sem extensão)
GRAMMAR_NAME=$(basename $(notdir $(GRAMMAR)))

# Extrai o nome da gramática sem a extensão para usar como o nome do pacote gerado pelo ANTLR
GRAMMAR_NAME_WITHOUT_EXT=$(shell basename $(GRAMMAR) .g)

# Executa o ANTLR e o compilador Java
all: antlr javac
    @echo "Done."

# Executa o ANTLR para compilar a gramática
# Use a opção -visitor para gerar o Visitor pattern, que é útil para a próxima etapa (AST/Visitor).
# Use a opção -package para definir o nome do pacote onde os arquivos serão gerados.
antlr:
    @mkdir -p $(GEN_PATH) # Garante que o diretório de destino exista
    @cd $(GEN_DIR) && $(ANTLR4) -visitor -package $(GRAMMAR_NAME_WITHOUT_EXT) $(notdir $(GRAMMAR)) -o $(GRAMMAR_NAME_WITHOUT_EXT)

# Executa o javac para compilar os arquivos gerados
javac:
    @$(JAVAC) $(CLASS_PATH_OPTION) $(GEN_PATH)*.java $(GEN_PATH)/*.java

# Executa o parser. Comando: $ make run GRAMMAR=subdir/Exemplo01.g FILE=arquivo_de_teste
run:
    @cd $(GEN_PATH) && $(GRUN) $(GRAMMAR_NAME_WITHOUT_EXT).$(GRAMMAR_NAME) program -tree $(FILE)

# Remove os arquivos gerados pelo ANTLR
clean:
    @rm -rf $(GEN_PATH)

# ... (restante do Makefile)