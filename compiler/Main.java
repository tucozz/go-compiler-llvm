package compiler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
import compiler.interpreter.GoInterpreter;
import compiler.codegen.GoCodegenVisitor; // <-- IMPORT ADICIONADO

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Uso: java compiler.Main [--llvm | -c] <ficheiro_entrada.go>");
            System.err.println("Exemplo (interpretar): make rc FILE=valid_tests/declarations/test1.go");
            System.err.println("Exemplo (compilar):  make rc FILE=\"--llvm valid_tests/declarations/test1.go\"");
            return;
        }

        // --- Processamento dos Argumentos ---
        List<String> argsList = Arrays.asList(args);
        boolean compileMode = argsList.contains("--llvm") || argsList.contains("-c");
        String filePath = "";
        for (String arg : args) {
            if (!arg.equals("--llvm") && !arg.equals("-c")) {
                filePath = arg;
                break;
            }
        }

        if (filePath.isEmpty()) {
            System.err.println("Erro: Arquivo de entrada não especificado.");
            return;
        }
        
        String mode = compileMode ? "Compilação (LLVM)" : "Interpretação";
        System.out.println("Go Compiler - Modo: " + mode);
        System.out.println("Ficheiro: " + filePath);
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
            ParseTree tree = parser.program();

            // === 3. ANÁLISE SEMÂNTICA ===
            System.out.println("3. Análise Semântica...");
            GoSemanticChecker checker = new GoSemanticChecker();
            AST ast = checker.visit(tree);
            checker.printReport();

            // Verifica se ocorreram erros
            if (checker.hasSemanticErrors() || parser.getNumberOfSyntaxErrors() > 0) {
                System.err.println("\nRESULTADO: Compilação falhou devido a erros.");
                return;
            }
            
            System.out.println("\n--------------------------------------------------");

            if (compileMode) {
                // --- 4. GERAÇÃO DE CÓDIGO LLVM ---
                System.out.println(">>> Gerando código LLVM IR...");
                GoCodegenVisitor codegen = new GoCodegenVisitor();
                String llvmIr = codegen.run(ast);
                
                System.out.println("\n--- INÍCIO DO CÓDIGO LLVM IR ---");
                System.out.println(llvmIr);
                System.out.println("--- FIM DO CÓDIGO LLVM IR ---\n");
                
                System.out.println("Para executar o código gerado (requer LLVM instalado):");
                System.out.println("1. Guarde a saída em um arquivo (ex: output.ll)");
                System.out.println("2. Execute com o interpretador LLVM: lli output.ll");
                System.out.println("3. Verifique o código de saída: echo $?");

            } else {
                // --- 4. MODO INTERPRETADOR ---
                System.out.println(">>> Gerando Abstract Syntax Tree (AST)...");
                if (ast != null) {
                    System.out.println("Para visualizar a árvore, copie o código DOT abaixo");
                    System.out.println("e cole num visualizador como: https://dreampuf.github.io/GraphvizOnline/");
                    System.out.println("\n--- INÍCIO DO CÓDIGO DOT ---");
                    System.out.println(ASTPrinter.toDot(ast));
                    System.out.println("--- FIM DO CÓDIGO DOT ---\n");
                }

                System.out.println(">>> Executando o interpretador...");
                GoInterpreter interpreter = new GoInterpreter();
                interpreter.execute(ast);
                System.out.println(">>> Execução concluída.");
            }

            System.out.println("\nRESULTADO: Processo concluído com sucesso!");

        } catch (Exception e) {
            System.err.println("❌ Erro durante a análise: " + e.getMessage());
            e.printStackTrace();
        }
    }
}