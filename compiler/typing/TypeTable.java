package compiler.typing;

import java.util.HashMap;
import java.util.Map;

/**
 * Tabela para gerenciar tipos de variáveis e realizar inferência de tipos
 */
public class TypeTable {

    // Mapa que associa nome da variável ao seu tipo
    private Map<String, GoType> variableTypes;

    public TypeTable() {
        this.variableTypes = new HashMap<>();
    }

    /**
     * Adiciona uma variável com seu tipo à tabela
     */
    public void addVariable(String varName, GoType type) {
        variableTypes.put(varName, type);
    }

    /**
     * Adiciona uma variável com tipo como string
     */
    public void addVariable(String varName, String typeStr) {
        GoType type = GoType.fromString(typeStr);
        variableTypes.put(varName, type);
    }

    /**
     * Obtém o tipo de uma variável
     */
    public GoType getVariableType(String varName) {
        return variableTypes.getOrDefault(varName, GoType.UNKNOWN);
    }

    /**
     * Verifica se uma variável existe na tabela
     */
    public boolean hasVariable(String varName) {
        return variableTypes.containsKey(varName);
    }

    /**
     * Infere o tipo de um literal
     */
    public GoType inferLiteralType(String literal) {
        if (literal == null)
            return GoType.UNKNOWN;

        // Inferir tipo de literal string
        if (literal.startsWith("\"") && literal.endsWith("\"")) {
            return GoType.STRING;
        }

        // Inferir tipo de literal booleano
        if (literal.equals("true") || literal.equals("false")) {
            return GoType.BOOL;
        }

        // Inferir tipo de literal numérico
        try {
            if (literal.contains(".")) {
                Double.parseDouble(literal);
                return GoType.FLOAT64;
            } else {
                Integer.parseInt(literal);
                return GoType.INT;
            }
        } catch (NumberFormatException e) {
            return GoType.UNKNOWN;
        }
    }

    /**
     * Determina o tipo resultante de uma operação binária
     */
    public GoType getBinaryOperationResultType(GoType leftType, GoType rightType, String operator) {
        // Se algum dos tipos é desconhecido, retorna desconhecido
        if (leftType == GoType.UNKNOWN || rightType == GoType.UNKNOWN) {
            return GoType.UNKNOWN;
        }

        // Operações de comparação sempre retornam bool
        if (operator.equals("==") || operator.equals("!=") ||
                operator.equals("<") || operator.equals(">") ||
                operator.equals("<=") || operator.equals(">=")) {
            return GoType.BOOL;
        }

        // Operações lógicas (&&, ||) sempre retornam bool
        if (operator.equals("&&") || operator.equals("||")) {
            return GoType.BOOL;
        }

        // Operações aritméticas
        if (operator.equals("+") || operator.equals("-") ||
                operator.equals("*") || operator.equals("/") || operator.equals("%")) {

            // Se ambos são do mesmo tipo, retorna esse tipo
            if (leftType == rightType) {
                return leftType;
            }

            // Promoção de tipos: float tem precedência sobre int
            if (leftType.isFloat() || rightType.isFloat()) {
                return GoType.FLOAT64;
            }

            // Se ambos são inteiros, retorna int
            if (leftType.isInteger() && rightType.isInteger()) {
                return GoType.INT;
            }
        }

        return GoType.UNKNOWN;
    }

    /**
     * Verifica se dois tipos são compatíveis para atribuição
     */
    public boolean isAssignmentCompatible(GoType targetType, GoType sourceType) {
        if (targetType == sourceType)
            return true;

        // AUTO pode receber qualquer tipo
        if (targetType == GoType.AUTO)
            return true;

        // Compatibilidade entre tipos numéricos
        if (targetType.isNumeric() && sourceType.isNumeric()) {
            return true;
        }

        return false;
    }

    /**
     * Resolve o tipo AUTO baseado no tipo da expressão atribuída
     */
    public GoType resolveAutoType(GoType sourceType) {
        if (sourceType == GoType.AUTO || sourceType == GoType.UNKNOWN) {
            return GoType.INT; // Tipo padrão
        }
        return sourceType;
    }

    /**
     * Atualiza o tipo de uma variável (útil para inferência de tipo)
     */
    public void updateVariableType(String varName, GoType newType) {
        if (variableTypes.containsKey(varName)) {
            GoType currentType = variableTypes.get(varName);

            // Se o tipo atual é AUTO, resolve para o novo tipo
            if (currentType == GoType.AUTO) {
                variableTypes.put(varName, resolveAutoType(newType));
            }
        }
    }

    /**
     * Limpa a tabela
     */
    public void clear() {
        variableTypes.clear();
    }

    /**
     * Retorna o número de variáveis na tabela
     */
    public int size() {
        return variableTypes.size();
    }

    /**
     * Verifica se a tabela está vazia
     */
    public boolean isEmpty() {
        return variableTypes.isEmpty();
    }

    /**
     * Imprime a tabela de tipos para debug
     */
    public void printTable() {
        System.out.println("\n=== TYPE TABLE ===");
        if (variableTypes.isEmpty()) {
            System.out.println("No variables with types.");
            return;
        }

        for (Map.Entry<String, GoType> entry : variableTypes.entrySet()) {
            System.out.println("  " + entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("==================");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TypeTable{");
        for (Map.Entry<String, GoType> entry : variableTypes.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(", ");
        }
        if (!variableTypes.isEmpty()) {
            sb.setLength(sb.length() - 2); // Remove última vírgula
        }
        sb.append("}");
        return sb.toString();
    }
}
