package compiler.ast;

// Enumeração dos tipos de nós de uma AST para a linguagem Go.
public enum NodeKind {
    // Nós de Programa e Bloco
    PROGRAM_NODE,
    BLOCK_NODE,

    // Nós de Declaração
    CONST_DECL_NODE,
    VAR_DECL_NODE,
    VAR_LIST_NODE,
    FUNC_DECL_NODE,
    
    // Nós de Statement
    ASSIGN_NODE,
    SHORT_ASSIGN_NODE, // para o :=
    IF_NODE,
    FOR_NODE,
    RETURN_NODE,
    BREAK_NODE,
    CONTINUE_NODE,
    INC_DEC_NODE, // para i++ e i--

    // Nós de Expressão Binária
    PLUS_NODE,
    MINUS_NODE,
    TIMES_NODE,
    OVER_NODE,
    MOD_NODE,
    AND_NODE,
    OR_NODE,

    // Nós de Comparação
    EQ_NODE,   // ==
    NEQ_NODE,  // !=
    LT_NODE,   // <
    GT_NODE,   // >
    LEQ_NODE,  // <=
    GEQ_NODE,  // >=

    // Nós de Expressão Unária
    UNARY_PLUS_NODE,
    UNARY_MINUS_NODE,
    NOT_NODE,

    // Nós de Acesso e Chamada
    VAR_USE_NODE,
    FUNC_CALL_NODE,
    ARRAY_ACCESS_NODE,

    // Nós Folha (Literais)
    INT_VAL_NODE,
    FLOAT_VAL_NODE,
    BOOL_VAL_NODE,
    STR_VAL_NODE; // <-- PONTO E VÍRGULA NECESSÁRIO AQUI!

    // A representação em String para a visualização DOT
    @Override
    public String toString() {
        switch(this) {
            case ASSIGN_NODE:       return "=";
            case SHORT_ASSIGN_NODE: return ":=";
            case EQ_NODE:           return "==";
            case NEQ_NODE:          return "!=";
            case LT_NODE:           return "<";
            case GT_NODE:           return ">";
            case LEQ_NODE:          return "<=";
            case GEQ_NODE:          return ">=";
            case BLOCK_NODE:        return "block";
            case BOOL_VAL_NODE:     return "";
            case IF_NODE:           return "if";
            case FOR_NODE:          return "for";
            case INT_VAL_NODE:      return "";
            case FLOAT_VAL_NODE:    return "";
            case STR_VAL_NODE:      return "";
            case MINUS_NODE:        return "-";
            case OVER_NODE:         return "/";
            case MOD_NODE:          return "%";
            case PLUS_NODE:         return "+";
            case PROGRAM_NODE:      return "program";
            case RETURN_NODE:       return "return";
            case TIMES_NODE:        return "*";
            case VAR_DECL_NODE:     return "var_decl";
            case VAR_LIST_NODE:     return "var_list";
            case VAR_USE_NODE:      return "var_use";
            case FUNC_CALL_NODE:    return "call";
            case FUNC_DECL_NODE:    return "func";
            case ARRAY_ACCESS_NODE: return "[]";
            case AND_NODE:          return "&&";
            case OR_NODE:           return "||";
            case NOT_NODE:          return "!";
            case INC_DEC_NODE:      return "++/--";
            case UNARY_MINUS_NODE:  return "u-";
            case UNARY_PLUS_NODE:   return "u+";
            case BREAK_NODE:        return "break";
            case CONTINUE_NODE:     return "continue";
            default:
                throw new IllegalStateException("Unhandled NodeKind in toString(): " + this);
        }
    }
}
