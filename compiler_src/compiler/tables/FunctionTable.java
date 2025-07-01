package compiler.tables;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Tabela de símbolos específica para funções usando HashMap
 */
public class FunctionTable {
    private Map<String, FunctionInfo> functions;
    
    public FunctionTable() {
        this.functions = new HashMap<>();
        // addBuiltInFunctions(); // Commented out for testing
    }
    
    /**
     * Adiciona funções built-in do Go
     */
    private void addBuiltInFunctions() {
        // println function
        List<String> printlnParamNames = new ArrayList<>();
        List<String> printlnParamTypes = new ArrayList<>();
        printlnParamNames.add("args");
        printlnParamTypes.add("string");
        addFunction("println", printlnParamNames, printlnParamTypes, "void", 0);
        
        // len function
        List<String> lenParamNames = new ArrayList<>();
        List<String> lenParamTypes = new ArrayList<>();
        lenParamNames.add("obj");
        lenParamTypes.add("string");
        addFunction("len", lenParamNames, lenParamTypes, "int", 0);
        
        // Marca como definidas
        markAsDefined("println");
        markAsDefined("len");
    }
    
    /**
     * Adiciona uma função à tabela
     */
    public boolean addFunction(String name, List<String> parameterNames, List<String> parameterTypes, String returnType, int declarationLine) {
        if (functions.containsKey(name)) {
            return false; // Função já existe
        }
        
        FunctionInfo funcInfo = new FunctionInfo(name, parameterNames, parameterTypes, returnType, declarationLine);
        functions.put(name, funcInfo);
        return true;
    }
    
    /**
     * Obtém informações de uma função
     */
    public FunctionInfo getFunction(String name) {
        return functions.get(name);
    }
    
    /**
     * Verifica se uma função existe
     */
    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }
    
    /**
     * Verifica se uma chamada de função é válida
     */
    public boolean isValidCall(String functionName, List<String> argumentTypes) {
        FunctionInfo func = functions.get(functionName);
        if (func == null) {
            return false;
        }
        
        return func.isCallCompatible(argumentTypes);
    }
    
    /**
     * Marca uma função como definida
     */
    public boolean markAsDefined(String name) {
        FunctionInfo func = functions.get(name);
        if (func != null) {
            func.setDefined(true);
            return true;
        }
        return false;
    }
    
    /**
     * Obtém todas as funções
     */
    public List<FunctionInfo> getAllFunctions() {
        return new ArrayList<>(functions.values());
    }
    
    /**
     * Obtém funções não definidas
     */
    public List<FunctionInfo> getUndefinedFunctions() {
        List<FunctionInfo> undefined = new ArrayList<>();
        for (FunctionInfo func : functions.values()) {
            if (!func.isDefined()) {
                undefined.add(func);
            }
        }
        return undefined;
    }
    
    /**
     * Número de funções na tabela
     */
    public int size() {
        return functions.size();
    }
    
    /**
     * Verifica se está vazia
     */
    public boolean isEmpty() {
        return functions.isEmpty();
    }
    
    /**
     * Limpa a tabela
     */
    public void clear() {
        functions.clear();
        addBuiltInFunctions();
    }
    
    /**
     * Imprime a tabela de funções
     */
    public void printTable() {
        System.out.println("\n=== FUNCTION TABLE ===");
        if (functions.isEmpty()) {
            System.out.println("No functions declared.");
            return;
        }
        
        System.out.println("Total functions: " + functions.size());
        
        int defined = 0;
        int builtIn = 0;
        
        for (FunctionInfo func : functions.values()) {
            if (func.isDefined()) defined++;
            if (func.getDeclarationLine() == 0) builtIn++;
            System.out.println("  " + func);
        }
        
        // System.out.println("Statistics: Defined=" + defined + ", Built-in=" + builtIn + ", Declared only=" + (functions.size() - defined));
        System.out.println("======================");
    }
}
