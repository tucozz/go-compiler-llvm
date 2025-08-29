package compiler.tables;

import compiler.typing.GoType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tabela de símbolos para arrays
 * Gerencia informações sobre declarações e uso de arrays
 */
public class ArrayTable {
    private Map<String, ArrayInfo> arrays;
    
    public ArrayTable() {
        this.arrays = new HashMap<>();
    }
    
    /**
     * Adiciona um array à tabela
     * @param name Nome do array
     * @param elementType Tipo dos elementos do array
     * @param size Tamanho do array (-1 para tamanho dinâmico)
     * @param declarationLine Linha onde foi declarado
     * @return true se adicionado com sucesso, false se já existe
     */
    public boolean addArray(String name, GoType elementType, int size, int declarationLine) {
        if (arrays.containsKey(name)) {
            return false; // Array já existe
        }
        
        ArrayInfo arrayInfo = new ArrayInfo(name, elementType, size, declarationLine);
        arrays.put(name, arrayInfo);
        return true;
    }
    
    /**
     * Adiciona um array dinâmico (sem tamanho fixo)
     */
    public boolean addDynamicArray(String name, GoType elementType, int declarationLine) {
        return addArray(name, elementType, -1, declarationLine);
    }
    
    /**
     * Verifica se um array existe na tabela
     */
    public boolean hasArray(String name) {
        return arrays.containsKey(name);
    }
    
    /**
     * Obtém informações de um array
     */
    public ArrayInfo getArray(String name) {
        return arrays.get(name);
    }
    
    /**
     * Remove um array da tabela
     */
    public boolean removeArray(String name) {
        return arrays.remove(name) != null;
    }
    
    /**
     * Verifica se um índice é válido para um array específico
     */
    public boolean isValidArrayAccess(String arrayName, int index) {
        ArrayInfo arrayInfo = arrays.get(arrayName);
        if (arrayInfo == null) {
            return false; // Array não existe
        }
        return arrayInfo.isValidIndex(index);
    }
    
    /**
     * Obtém o tipo do elemento de um array
     */
    public GoType getElementType(String arrayName) {
        ArrayInfo arrayInfo = arrays.get(arrayName);
        if (arrayInfo == null) {
            return GoType.UNKNOWN;
        }
        return arrayInfo.getElementType();
    }
    
    /**
     * Obtém o tipo do array completo
     */
    public GoType getArrayType(String arrayName) {
        ArrayInfo arrayInfo = arrays.get(arrayName);
        if (arrayInfo == null) {
            return GoType.UNKNOWN;
        }
        return arrayInfo.getArrayType();
    }
    
    /**
     * Verifica se um array é dinâmico (sem tamanho fixo)
     */
    public boolean isDynamicArray(String arrayName) {
        ArrayInfo arrayInfo = arrays.get(arrayName);
        if (arrayInfo == null) {
            return false;
        }
        return arrayInfo.isDynamicSize();
    }
    
    /**
     * Obtém o tamanho de um array (retorna -1 se dinâmico)
     */
    public int getArraySize(String arrayName) {
        ArrayInfo arrayInfo = arrays.get(arrayName);
        if (arrayInfo == null) {
            return -1;
        }
        return arrayInfo.getSize();
    }
    
    /**
     * Obtém todos os nomes de arrays
     */
    public Set<String> getArrayNames() {
        return arrays.keySet();
    }
    
    /**
     * Limpa a tabela
     */
    public void clear() {
        arrays.clear();
    }
    
    /**
     * Retorna o número de arrays na tabela
     */
    public int size() {
        return arrays.size();
    }
    
    /**
     * Verifica se a tabela está vazia
     */
    public boolean isEmpty() {
        return arrays.isEmpty();
    }
    
    /**
     * Imprime a tabela de arrays
     */
    public void printTable() {
        System.out.println("\n=== ARRAY TABLE ===");
        if (arrays.isEmpty()) {
            System.out.println("No arrays declared.");
        } else {
            System.out.println("Total arrays: " + arrays.size());
            for (ArrayInfo arrayInfo : arrays.values()) {
                System.out.println("  " + arrayInfo);
            }
        }
        System.out.println("===================");
    }
    
    /**
     * Analisa uma declaração de tipo de array e extrai informações
     * @param typeString String representando o tipo (ex: "[]int", "[10]string")
     * @return ArrayInfo parcial com tipo e tamanho, ou null se inválido
     */
    public static ArrayInfo parseArrayType(String typeString) {
        if (typeString == null || !typeString.startsWith("[")) {
            return null; // Não é um array
        }
        
        int closeBracket = typeString.indexOf("]");
        if (closeBracket == -1) {
            return null; // Sintaxe inválida
        }
        
        String sizeStr = typeString.substring(1, closeBracket);
        String elementTypeStr = typeString.substring(closeBracket + 1);
        
        GoType elementType = GoType.fromString(elementTypeStr);
        if (elementType == null) {
            elementType = GoType.UNKNOWN;
        }
        
        int size = -1; // Dinâmico por padrão
        if (!sizeStr.isEmpty()) {
            try {
                size = Integer.parseInt(sizeStr);
            } catch (NumberFormatException e) {
                return null; // Tamanho inválido
            }
        }
        
        return new ArrayInfo("", elementType, size, 0);
    }
    
    /**
     * Verifica se uma string representa um tipo de array
     */
    public static boolean isArrayType(String typeString) {
        return parseArrayType(typeString) != null;
    }
    
    /**
     * Extrai o tipo do elemento de uma string de tipo de array
     */
    public static GoType extractElementType(String arrayTypeString) {
        ArrayInfo info = parseArrayType(arrayTypeString);
        return info != null ? info.getElementType() : GoType.UNKNOWN;
    }
}
