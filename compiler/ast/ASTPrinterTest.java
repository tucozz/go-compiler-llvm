package compiler.ast;

import compiler.typing.GoType;

/**
 * Teste simples para demonstrar o ASTPrinter funcionando
 */
public class ASTPrinterTest {
    
    public static void main(String[] args) {
        System.out.println("=== TESTE DO ASTPRINTER ===\n");
        
        // Criando uma AST simples manualmente para demonstração
        // Simula: var x int = 5 + 3
        
        // Nó raiz do programa
        AST program = new AST(NodeKind.PROGRAM_NODE, GoType.NO_TYPE);
        
        // Declaração de variável: var x int = 5 + 3
        AST varDecl = new AST(NodeKind.VAR_DECL_NODE, "x", GoType.INT);
        
        // Expressão de soma: 5 + 3
        AST plusExpr = new AST(NodeKind.PLUS_NODE, GoType.INT);
        
        // Literal 5
        AST five = new AST(NodeKind.INT_VAL_NODE, 5, GoType.INT);
        
        // Literal 3
        AST three = new AST(NodeKind.INT_VAL_NODE, 3, GoType.INT);
        
        // Montando a árvore
        plusExpr.addChild(five);
        plusExpr.addChild(three);
        varDecl.addChild(plusExpr);
        program.addChild(varDecl);
        
        // Gerando e exibindo o DOT
        String dotCode = ASTPrinter.toDot(program);
        
        System.out.println("Código DOT gerado:");
        System.out.println("==================");
        System.out.println(dotCode);
        System.out.println("==================");
        
        System.out.println("\n✅ Para visualizar a árvore:");
        System.out.println("1. Copie o código DOT acima");
        System.out.println("2. Cole em: https://dreampuf.github.io/GraphvizOnline/");
        System.out.println("3. Veja sua AST renderizada graficamente!");
    }
}
