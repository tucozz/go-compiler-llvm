package compiler.tables;

import java.util.List;
import java.util.ArrayList;

/**
 * Classe que representa informações sobre uma função
 */
public class FunctionInfo {
    private String name;
    private List<String> parameterNames;
    private List<String> parameterTypes;
    private String returnType;
    private int declarationLine;
    private boolean isDefined;
    
    public FunctionInfo(String name, List<String> parameterNames, List<String> parameterTypes, String returnType, int declarationLine) {
        this.name = name;
        this.parameterNames = new ArrayList<>(parameterNames);
        this.parameterTypes = new ArrayList<>(parameterTypes);
        this.returnType = returnType;
        this.declarationLine = declarationLine;
        this.isDefined = false;
    }
    
    // Getters
    public String getName() { return name; }
    public List<String> getParameterNames() { return new ArrayList<>(parameterNames); }
    public List<String> getParameterTypes() { return new ArrayList<>(parameterTypes); }
    public String getReturnType() { return returnType; }
    public int getDeclarationLine() { return declarationLine; }
    public boolean isDefined() { return isDefined; }
    public int getParameterCount() { return parameterNames.size(); }
    
    // Setters
    public void setDefined(boolean defined) { this.isDefined = defined; }
    
    /**
     * Verifica se uma chamada de função é compatível
     */
    public boolean isCallCompatible(List<String> argumentTypes) {
        if (parameterTypes.size() != argumentTypes.size()) {
            return false;
        }
        
        for (int i = 0; i < parameterTypes.size(); i++) {
            String paramType = parameterTypes.get(i);
            String argType = argumentTypes.get(i);
            
            if (!isTypeCompatible(paramType, argType)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Verifica compatibilidade entre tipos
     */
    private boolean isTypeCompatible(String paramType, String argType) {
        if (paramType.equals(argType)) {
            return true;
        }
        
        // Compatibilidade entre tipos inteiros
        if (isIntegerType(paramType) && isIntegerType(argType)) {
            return true;
        }
        
        // Compatibilidade entre tipos float
        if (isFloatType(paramType) && isFloatType(argType)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isIntegerType(String type) {
        return type.equals("int") || type.equals("int8") || type.equals("int16") || 
               type.equals("int32") || type.equals("int64") || type.equals("uint") ||
               type.equals("uint8") || type.equals("uint16") || type.equals("uint32") ||
               type.equals("uint64");
    }
    
    private boolean isFloatType(String type) {
        return type.equals("float32") || type.equals("float64");
    }
    
    /**
     * Cria assinatura da função
     */
    public String getSignature() {
        StringBuilder sig = new StringBuilder();
        sig.append(name).append("(");
        
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) sig.append(", ");
            sig.append(parameterNames.get(i)).append(" ").append(parameterTypes.get(i));
        }
        
        sig.append(")");
        
        if (returnType != null && !returnType.equals("void")) {
            sig.append(" ").append(returnType);
        }
        
        return sig.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Function: ").append(name);
        sb.append(" | Signature: ").append(getSignature());
        sb.append(" | Line: ").append(declarationLine);
        sb.append(" | Status: ").append(isDefined ? "Defined" : "Declared");
        return sb.toString();
    }
}
