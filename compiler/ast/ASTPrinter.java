package compiler.ast;

import compiler.typing.GoType;

/**
 * Classe utilitária para gerar uma representação da AST no formato DOT (Graphviz).
 * Isso permite a visualização gráfica da árvore AST do compilador Go.
 */
public class ASTPrinter {

    private static int nodeCounter;

    /**
     * Gera uma string no formato DOT para a AST fornecida.
     *
     * @param root O nó raiz da AST.
     * @return Uma string que pode ser usada para renderizar a árvore com o Graphviz.
     */
    public static String toDot(AST root) {
        nodeCounter = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("digraph GoAST {\n");
        sb.append("  node [shape=box, style=\"rounded,filled\", fillcolor=lightblue];\n");
        sb.append("  rankdir=TB;\n"); // Top to Bottom layout
        generateDotRecursive(root, sb);
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Método recursivo para percorrer a árvore e gerar as definições de nós e arestas.
     *
     * @param node O nó atual a ser processado.
     * @param sb   O StringBuilder para construir a string de saída.
     * @return O ID único do nó processado.
     */
    private static int generateDotRecursive(AST node, StringBuilder sb) {
        if (node == null) {
            return -1;
        }

        int currentNodeId = nodeCounter++;
        
        // Cria a label para o nó com informações detalhadas
        sb.append("  node").append(currentNodeId)
          .append(" [label=\"")
          .append(formatNodeLabel(node))
          .append("\"];\n");

        // Processa recursivamente os filhos
        for (AST child : node.getChildren()) {
            if (child != null) {
                int childNodeId = generateDotRecursive(child, sb);
                // Adiciona a aresta do nó atual para o filho
                sb.append("  node").append(currentNodeId)
                  .append(" -> node").append(childNodeId)
                  .append(";\n");
            }
        }

        return currentNodeId;
    }

    /**
     * Formata a label de um nó da AST para exibição no grafo.
     *
     * @param node O nó da AST.
     * @return Uma string formatada para a label do nó.
     */
    private static String formatNodeLabel(AST node) {
        StringBuilder label = new StringBuilder();
        
        // Nome do tipo do nó
        label.append(node.kind.toString());

        // Adiciona o tipo do nó, se houver e não for 'no_type'
        if (node.type != null && node.type != GoType.NO_TYPE) {
            label.append("\\nType: ").append(node.type.toString());
        }

        // Adiciona tipo anotado se diferente do tipo original
        if (node.getAnnotatedType() != null && 
            node.getAnnotatedType() != node.type && 
            node.getAnnotatedType() != GoType.NO_TYPE) {
            label.append("\\nAnnotated: ").append(node.getAnnotatedType().toString());
        }

        // Adiciona dados específicos dependendo do tipo do nó
        switch (node.kind) {
            // Identificadores e declarações
            case ID_NODE:
            case VAR_DECL_NODE:
            case CONST_DECL_NODE:
            case FUNC_DECL_NODE:
            case PARAM_NODE:
                if (node.text != null) {
                    label.append("\\nName: ").append(node.text);
                }
                break;

            // Valores literais
            case INT_VAL_NODE:
                label.append("\\nValue: ").append(node.intData);
                break;

            case REAL_VAL_NODE:
                label.append("\\nValue: ").append(node.floatData);
                break;

            case STR_VAL_NODE:
                if (node.text != null) {
                    // Escapa a string para exibição
                    String escapedText = node.text.replace("\\", "\\\\").replace("\"", "\\\"");
                    label.append("\\nValue: \\\"").append(escapedText).append("\\\"");
                }
                break;

            case BOOL_VAL_NODE:
                label.append("\\nValue: ").append(node.boolData ? "true" : "false");
                break;

            // Operadores - mostra símbolo
            case PLUS_NODE:
                label.append("\\nOp: +");
                break;
            case MINUS_NODE:
                label.append("\\nOp: -");
                break;
            case TIMES_NODE:
                label.append("\\nOp: *");
                break;
            case OVER_NODE:
                label.append("\\nOp: /");
                break;
            case MOD_NODE:
                label.append("\\nOp: %");
                break;
            case EQUAL_NODE:
                label.append("\\nOp: ==");
                break;
            case NOT_EQUAL_NODE:
                label.append("\\nOp: !=");
                break;
            case LESS_NODE:
                label.append("\\nOp: <");
                break;
            case GREATER_NODE:
                label.append("\\nOp: >");
                break;
            case LESS_EQ_NODE:
                label.append("\\nOp: <=");
                break;
            case GREATER_EQ_NODE:
                label.append("\\nOp: >=");
                break;
            case AND_NODE:
                label.append("\\nOp: &&");
                break;
            case OR_NODE:
                label.append("\\nOp: ||");
                break;
            case NOT_NODE:
                label.append("\\nOp: !");
                break;
            case UNARY_PLUS_NODE:
                label.append("\\nOp: +");
                break;
            case UNARY_MINUS_NODE:
                label.append("\\nOp: -");
                break;

            // Posição no código (para nós importantes)
            case PROGRAM_NODE:
            case IF_NODE:
            case FOR_CLAUSE_NODE:
            case FOR_COND_NODE:
                if (node.line > 0 || node.column > 0) {
                    label.append("\\nPos: ").append(node.line).append(":").append(node.column);
                }
                break;

            default:
                // Para outros nós, não adiciona dados extras
                break;
        }
        
        // Escapa caracteres especiais para o formato DOT
        return label.toString().replace("\"", "\\\"");
    }
}