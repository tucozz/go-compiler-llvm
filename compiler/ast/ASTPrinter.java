package compiler.ast;

import compiler.typing.GoType; // <-- NOVO IMPORT

/**
 * Classe utilitária para gerar uma representação da AST no formato DOT (Graphviz).
 * Isso permite a visualização gráfica da árvore.
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
        sb.append("digraph AST {\n");
        sb.append("  node [shape=box, style=rounded];\n");
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
        label.append(node.kind.toString()); // Ex: VAR_DECL_NODE

        // Adiciona o tipo do nó, se houver e não for 'no_type'
        // MODIFICAÇÃO: Adicionada verificação para não imprimir 'no_type'.
        if (node.type != null && node.type != GoType.NO_TYPE) {
            label.append("\\nType: ").append(node.type.toString());
        }

        // Adiciona dados extras dependendo do tipo do nó
        switch (node.kind) {
            case VAR_USE_NODE:
            case VAR_DECL_NODE:
                label.append("\\nName: ").append(node.stringData);
                break;
            case INT_VAL_NODE:
                label.append("\\nValue: ").append(node.intData);
                break;
            case FLOAT_VAL_NODE:
                label.append("\\nValue: ").append(node.floatData);
                break;
            case STR_VAL_NODE:
                // Para strings, o intData é o índice na StrTable
                label.append("\\nStrTable Idx: ").append(node.intData);
                break;
            case BOOL_VAL_NODE:
                label.append("\\nValue: ").append(node.intData == 1 ? "true" : "false");
                break;
            default:
                // Para outros nós, não adiciona dados extras
                break;
        }
        
        // Escapa caracteres especiais para o formato DOT
        return label.toString().replace("\"", "\\\"");
    }
}
