package compiler.ast;

public enum NodeKind {
    // Programa e blocos
    PROGRAM_NODE,
    BLOCK_NODE,

    // Declarações
    VAR_DECL_NODE,
    CONST_DECL_NODE,
    SHORT_VAR_DECL_NODE,
    FUNC_DECL_NODE,
    PARAM_LIST_NODE,
    PARAM_NODE,
    RESULT_NODE,

    // Statements
    ASSIGN_NODE,
    RETURN_NODE,
    BREAK_NODE,
    CONTINUE_NODE,
    IF_NODE,
    FOR_CLAUSE_NODE, // for init; cond; post
    FOR_COND_NODE, // for cond

    // Expressões primárias
    ID_NODE,
    INT_VAL_NODE,
    REAL_VAL_NODE,
    STR_VAL_NODE,
    BOOL_VAL_NODE,
    CALL_NODE,
    INDEX_NODE, // a[i]
    COMPOSITE_LITERAL_NODE, // []T{...}
    TYPE_CONV_NODE, // T(x)

    UNARY_PLUS_NODE,
    UNARY_MINUS_NODE,
    NOT_NODE,

    // Operadores binários aritméticos
    PLUS_NODE,
    MINUS_NODE,
    TIMES_NODE,
    OVER_NODE,
    MOD_NODE, // % (MOD)

    // Operadores relacionais
    EQUAL_NODE, // == (EQUALS)
    NOT_EQUAL_NODE, // != (NOTEQUAL)
    LESS_NODE, // < (LTHAN)
    GREATER_NODE, // > (GTHAN)
    LESS_EQ_NODE, // <= (LETHAN)
    GREATER_EQ_NODE, // >= (GETHAN)

    // Operadores lógicos
    AND_NODE, // && (AND)
    OR_NODE, // || (OR)

    // Statements adicionais que existem na gramática:
    EXPR_STMT_NODE, // expr statementEnd → #ExpressionSimpleStmt
    INC_DEC_STMT_NODE, // lvalue (INC|DEC) → #IncDecOperationStatement

    // Especificações (detalhamento):
    CONST_SPEC_NODE, // constSpec → #ConstSpecification
    VAR_SPEC_NODE, // varSpec → #VarSpecification
    IDENTIFIER_LIST_NODE, // identifierList → #IdentifierListRule
    EXPR_LIST_NODE; // expressionList → #ExprList

    public String toString() {
        switch(this) {
            // Statements e declarações
            case ASSIGN_NODE:           return "=";
            case SHORT_VAR_DECL_NODE:   return ":=";
            case BLOCK_NODE:            return "block";
            case IF_NODE:               return "if";
            case FOR_CLAUSE_NODE:       return "for";
            case FOR_COND_NODE:         return "for_cond";
            case RETURN_NODE:           return "return";
            case BREAK_NODE:            return "break";
            case CONTINUE_NODE:         return "continue";
            case VAR_DECL_NODE:         return "var_decl";
            case CONST_DECL_NODE:       return "const_decl";
            case FUNC_DECL_NODE:        return "func_decl";
            case PROGRAM_NODE:          return "program";
            
            // Literais (retornam string vazia pois o valor está nos dados)
            case BOOL_VAL_NODE:         return "";
            case INT_VAL_NODE:          return "";
            case REAL_VAL_NODE:         return "";
            case STR_VAL_NODE:          return "";
            case ID_NODE:               return "";
            
            // Operadores binários aritméticos
            case PLUS_NODE:             return "+";
            case MINUS_NODE:            return "-";
            case TIMES_NODE:            return "*";
            case OVER_NODE:             return "/";
            case MOD_NODE:              return "%";
            
            // Operadores relacionais
            case EQUAL_NODE:            return "==";
            case NOT_EQUAL_NODE:        return "!=";
            case LESS_NODE:             return "<";
            case GREATER_NODE:          return ">";
            case LESS_EQ_NODE:          return "<=";
            case GREATER_EQ_NODE:       return ">=";
            
            // Operadores lógicos
            case AND_NODE:              return "&&";
            case OR_NODE:               return "||";
            
            // Operadores unários
            case UNARY_PLUS_NODE:       return "+";
            case UNARY_MINUS_NODE:      return "-";
            case NOT_NODE:              return "!";
            
            // Expressões complexas
            case CALL_NODE:             return "call";
            case INDEX_NODE:            return "index";
            case COMPOSITE_LITERAL_NODE: return "composite_lit";
            case TYPE_CONV_NODE:        return "type_conv";
            
            // Especificações e listas
            case PARAM_LIST_NODE:       return "param_list";
            case PARAM_NODE:            return "param";
            case RESULT_NODE:           return "result";
            case EXPR_STMT_NODE:        return "expr_stmt";
            case INC_DEC_STMT_NODE:     return "inc_dec";
            case CONST_SPEC_NODE:       return "const_spec";
            case VAR_SPEC_NODE:         return "var_spec";
            case IDENTIFIER_LIST_NODE:  return "id_list";
            case EXPR_LIST_NODE:        return "expr_list";
            
            default:
                System.err.println("ERROR: Fall through in NodeKind enumeration!");
                System.exit(1);
                return ""; // Never reached.
        }
    }

    public static boolean hasData(NodeKind kind) {
        switch (kind) {
            // Nós que carregam dados literais
            case BOOL_VAL_NODE:
            case INT_VAL_NODE:
            case REAL_VAL_NODE:
            case STR_VAL_NODE:
            case ID_NODE:
            // Nós de declaração que podem carregar índices/referências
            case VAR_DECL_NODE:
            case CONST_DECL_NODE:
            case FUNC_DECL_NODE:
                return true;
            default:
                return false;
        }
    }
}
