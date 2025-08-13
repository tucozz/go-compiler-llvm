package compiler;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// Imports para o lexer e parser ANTLR (pacote Go_Parser)
import Go_Parser.Go_Lexer;
import Go_Parser.Go_Parser;

import compiler.checker.GoSemanticChecker;

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
            System.out.println("1. Análise Léxica...");
            CharStream input = CharStreams.fromFileName(filePath);
            Go_Lexer lexer = new Go_Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // === 2. ANÁLISE SINTÁTICA ===
            System.out.println("2. Análise Sintática...");
            Go_Parser parser = new Go_Parser(tokens);
            ParseTree tree = parser.program(); // Regra inicial da gramática

            // === 3. ANÁLISE SEMÂNTICA ===
            System.out.println("3. Análise Semântica...");
            GoSemanticChecker visitor = new GoSemanticChecker();

            // Visitar a árvore sintática com o visitor
            visitor.visit(tree);

            // Imprimir relatório da análise
            visitor.printReport();

            System.out.println("\n✅ Análise concluída!");

            // Verificar erros sintáticos
            if (parser.getNumberOfSyntaxErrors() > 0) {
                System.err.println("❌ Erros sintáticos encontrados!");
                return;
            }

            System.out.println("✅ Parsing concluído com sucesso!");
            System.out.println("Parse tree nodes: " + tree.getChildCount());

            // === 4. AST ===
            visitor.printAST();

        } catch (Exception e) {
            System.err.println("❌ Erro durante a análise: " + e.getMessage());
            e.printStackTrace();
        }
    }
}