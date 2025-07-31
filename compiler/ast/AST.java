package ast;

import compiler.typing.GoType; // Use your GoType enum
import compiler.tables.SymbolTable; // We will use your SymbolTable
import compiler.tables.SymbolTableEntry; // To get symbol info

import java.util.ArrayList;
import java.util.List;

// Implementation of AST nodes.
public class AST {

	public final NodeKind kind;
	public final int intData;
	public final float floatData;
	public final GoType type; // Change Type to GoType
	private final List<AST> children;

	// Private constructor to ensure 'data' fields are not filled simultaneously.
	private AST(NodeKind kind, int intData, float floatData, GoType type) { // Change Type to GoType
		this.kind = kind;
		this.intData = intData;
		this.floatData = floatData;
		this.type = type;
		this.children = new ArrayList<>();
	}

	// Create node with an integer data.
	public AST(NodeKind kind, int intData, GoType type) { // Change Type to GoType
		this(kind, intData, 0.0f, type);
	}

	// Create node with a float data.
	public AST(NodeKind kind, float floatData, GoType type) { // Change Type to GoType
		this(kind, 0, floatData, type);
	}

    // Create node without explicit data (like BLOCK_NODE, IF_NODE)
    public AST(NodeKind kind, GoType type) { // Change Type to GoType
        this(kind, 0, 0.0f, type);
    }

	// Add a new child to the node.
	public void addChild(AST child) {
		this.children.add(child);
	}

	// Returns the child at the given index.
	public AST getChild(int idx) {
	    return this.children.get(idx);
	}

    // Returns the list of children (read-only)
    public List<AST> getChildren() {
        return new ArrayList<>(this.children); // Return a copy to prevent external modification
    }

	// Creates a node and appends all children passed as argument.
	public static AST newSubtree(NodeKind kind, GoType type, AST... children) { // Change Type to GoType
		AST node = new AST(kind, type); // Use the new constructor for no-data
	    for (AST child: children) {
	    	node.addChild(child);
	    }
	    return node;
	}

    // Helper for ASTs without children
    public static AST newLeaf(NodeKind kind, GoType type) { // Change Type to GoType
        return new AST(kind, type);
    }
    public static AST newLeaf(NodeKind kind, int intData, GoType type) { // Change Type to GoType
        return new AST(kind, intData, type);
    }
    public static AST newLeaf(NodeKind kind, float floatData, GoType type) { // Change Type to GoType
        return new AST(kind, floatData, type);
    }

	// Internal variables used for DOT graph generation.
	// We'll need a SymbolTable reference, not VarTable.
	private static int nr;
	private static SymbolTable st; // Change VarTable to SymbolTable

	// Recursively prints the DOT encoding of the subtree starting at the current node.
	// Uses stderr as output for easy redirection.
	private int printNodeDot() {
		int myNr = nr++;

	    System.err.printf("node%d[label=\"", myNr);
	    if (this.type != GoType.NO_TYPE) { // Change NO_TYPE to GoType.NO_TYPE
	    	System.err.printf("(%s) ", this.type.toString());
	    }
	    // Adapt VAR_DECL_NODE and VAR_USE_NODE to use SymbolTable
	    if (this.kind == NodeKind.VAR_DECL_NODE || this.kind == NodeKind.VAR_USE_NODE) {
	    	// This assumes intData stores the index/ID from SymbolTable (if applicable)
            // Or you might need to store the variable name directly if your SymbolTable doesn't use simple indices
            // For now, let's assume intData maps to something in SymbolTable
            SymbolTableEntry entry = st.getEntryByIndex(this.intData); // Assuming a getEntryByIndex in SymbolTable
            if (entry != null) {
	    	    System.err.printf("%s@%d", entry.getName(), this.intData);
            } else {
                System.err.printf("%s@%d (Unknown)", this.kind.toString(), this.intData);
            }
	    } else {
	    	System.err.printf("%s", this.kind.toString());
	    }
	    if (NodeKind.hasData(this.kind)) {
	        if (this.kind == NodeKind.REAL_VAL_NODE) {
	        	System.err.printf("%.2f", this.floatData);
	        } else if (this.kind == NodeKind.STR_VAL_NODE) {
	        	System.err.printf("@%d", this.intData); // Index in StringTable
	        } else if (this.kind == NodeKind.INT_VAL_NODE || this.kind == NodeKind.BOOL_VAL_NODE) { // Include BOOL_VAL_NODE
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

	// Prints the entire tree to stderr.
	public static void printDot(AST tree, SymbolTable table) { // Change VarTable to SymbolTable
	    nr = 0;
	    st = table; // Assign SymbolTable
	    System.err.printf("digraph {\ngraph [ordering=\"out\"];\n");
	    tree.printNodeDot();
	    System.err.printf("}\n");
	}
}