package compiler.ast;

import java.util.ArrayList;
import java.util.List;

import compiler.tables.VarTable;
import compiler.typing.GoType;

// Implementação dos nós da AST.
public class AST {

	public final NodeKind kind;

	// Dados de valor literal
	public final int intData;       // inteiros, índices
	public final float floatData;   // números de ponto flutuante
    public final boolean boolData;  // valores booleanos
    public final String text;       // identificadores e strings literais

	// Posição no código fonte
	public final int line;
	public final int column;

	// Tipo estático inicial e tipo inferido pelo checker
	public final GoType type;           // Tipo inicial (pode ser null)
	private GoType annotatedType;       // Tipo inferido pelo semantic checker

	private final List<AST> children; // Privado para que a manipulação da lista seja controlável.

	// Construtor completo para poder tornar todos os campos finais.
	// Privado porque não queremos conflitos entre os diferentes tipos de dados.
	private AST(NodeKind kind, int intData, float floatData, boolean boolData, 
			   String text, GoType type, int line, int column) {
		this.kind = kind;
		this.intData = intData;
		this.floatData = floatData;
		this.boolData = boolData;
		this.text = text;
		this.type = type;
		this.line = line;
		this.column = column;
		this.annotatedType = null;
		this.children = new ArrayList<AST>();
	}

	// Construtores públicos específicos

	// Cria o nó com um dado inteiro.
	public AST(NodeKind kind, int intData, GoType type) {
		this(kind, intData, 0.0f, false, null, type, 0, 0);
	}

	// Cria o nó com um dado float.
	public AST(NodeKind kind, float floatData, GoType type) {
		this(kind, 0, floatData, false, null, type, 0, 0);
	}

	// Cria nó sem dados (para operadores, statements, etc.)
	public AST(NodeKind kind, GoType type) {
		this(kind, 0, 0.0f, false, null, type, 0, 0);
	}

	// Métodos para manipulação de filhos

	// Adiciona um novo filho ao nó.
	public void addChild(AST child) {
		// A lista cresce automaticamente, então nunca vai dar erro ao adicionar.
		this.children.add(child);
	}

	// Retorna o filho no índice passado.
	// Não há nenhuma verificação de erros!
	public AST getChild(int idx) {
		// Claro que um código em produção precisa testar o índice antes para
		// evitar uma exceção.
	    return this.children.get(idx);
	}

	public int getChildCount() {
		return this.children.size();
	}

	public List<AST> getChildren() {
		return this.children;
	}

	public boolean hasChildren() {
		return !this.children.isEmpty();
	}

	// Métodos para tipo anotado
	public GoType getAnnotatedType() {
		return annotatedType;
	}

	public void setAnnotatedType(GoType type) {
		this.annotatedType = type;
	}

	public boolean hasAnnotatedType() {
		return annotatedType != null;
	}

	// Métodos factory estáticos

	// Cria um nó e pendura todos os filhos passados como argumento.
	public static AST newSubtree(NodeKind kind, GoType type, AST... children) {
		AST node = new AST(kind, type);
	    for (AST child: children) {
	    	node.addChild(child);
	    }
	    return node;
	}

	// Factory para identificadores
	public static AST id(String name, int line, int column) {
		return new AST(NodeKind.ID_NODE, 0, 0.0f, false, name, null, line, column);
	}

	// Factory para literais inteiros
	public static AST intLit(int value, int line, int column) {
		return new AST(NodeKind.INT_VAL_NODE, value, 0.0f, false, null, null, line, column);
	}

	// Factory para literais de ponto flutuante
	public static AST realLit(float value, int line, int column) {
		return new AST(NodeKind.REAL_VAL_NODE, 0, value, false, null, null, line, column);
	}

	// Factory para literais booleanos
	public static AST boolLit(boolean value, int line, int column) {
		return new AST(NodeKind.BOOL_VAL_NODE, 0, 0.0f, value, null, null, line, column);
	}

