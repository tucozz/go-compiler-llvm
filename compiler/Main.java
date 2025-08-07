package compiler;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// Imports para o lexer e parser ANTLR (gerados na pasta Go_Parser)
import Go_Parser.Go_Lexer;
import Go_Parser.Go_Parser;

import compiler.ast.AST;
import compiler.ast.ASTPrinter;
import compiler.checker.GoSemanticChecker;

/**
 * Classe principal do compilador.
 * Orquestra as fases de análise: léxica, sintática e semântica.
 * Adicionada a funcionalidade de imprimir a AST para teste/visualização.
 * * Uso: java compiler.Main <caminho_para_arquivo.go>
 * * O programa termina com status 0 em caso de sucesso (nenhum erro semântico)
 * e com status 1 caso contrário.
 */
public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java -cp <classpath> compiler.Main <arquivo_de_entrada.go>");
            System.err.println("Exemplo: make test FILE=valid_tests/declarations/test1.go");
            System.exit(1);
            return;
        }

        String filePath = args[0];
        System.out.println(">>> Iniciando Compilador para o arquivo: " + filePath);
        System.out.println("--------------------------------------------------");

        try {
            // 1. ANÁLISE LÉXICA E SINTÁTICA
            CharStream input = CharStreams.fromFileName(filePath);
            Go_Lexer lexer = new Go_Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Go_Parser parser = new Go_Parser(tokens);
            
            // CORREÇÃO: O método da regra inicial provavelmente é 'program()', não 'programRule()'.
            ParseTree tree = parser.program(); 

            if (parser.getNumberOfSyntaxErrors() > 0) {
                System.err.println("ERRO: Foram encontrados erros sintáticos. A compilação será interrompida.");
                System.exit(1);
                return;
            }
            System.out.println("[FASE 1/2] Análise Sintática concluída com sucesso.");

            // 2. ANÁLISE SEMÂNTICA
            System.out.println("[FASE 2/2] Iniciando Análise Semântica...");
            GoSemanticChecker semanticChecker = new GoSemanticChecker();
            AST ast = semanticChecker.visit(tree);
            
            // Imprime o relatório de erros e tabelas
            semanticChecker.printReport();

            // Verifica se ocorreram erros semânticos
            if (semanticChecker.hasSemanticErrors()) {
                System.err.println("\nRESULTADO: Compilação falhou devido a erros semânticos.");
                System.exit(1);
            } else {
                // 3. IMPRESSÃO DA AST
                System.out.println("\n--------------------------------------------------");
                System.out.println(">>> [FASE 3/3] Gerando Abstract Syntax Tree (AST)...");
                if (ast != null) {
                    System.out.println("Para visualizar a árvore, copie o código DOT abaixo");
                    System.out.println("e cole em um visualizador como: https://dreampuf.github.io/GraphvizOnline/");
                    System.out.println("\n--- INÍCIO DO CÓDIGO DOT ---");
                    // Gera e imprime a representação da AST em formato DOT
                    System.out.println(ASTPrinter.toDot(ast));
                    System.out.println("--- FIM DO CÓDIGO DOT ---\n");
                } else {
                    System.out.println("A AST não foi gerada devido a erros anteriores.");
                }

                System.out.println("RESULTADO: Compilação concluída com sucesso! Nenhum erro encontrado.");
                System.exit(0);
            }

        } catch (IOException e) {
            System.err.println("ERRO: Não foi possível ler o arquivo '" + filePath + "'.");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERRO: Ocorreu um erro inesperado durante a compilação.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
