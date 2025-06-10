# Comandos do Java
JAVAC = javac
JAVA = java

# Caminho para o JAR do ANTLR
ANTLR_JAR = ../tools/antlr-4.13.2-complete.jar

# Classpath com ANTLR incluso
CLASSPATH = .:$(ANTLR_JAR)

# Comando ANTLR4
ANTLR4 = $(JAVA) -jar $(ANTLR_JAR)

# Nome da gramática (prefixo comum)
GRAMMAR_PREFIX = Go_

OPT = -gui -tree

# Diretório de saída
OUT_DIR = parser

# Arquivos da gramática
LEXER_GRAMMAR = Go_Lexer.g
PARSER_GRAMMAR = Go_Parser.g

# Regra principal
all: antlr javac
	@echo "Compilação concluída."

# Compila os arquivos .g com ANTLR (lexer primeiro, depois parser)
antlr: antlr-lexer antlr-parser

# Gera o lexer primeiro
antlr-lexer:
	@mkdir -p $(OUT_DIR)
	$(ANTLR4) -no-listener -o $(OUT_DIR) $(LEXER_GRAMMAR)
	@cp $(OUT_DIR)/Go_Lexer.tokens grammar/ 2>/dev/null || true

# Gera o parser (depois do lexer)
antlr-parser: antlr-lexer
	$(ANTLR4) -no-listener -o $(OUT_DIR) $(PARSER_GRAMMAR)
	@rm -f grammar/Go_Lexer.tokens

# Compila os arquivos Java gerados
javac:
	$(JAVAC) -cp $(CLASSPATH) $(OUT_DIR)/*.java

# Executa o GRUN com ou sem um arquivo de entrada
# Exemplo de uso: make run FILE=exemplo.go
run:
ifeq ($(strip $(FILE)),)
	cd $(OUT_DIR) && $(JAVA) -cp ../$(ANTLR_JAR):. org.antlr.v4.gui.TestRig $(GRAMMAR_PREFIX) program $(OPT)
else
	cd $(OUT_DIR) && $(JAVA) -cp ../$(ANTLR_JAR):. org.antlr.v4.gui.TestRig $(GRAMMAR_PREFIX) program $(OPT) ../$(FILE)
endif

# Limpa os arquivos gerados
clean:
	@rm -rf $(OUT_DIR)/*.java $(OUT_DIR)/*.class $(OUT_DIR)/*.tokens $(OUT_DIR)/*.interp

.PHONY: all antlr antlr-lexer antlr-parser javac run clean