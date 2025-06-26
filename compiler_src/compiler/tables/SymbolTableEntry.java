package compiler.tables;

public class SymbolTableEntry {
    private String name;
    private String type; // Ex: "int", "string", "bool", "[]int", "struct Point"
    private int declarationLine;
    // Futuramente, você pode adicionar 'scope' ou 'isFunction'

    public SymbolTableEntry(String name, String type, int declarationLine) {
        this.name = name;
        this.type = type;
        this.declarationLine = declarationLine;
    }

    // --- Getters ---
    public String getName() { return name; }
    public String getType() { return type; }
    public int getDeclarationLine() { return declarationLine; }

    @Override
    public String toString() {
        return "Name: " + name + ", Type: " + type + ", Declared at line: " + declarationLine;
    }
}