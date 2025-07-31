# Comandos do Java
JAVAC = javac
JAVA = java

# Caminho para o JAR do ANTLR (relativo à raiz do projeto)
ANTLR_JAR = tools/antlr-4.13.2-complete.jar

# Nome da gramática principal para o pacote Java.
GRAMMAR_PACKAGE_NAME = Go_Parser

# Diretório base onde as gramáticas estão
GRAMMAR_BASE_DIR = grammar

# Caminho completo para o diretório de saída das classes Java geradas pelo ANTLR
OUTPUT_JAVA_DIR = $(GRAMMAR_BASE_DIR)/$(GRAMMAR_PACKAGE_NAME)

# NOVO: Diretório raiz do seu código Java manual.
# Baseado no seu 'tree' output, é a pasta 'compiler' na raiz do projeto.
COMPILER_SRC_BASE_DIR = compiler

# Diretório para onde as classes .class serão compiladas
COMPILER_BIN_DIR = bin

# Classpath completo para javac e java
# Inclui: diretório atual, ANTLR JAR, diretório base das gramáticas (para encontrar 'Go_Parser' pacote),
# o diretório de saída das classes compiladas do ANTLR, e a raiz dos seus pacotes manuais (compiler/)
# e o diretório de saída dos binários compilados (bin)
CLASSPATH = .:$(ANTLR_JAR):$(GRAMMAR_BASE_DIR):$(OUTPUT_JAVA_DIR):$(COMPILER_BIN_DIR):$(COMPILER_SRC_BASE_DIR)

# Comando ANTLR4 (definido para ser executado DA RAIZ do projeto)
ANTLR4 = $(JAVA) -jar $(ANTLR_JAR)

# Nomes dos arquivos de gramática (sem o caminho, pois o cd vai para grammar/)
LEXER_GRAMMAR_NAME = Go_Lexer.g
PARSER_GRAMMAR_NAME = Go_Parser.g

# Regra principal
all: antlr javac compiler_javac
	@echo "Compilação concluída."

# Regras 'antlr', 'antlr-lexer', 'antlr-parser' (permanecem as mesmas)
antlr: antlr-lexer antlr-parser

antlr-lexer:
	@mkdir -p $(OUTPUT_JAVA_DIR)
	cd $(GRAMMAR_BASE_DIR) && $(JAVA) -jar ../$(ANTLR_JAR) -no-listener -package $(GRAMMAR_PACKAGE_NAME) -o $(GRAMMAR_PACKAGE_NAME) $(LEXER_GRAMMAR_NAME)

antlr-parser: antlr-lexer
	cd $(GRAMMAR_BASE_DIR) && $(JAVA) -jar ../$(ANTLR_JAR) -no-listener -visitor -package $(GRAMMAR_PACKAGE_NAME) -o $(GRAMMAR_PACKAGE_NAME) $(PARSER_GRAMMAR_NAME)

# Compila os arquivos Java gerados pelo ANTLR
javac:
	@mkdir -p $(OUTPUT_JAVA_DIR)
	$(JAVAC) -cp $(CLASSPATH) $(OUTPUT_JAVA_DIR)/*.java

# NOVO: Regra para compilar o seu código Java manual (tabelas, visitor, Main)
compiler_javac: javac
	@mkdir -p $(COMPILER_BIN_DIR)
	# Compila todos os arquivos Java a partir da raiz de 'compiler/'
	$(JAVAC) -cp $(CLASSPATH) -d $(COMPILER_BIN_DIR) \
		$(COMPILER_SRC_BASE_DIR)/ast/*.java \
		$(COMPILER_SRC_BASE_DIR)/compiler/*.java \
		$(COMPILER_SRC_BASE_DIR)/compiler/tables/*.java \
		$(COMPILER_SRC_BASE_DIR)/compiler/typing/*.java

# Executa o compilador (Main.java)
run_compiler: all # Depende de 'all' para garantir que tudo esteja compilado
	$(JAVA) -cp $(CLASSPATH) compiler.Main $(FILE)

# Limpa todos os arquivos gerados
clean:
	@rm -rf $(OUTPUT_JAVA_DIR)
	@rm -f $(GRAMMAR_BASE_DIR)/*.tokens
	@rm -f $(GRAMMAR_BASE_DIR)/*.interp
	@rm -rf $(COMPILER_BIN_DIR)

.PHONY: all antlr antlr-lexer antlr-parser javac compiler_javac run_compiler clean