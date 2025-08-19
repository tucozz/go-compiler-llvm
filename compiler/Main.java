package compiler;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// Imports para o lexer e parser ANTLR (pacote Go_Parser)
import Go_Parser.Go_Lexer;
import Go_Parser.Go_Parser;
import compiler.ast.AST;
import compiler.ast.ASTPrinter;
import compiler.checker.GoSemanticChecker;
import compiler.interpreter.GoInterpreter; // <-- IMPORT ADICIONADO

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java -cp <classpath> compiler.Main <input_file.go>");
            System.err.println("Example: make test FILE=tests/arithmetics/test01/main.go");
            return;
        }

        String filePath = args[0];

        System.out.println("Go Compiler - Análise Semântica com ANTLR");
        System.out.println("Arquivo: " + filePath);
        System.out.println();

        try {
            // === 1. ANÁLISE LÉXICA ===
            System.out.println("\n1. Análise Léxica...");
            CharStream input = CharStreams.fromFileName(filePath);
            Go_Lexer lexer = new Go_Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // === 2. ANÁLISE SINTÁTICA ===
            System.out.println("\n2. Análise Sintática...");
            Go_Parser parser = new Go_Parser(tokens);
            ParseTree tree = parser.program(); // Regra inicial da gramática

            // === 3. ANÁLISE SEMÂNTICA ===
            System.out.println("\n3. Análise Semântica...");
            GoSemanticChecker checker = new GoSemanticChecker();

            // Visitar a árvore sintática com o visitor
            AST ast = checker.visit(tree);

            // Imprimir relatório da análise
            checker.printReport();

            // Verifica se ocorreram erros semânticos
            if (checker.hasSemanticErrors()) {
                System.err.println("\nRESULTADO: Compilação falhou devido a erros semânticos.");
                return;
            } else if (parser.getNumberOfSyntaxErrors() > 0) {
                System.err.println("\nRESULTADO: Compilação falhou devido a erros sintáticos.");
                return;
            } else {
                // 4. IMPRESSÃO DA AST
                System.out.println("\n--------------------------------------------------");
                System.out.println(">>> Gerando Abstract Syntax Tree (AST)...");
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

                // --- 5. EXECUÇÃO DO INTERPRETADOR ---
                System.out.println(">>> Executando o interpretador...");
                GoInterpreter interpreter = new GoInterpreter();
                interpreter.execute(ast);
                System.out.println(">>> Execução concluída.");
                // --- FIM DA EXECUÇÃO ---

                System.out.println("\nRESULTADO: Compilação concluída com sucesso! Nenhum erro encontrado.");
                return;
            }

        } catch (Exception e) {
            System.err.println("❌ Erro durante a análise: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
