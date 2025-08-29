package compiler.tables;

import compiler.typing.GoType;

/**
 * Classe que representa informações de um array na tabela de símbolos
 */
public class ArrayInfo {
    private String name;
    private GoType elementType;
    private int size;
    private int declarationLine;
    private boolean isDynamicSize; // Para arrays sem tamanho fixo []int
    
    public ArrayInfo(String name, GoType elementType, int size, int declarationLine) {
        this.name = name;
        this.elementType = elementType;
        this.size = size;
        this.declarationLine = declarationLine;
        this.isDynamicSize = (size == -1); // -1 indica tamanho dinâmico
    }
    
    // Construtor para arrays dinâmicos (sem tamanho fixo)
    public ArrayInfo(String name, GoType elementType, int declarationLine) {
        this(name, elementType, -1, declarationLine);
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public GoType getElementType() {
        return elementType;
    }
    
    public int getSize() {
        return size;
    }
    
    public int getDeclarationLine() {
        return declarationLine;
    }
    
    public boolean isDynamicSize() {
        return isDynamicSize;
    }
    
    public GoType getArrayType() {
        // Retorna o tipo do array baseado no tipo do elemento
        switch (elementType) {
            case INT: return GoType.ARRAY_INT;
            case FLOAT64: return GoType.ARRAY_FLOAT64;
            case FLOAT32: return GoType.ARRAY_FLOAT32;
            case STRING: return GoType.ARRAY_STRING;
            case BOOL: return GoType.ARRAY_BOOL;
            default: return GoType.UNKNOWN;
        }
    }
    
    // Setters
    public void setSize(int size) {
        this.size = size;
        this.isDynamicSize = (size == -1);
    }
    
    /**
     * Verifica se um índice é válido para este array
     */
    public boolean isValidIndex(int index) {
        if (isDynamicSize) {
            return index >= 0; // Arrays dinâmicos aceitam qualquer índice não-negativo
        }
        return index >= 0 && index < size;
    }
    
    /**
     * Retorna uma representação em string do array
     */
    public String getSignature() {
        if (isDynamicSize) {
            return "[]" + elementType.getTypeName();
        } else {
            return "[" + size + "]" + elementType.getTypeName();
        }
    }
    
    @Override
    public String toString() {
        return "Array " + name + ": " + getSignature() + " (line " + declarationLine + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ArrayInfo arrayInfo = (ArrayInfo) obj;
        return name.equals(arrayInfo.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
