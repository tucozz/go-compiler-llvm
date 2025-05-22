# Comando do compilador Java
JAVAC=javac
# Comando da JVM
JAVA=java
# ROOT é a raiz dos diretórios com todos os roteiros de laboratórios
YEAR=$(shell pwd | grep -o '20..-.')
ROOT := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
# Caminho para o JAR do ANTLR em labs/tools
ANTLR_PATH=$(ROOT)tools/antlr-4.13.2-complete.jar
# Opção de configuração do CLASSPATH para o ambiente Java
CLASS_PATH_OPTION=-cp .:$(ANTLR_PATH)
# Configuração do comando de compilação do ANTLR
ANTLR4=$(JAVA) -jar $(ANTLR_PATH)
# Configuração do ambiente de teste do ANTLR
GRUN=$(JAVA) $(CLASS_PATH_OPTION) org.antlr.v4.gui.TestRig
# Diretório para os arquivos gerados
GEN_DIR := $(dir $(GRAMMAR))
GEN_PATH := $(GEN_DIR)lexer

# Espera-se que a variável GRAMMAR seja definida na linha de comando: make GRAMMAR=path/Exemplo01.g
ifndef GRAMMAR
$(error É necessário passar a variável GRAMMAR, ex: make GRAMMAR=subdir/Exemplo01.g)
endif

# Nome base da gramática (sem path e sem extensão)
GRAMMAR_NAME=$(basename $(notdir $(GRAMMAR)))

# Executa o ANTLR e o compilador Java
all: antlr javac
	@echo "Done."

# Executa o ANTLR para compilar a gramática
antlr:
	cd $(GEN_DIR) && $(ANTLR4) -o lexer $(notdir $(GRAMMAR))

# Executa o javac para compilar os arquivos gerados
javac:
	$(JAVAC) $(CLASS_PATH_OPTION) $(GEN_PATH)/*.java

# Executa o lexer. Comando: $ make run GRAMMAR=subdir/Exemplo01.g FILE=arquivo_de_teste
run:
	@cd $(GEN_PATH) && $(GRUN) $(GRAMMAR_NAME) tokens -tokens $(FILE)

# Remove os arquivos gerados pelo ANTLR
clean:
	@rm -rf $(GEN_PATH)

# Torna os scripts executáveis
fix-permissions:
	chmod +x run_all_tests.sh compare_outputs.sh

test: fix-permissions
	@scripts/generate_outputs.sh
	@scripts/test_diff.sh