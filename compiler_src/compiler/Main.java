// compiler_src/Main.java
package compiler; // O pacote da sua classe Main

import java.io.IOException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// Imports para o seu lexer, parser e visitor, baseado nos pacotes que o ANTLR gera
// Certifique-se de que 'parser' é o pacote que você definiu no Makefile para as classes ANTLR
import Go_Parser.Go_Lexer;
import Go_Parser.Go_Parser;
import compiler.GoSemanticAnalyzer; // O pacote que você criou para o visitor

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java -cp <classpath> compiler.Main <input_file.go>");
            System.err.println("Example: make run_compiler FILE=inputs/myprogram.go");
            return;
        }

        String filePath = args[0];
        CharStream input = CharStreams.fromFileName(filePath); // Lê o código do arquivo

        // --- 1. Análise Léxica ---
        Go_Lexer lexer = new Go_Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // --- 2. Análise Sintática ---
        Go_Parser parser = new Go_Parser(tokens);
        ParseTree tree = parser.program(); // Inicia o parsing a partir da regra 'program'

        // Verifica erros sintáticos
        if (parser.getNumberOfSyntaxErrors() != 0) {
            System.err.println("Parsing finished with syntax errors. Aborting semantic analysis.");
            // Não imprime as tabelas se houver erros sintáticos, pois a árvore pode estar inconsistente.
            return;
        }

        // --- 3. Análise Semântica ---
        GoSemanticAnalyzer semanticAnalyzer = new GoSemanticAnalyzer();
        semanticAnalyzer.visit(tree); // Inicia o caminhamento da árvore de parse para a análise semântica

        // --- 4. Relatório Final e Impressão das Tabelas (Atividade 4.2) ---
        if (semanticAnalyzer.hasSemanticErrors()) {
            System.err.println("\nSemantic analysis finished with ERRORS. Please fix them.");
        } else {
            System.out.println("\nSemantic analysis completed successfully. No semantic errors found.");
        }

        // Imprime as tabelas de símbolos e de strings, independentemente de erros
        semanticAnalyzer.getSymbolTable().printTable();
        semanticAnalyzer.getStringTable().printTable();
    }
}