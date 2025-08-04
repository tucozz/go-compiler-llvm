package compiler.tables;

import compiler.typing.GoType;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Tabela de funções melhorada para suportar tipos GoType
 */
public class FunctionTable {
    private Map<String, FunctionInfo> functions;
    
    public FunctionTable() {
        this.functions = new HashMap<>();
    }

    /**
     * Adiciona uma função built-in se ela não existir ainda
     */
    public void addBuiltInFunctionIfNeeded(String functionName) {
        if (functions.containsKey(functionName)) {
            return; // Já existe
        }
        
        switch (functionName) {
            case "println":
                List<String> printlnParamNames = new ArrayList<>();
                List<GoType> printlnParamTypes = new ArrayList<>();
                printlnParamNames.add("args");
                printlnParamTypes.add(GoType.STRING);
                addFunction("println", printlnParamNames, printlnParamTypes, GoType.VOID, 0);
                markAsDefined("println");
                break;
                
            case "len":
                List<String> lenParamNames = new ArrayList<>();
                List<GoType> lenParamTypes = new ArrayList<>();
                lenParamNames.add("obj");
                lenParamTypes.add(GoType.STRING);
                addFunction("len", lenParamNames, lenParamTypes, GoType.INT, 0);
                markAsDefined("len");
                break;
                
            default:
                // Função built-in desconhecida - não fazer nada
                break;
        }
    }
    
    /**
     * Adiciona uma função à tabela com tipos GoType
     */
    public boolean addFunction(String name, List<String> parameterNames, List<GoType> parameterTypes, GoType returnType, int declarationLine) {
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
     * Verifica se uma função existe (método alternativo)
     */
    public boolean functionExists(String name) {
        return functions.containsKey(name);
    }
    
    /**
     * Verifica se uma chamada de função é válida
     */
    public boolean isValidCall(String functionName, List<GoType> argumentTypes) {
        FunctionInfo func = functions.get(functionName);
        if (func == null) {
            return false;
        }
        
        return func.isCallCompatible(argumentTypes);
    }
    
    /**
     * Marca uma função como definida
     */
    public boolean markAsDefined(String functionName) {
        FunctionInfo func = functions.get(functionName);
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
    }
    
    /**
     * Imprime a tabela de funções
     */
    public void printTable() {
        System.out.println("\n=== FUNCTION TABLE ===");
        if (functions.isEmpty()) {
            System.out.println("No functions declared.");
        } else {
            System.out.println("Total functions: " + functions.size());
            for (FunctionInfo func : functions.values()) {
                String status = func.isDefined() ? "Defined" : "Declared";
                System.out.println("  Function: " + func.getName() + " | Signature: " + func.getSignature() + 
                                 " | Line: " + func.getDeclarationLine() + " | Status: " + status);
            }
        }
        System.out.println("======================");
    }
}
