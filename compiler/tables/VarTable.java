package compiler.tables;

import compiler.typing.GoType;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Tabela de variáveis com suporte a escopos aninhados.
 * Gerencia declarações de variáveis, constantes e parâmetros de função.
 */
public class VarTable {
    // Stack de scopes para suportar blocos aninhados
    private Stack<Map<String, VarEntry>> scopes;
    
    public VarTable() {
        this.scopes = new Stack<>();
        // Escopo global
        enterScope();
    }
    
    /**
     * Entra em um novo escopo (função, bloco, etc.)
     */
    public void enterScope() {
        scopes.push(new HashMap<>());
    }
    
    /**
     * Sai do escopo atual
     */
    public void exitScope() {
        if (scopes.size() > 1) { // Mantém pelo menos o escopo global
            scopes.pop();
        }
    }
    
    /**
     * Adiciona uma variável ao escopo atual
     */
    public boolean addVariable(String name, GoType type, int line) {
        Map<String, VarEntry> currentScope = scopes.peek();
        
        if (currentScope.containsKey(name)) {
            return false; // Já declarada no escopo atual
        }
        
        currentScope.put(name, new VarEntry(name, type, line, false));
        return true;
    }
    
    /**
     * Adiciona uma constante ao escopo atual
     */
    public boolean addConstant(String name, GoType type, int line) {
        Map<String, VarEntry> currentScope = scopes.peek();
        
        if (currentScope.containsKey(name)) {
            return false; // Já declarada no escopo atual
        }
        
        currentScope.put(name, new VarEntry(name, type, line, true));
        return true;
    }
    
    /**
     * Busca uma variável em todos os escopos (do mais interno para o externo)
     */
    public VarEntry lookup(String name) {
        // Busca do escopo mais interno para o mais externo
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, VarEntry> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null; // Não encontrada
    }
    
    /**
     * Verifica se uma variável existe no escopo atual
     */
    public boolean existsInCurrentScope(String name) {
        return scopes.peek().containsKey(name);
    }
    
    /**
     * Verifica se uma variável existe em qualquer escopo
     */
    public boolean exists(String name) {
        return lookup(name) != null;
    }
    
    /**
     * Retorna o número de escopos ativos
     */
    public int getScopeDepth() {
        return scopes.size();
    }
    
    /**
     * Classe interna para representar uma entrada da tabela
     */
    public static class VarEntry {
        private String name;
        private GoType type;
        private int declarationLine;
        private boolean isConstant;
        private boolean isArray;
        private int arraySize; // -1 para slices
        
        public VarEntry(String name, GoType type, int declarationLine, boolean isConstant) {
            this.name = name;
            this.type = type;
            this.declarationLine = declarationLine;
            this.isConstant = isConstant;
            this.isArray = false;
            this.arraySize = 0;
        }
        
        // Construtor para arrays
        public VarEntry(String name, GoType type, int declarationLine, boolean isConstant, 
                       boolean isArray, int arraySize) {
            this(name, type, declarationLine, isConstant);
            this.isArray = isArray;
            this.arraySize = arraySize;
        }
        
        // Getters
        public String getName() { return name; }
        public GoType getType() { return type; }
        public int getDeclarationLine() { return declarationLine; }
        public boolean isConstant() { return isConstant; }
        public boolean isArray() { return isArray; }
        public int getArraySize() { return arraySize; }
        
        // Setters para arrays
        public void setAsArray(int size) {
            this.isArray = true;
            this.arraySize = size;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(": ");
            if (isArray) {
                sb.append("[]");
            }
            sb.append(type);
            if (isConstant) {
                sb.append(" (const)");
            }
            sb.append(" [line ").append(declarationLine).append("]");
            return sb.toString();
        }
    }
}