	// Factory para literais de string
	public static AST strLit(String value, int line, int column) {
		return new AST(NodeKind.STR_VAL_NODE, 0, 0.0f, false, value, null, line, column);
	}

	// Factory para operadores binários
	public static AST binaryOp(NodeKind op, AST left, AST right, int line, int column) {
		AST node = new AST(op, 0, 0.0f, false, null, null, line, column);
		node.addChild(left);
		node.addChild(right);
		return node;
	}

	// Factory para operadores unários
	public static AST unaryOp(NodeKind op, AST operand, int line, int column) {
		AST node = new AST(op, 0, 0.0f, false, null, null, line, column);
		node.addChild(operand);
		return node;
	}

	// Factory para chamadas de função
	public static AST call(AST function, List<AST> args, int line, int column) {
		AST node = new AST(NodeKind.CALL_NODE, 0, 0.0f, false, null, null, line, column);
		node.addChild(function);
		if (args != null) {
			for (AST arg : args) {
				node.addChild(arg);
			}
		}
		return node;
	}

	// Factory para acesso a arrays
	public static AST index(AST array, AST idx, int line, int column) {
		AST node = new AST(NodeKind.INDEX_NODE, 0, 0.0f, false, null, null, line, column);
		node.addChild(array);
		node.addChild(idx);
		return node;
	}

	// Factory para assignments
	public static AST assign(AST lvalue, AST rvalue, int line, int column) {
		AST node = new AST(NodeKind.ASSIGN_NODE, 0, 0.0f, false, null, null, line, column);
		node.addChild(lvalue);
		node.addChild(rvalue);
		return node;
	}

	// Variáveis internas usadas para geração da saída em DOT.
	// Estáticas porque só precisamos de uma instância.
	private static int nr;

	// Imprime recursivamente a codificação em DOT da subárvore começando no nó atual.
	// Usa stderr como saída para facilitar o redirecionamento, mas isso é só um hack.
	private int printNodeDot() {
		int myNr = nr++;

	    System.err.printf("node%d[label=\"", myNr);
	    
	    // Imprime tipo se disponível
	    if (this.type != null) {
	    	System.err.printf("(%s) ", this.type.toString());
	    } else if (this.annotatedType != null) {
	    	System.err.printf("(%s) ", this.annotatedType.toString());
	    }
	    
	    // Imprime o tipo do nó
	    System.err.printf("%s", this.kind.toString());
	    
	    // Imprime dados específicos do nó
	    if (NodeKind.hasData(this.kind)) {
	        if (this.kind == NodeKind.REAL_VAL_NODE) {
	        	System.err.printf("%.2f", this.floatData);
	        } else if (this.kind == NodeKind.STR_VAL_NODE && this.text != null) {
	        	System.err.printf("\\\"%s\\\"", this.text);
	        } else if (this.kind == NodeKind.ID_NODE && this.text != null) {
	        	System.err.printf("%s", this.text);
	        } else if (this.kind == NodeKind.BOOL_VAL_NODE) {
	        	System.err.printf("%s", this.boolData ? "true" : "false");
	        } else if (this.kind == NodeKind.INT_VAL_NODE) {
	        	System.err.printf("%d", this.intData);
	        }
	    }
	    
	    System.err.printf("\"];\n");

	    for (int i = 0; i < this.children.size(); i++) {
	        int childNr = this.children.get(i).printNodeDot();
	        System.err.printf("node%d -> node%d;\n", myNr, childNr);
	    }
	    return myNr;
	}

	// Imprime a árvore toda em stderr.
	public static void printDot(AST tree, VarTable table) {
	    if (tree == null) {
	        System.err.println("AST is null - cannot print");
	        return;
	    }
	    nr = 0;
	    System.err.printf("\ndigraph {\ngraph [ordering=\"out\"];\n");
	    tree.printNodeDot();
	    System.err.printf("}\n");
	}
}
