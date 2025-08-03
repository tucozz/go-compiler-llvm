# Comandos do Java
JAVAC = javac
JAVA = java

# Caminho para o diretório principal do projeto
PROJECT_ROOT=$(shell dirname $(shell pwd))
# Caminho para o JAR do ANTLR em labs/tools
ANTLR_PATH=$(PROJECT_ROOT)/tools/antlr-4.13.2-complete.jar

# Caminho para o JAR do ANTLR
# ANTLR_PATH=../tools/antlr-4.13.2-complete.jar

# Classpath com ANTLR incluso
CLASSPATH = .:$(ANTLR_PATH)

# Comando ANTLR4
ANTLR4 = $(JAVA) -jar $(ANTLR_PATH)

# Nome da gramática (prefixo comum)
GRAMMAR_PREFIX = Go_

# Arquivos das gramáticas
GRAMMARS=Go_Lexer.g Go_Parser.g

OPT = -gui -tree

# Diretório para aonde vão os arquivos gerados
GEN_PATH=parser

# Executa o ANTLR e o compilador Java
all: antlr javac
	@echo "Compilação concluída."

# Executa o ANTLR para compilar as gramáticas
antlr: $(GRAMMARS)
	$(ANTLR4) -no-listener -o $(GEN_PATH) $(GRAMMARS)

# Executa o javac para compilar os arquivos gerados
javac:
	$(JAVAC) -cp $(CLASSPATH) $(GEN_PATH)/*.java

# Executa o parser. Comando: $ make run FILE=arquivo_de_teste
run:
ifeq ($(strip $(FILE)),)
	cd $(GEN_PATH) && $(JAVA) -cp $(CLASSPATH) org.antlr.v4.gui.TestRig $(GRAMMAR_PREFIX) program
else
	cd $(GEN_PATH) && $(JAVA) -cp $(CLASSPATH) org.antlr.v4.gui.TestRig $(GRAMMAR_PREFIX) program -gui ../$(FILE)
endif

# Executa o parser sem GUI (versão texto). Comando: $ make run-text FILE=arquivo_de_teste
run-text:
ifeq ($(strip $(FILE)),)
	@echo "Erro: É necessário especificar um arquivo. Use: make run-text FILE=../Lab02_Input/c01.ezl"
else
	cd $(GEN_PATH) && $(JAVA) -cp $(CLASSPATH) org.antlr.v4.gui.TestRig $(GRAMMAR_PREFIX) program -tokens ../$(FILE)
endif

# Executa o parser mostrando a árvore (versão texto). Comando: $ make run-tree FILE=arquivo_de_teste
run-tree:
ifeq ($(strip $(FILE)),)
	@echo "Erro: É necessário especificar um arquivo. Use: make run-tree FILE=../Lab02_Input/c01.ezl"
else
	cd $(GEN_PATH) && $(JAVA) -cp $(CLASSPATH) org.antlr.v4.gui.TestRig $(GRAMMAR_PREFIX) program -tree ../$(FILE)
endif

# Remove os arquivos gerados pelo ANTLR
clean:
	@rm -rf $(GEN_PATH)
