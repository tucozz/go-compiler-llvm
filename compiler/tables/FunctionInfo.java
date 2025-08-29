package compiler.tables;

import compiler.typing.GoType;
import java.util.List;
import java.util.ArrayList;

/**
 * Classe que representa informações sobre uma função
 */
public class FunctionInfo {
    private String name;
    private List<String> parameterNames;
    private List<GoType> parameterTypes;
    private GoType returnType;
    private int declarationLine;
    private boolean isDefined;

    public FunctionInfo(String name, List<String> parameterNames, List<GoType> parameterTypes, GoType returnType,
            int declarationLine) {
        this.name = name;
        this.parameterNames = new ArrayList<>(parameterNames);
        this.parameterTypes = new ArrayList<>(parameterTypes);
        this.returnType = returnType;
        this.declarationLine = declarationLine;
        this.isDefined = false;
    }

    // Construtor alternativo que aceita strings para os tipos (para
    // compatibilidade)
    public FunctionInfo(String name, List<String> parameterNames, List<String> parameterTypesStr, String returnTypeStr,
            int declarationLine) {
        this.name = name;
        this.parameterNames = new ArrayList<>(parameterNames);
        this.parameterTypes = new ArrayList<>();

        // Converter strings para GoType
        for (String typeStr : parameterTypesStr) {
            this.parameterTypes.add(GoType.fromString(typeStr));
        }

        this.returnType = GoType.fromString(returnTypeStr);
        this.declarationLine = declarationLine;
        this.isDefined = false;
    }

    // Getters
    public String getName() {
        return name;
    }

    public List<String> getParameterNames() {
        return new ArrayList<>(parameterNames);
    }

    public List<GoType> getParameterTypes() {
        return new ArrayList<>(parameterTypes);
    }

    public GoType getReturnType() {
        return returnType;
    }

    public int getDeclarationLine() {
        return declarationLine;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public int getParameterCount() {
        return parameterNames.size();
    }

    public String getReturnTypeAsString() {
        return returnType.toString();
    }

    // Setters
    public void setDefined(boolean defined) {
        this.isDefined = defined;
    }

    /**
     * Verifica se uma chamada de função é compatível com os tipos de argumentos fornecidos
     */
    public boolean isCallCompatible(List<GoType> argumentTypes) {
        // Verificar se o número de argumentos está correto
        if (argumentTypes.size() != parameterTypes.size()) {
            return false;
        }
        
        // Verificar se cada tipo de argumento é compatível com o tipo do parâmetro
        for (int i = 0; i < argumentTypes.size(); i++) {
            GoType paramType = parameterTypes.get(i);
            GoType argType = argumentTypes.get(i);
            
            // Se algum tipo é UNKNOWN, consideramos compatível (pode ser um erro anterior)
            if (paramType == GoType.UNKNOWN || argType == GoType.UNKNOWN) {
                continue;
            }
            
            // Verificar compatibilidade exata por enquanto (pode ser estendido para conversões implícitas)
            if (!paramType.equals(argType)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Verifica se uma chamada de função é compatível (versão com GoType)
     */
    public boolean isCallCompatibleWithTypes(List<GoType> argumentTypes) {
        if (parameterTypes.size() != argumentTypes.size()) {
            return false;
        }

        for (int i = 0; i < parameterTypes.size(); i++) {
            GoType paramType = parameterTypes.get(i);
            GoType argType = argumentTypes.get(i);

            if (!paramType.isCompatibleWith(argType)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Cria assinatura da função
     */
    public String getSignature() {
        StringBuilder sig = new StringBuilder();
        sig.append(name).append("(");

        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0)
                sig.append(", ");
            sig.append(parameterNames.get(i)).append(" ").append(parameterTypes.get(i).toString());
        }

        sig.append(")");

        if (returnType != null && returnType != GoType.VOID) {
            sig.append(" ").append(returnType.toString());
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