package compiler.ast;

import static compiler.typing.GoType.NO_TYPE;
import java.util.ArrayList;
import java.util.List;
import compiler.tables.StrTable;
import compiler.typing.GoType;

public class AST {
    public final NodeKind kind;
    public final int intData;
    public final float floatData;
    public final String stringData; // Campo para nomes de variáveis/funções
    public final GoType type;
    private final List<AST> children;

    // Construtor principal e privado que inicializa todos os campos.
    private AST(NodeKind kind, int intData, float floatData, String stringData, GoType type) {
        this.kind = kind;
        this.intData = intData;
        this.floatData = floatData;
        this.stringData = stringData;
        this.type = type;
        this.children = new ArrayList<AST>();
    }

    // Construtor para nós com dado inteiro (ex: literais int, bool, ou índices de string)
    public AST(NodeKind kind, int intData, GoType type) {
        this(kind, intData, 0.0f, null, type);
    }

    // Construtor para nós com dado float
    public AST(NodeKind kind, float floatData, GoType type) {
        this(kind, 0, floatData, null, type);
    }

    // Construtor para nós com dado String (nomes de variáveis/funções)
    public AST(NodeKind kind, String stringData, GoType type) {
        this(kind, 0, 0.0f, stringData, type);
    }

    // Construtor para nós sem dados extras (só tipo e filhos)
    public AST(NodeKind kind, GoType type) {
        this(kind, 0, 0.0f, null, type);
    }

    public void addChild(AST child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    public AST getChild(int idx) {
        return this.children.get(idx);
    }
    
    public List<AST> getChildren() {
        return this.children;
    }

    public static AST newSubtree(NodeKind kind, GoType type, AST... children) {
        AST node = new AST(kind, type);
        for (AST child : children) {
            node.addChild(child);
        }
        return node;
    }
    
    // --- Métodos para Visualização com GraphViz (DOT) ---
    private static int nr;
    private static StrTable st;

    private int printNodeDot() {
        int myNr = nr++;

        System.err.printf("node%d[label=\"", myNr);
        if (this.type != NO_TYPE && this.type != null) {
            System.err.printf("(%s) ", this.type.toString());
        }
        
        if (this.kind == NodeKind.VAR_DECL_NODE || this.kind == NodeKind.VAR_USE_NODE || this.kind == NodeKind.FUNC_CALL_NODE) {
            System.err.printf("%s: %s", this.kind.toString(), this.stringData);
        } else {
            System.err.printf("%s", this.kind.toString());
        }

        if (this.kind == NodeKind.INT_VAL_NODE) {
            System.err.printf(" %d", this.intData);
        } else if (this.kind == NodeKind.FLOAT_VAL_NODE) {
            System.err.printf(" %.6f", this.floatData);
        } else if (this.kind == NodeKind.BOOL_VAL_NODE) {
            System.err.printf(" %s", this.intData == 1 ? "true" : "false");
        } else if (this.kind == NodeKind.STR_VAL_NODE) {
            // CORREÇÃO: A linha abaixo foi modificada.
            // A visualização completa do conteúdo da string foi desabilitada
            // porque um método para buscar a string por índice (ex: get(int))
            // não foi encontrado na sua classe StrTable.
            // Agora, apenas o índice da string na tabela será exibido (ex: @0).
            System.err.printf(" @%d", this.intData);
        }
        
        System.err.printf("\"];\n");

        for (AST child : this.children) {
            int childNr = child.printNodeDot();
            System.err.printf("node%d -> node%d;\n", myNr, childNr);
        }
        return myNr;
    }

    public static void printDot(AST tree, StrTable strTable) {
        nr = 0;
        st = strTable;
        System.err.printf("digraph {\ngraph [ordering=\"out\"];\n");
        tree.printNodeDot();
        System.err.printf("}\n");
    }
}
