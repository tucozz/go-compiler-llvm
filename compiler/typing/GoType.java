package compiler.typing;

/**
 * Enum que representa os tipos básicos da linguagem Go
 */
public enum GoType {
    // Tipos inteiros
    INT("int"),
    INT8("int8"),
    INT16("int16"),
    INT32("int32"),
    INT64("int64"),

    // Tipos inteiros sem sinal
    UINT("uint"),
    UINT8("uint8"),
    UINT16("uint16"),
    UINT32("uint32"),
    UINT64("uint64"),

    // Tipos de ponto flutuante
    FLOAT32("float32"),
    FLOAT64("float64"),

    // Outros tipos básicos
    BOOL("bool"),
    STRING("string"),

    // Tipos especiais
    VOID("void"),
    UNKNOWN("unknown"),
    AUTO("auto"), // Para inferência de tipo
    NO_TYPE("no_type"), // Para operações inválidas
    
    // Tipos de array (tipos compostos)
    ARRAY_INT("[]int"),
    ARRAY_STRING("[]string"),
    ARRAY_BOOL("[]bool"),
    ARRAY_FLOAT32("[]float32"),
    ARRAY_FLOAT64("[]float64");

    private final String typeName;

    GoType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    /**
     * Converte uma string para o tipo GoType correspondente
     */
    public static GoType fromString(String typeStr) {
        if (typeStr == null)
            return UNKNOWN;

        for (GoType type : GoType.values()) {
            if (type.typeName.equals(typeStr)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * Verifica se o tipo é numérico
     */
    public boolean isNumeric() {
        return isInteger() || isFloat();
    }

    /**
     * Verifica se o tipo é inteiro
     */
    public boolean isInteger() {
        return this == INT || this == INT8 || this == INT16 || this == INT32 || this == INT64 ||
                this == UINT || this == UINT8 || this == UINT16 || this == UINT32 || this == UINT64;
    }

    /**
     * Verifica se o tipo é ponto flutuante
     */
    public boolean isFloat() {
        return this == FLOAT32 || this == FLOAT64;
    }

    /**
     * Verifica se o tipo é um array
     */
    public boolean isArray() {
        return this == ARRAY_INT || this == ARRAY_STRING || this == ARRAY_BOOL || 
               this == ARRAY_FLOAT32 || this == ARRAY_FLOAT64;
    }

    /**
     * Obtém o tipo do elemento do array
     */
    public GoType getElementType() {
        switch (this) {
            case ARRAY_INT: return INT;
            case ARRAY_STRING: return STRING;
            case ARRAY_BOOL: return BOOL;
            case ARRAY_FLOAT32: return FLOAT32;
            case ARRAY_FLOAT64: return FLOAT64;
            default: return UNKNOWN;
        }
    }

    /**
     * Cria tipo de array a partir do tipo do elemento
     */
    public static GoType arrayOf(GoType elementType) {
        switch (elementType) {
            case INT: return ARRAY_INT;
            case STRING: return ARRAY_STRING;
            case BOOL: return ARRAY_BOOL;
            case FLOAT32: return ARRAY_FLOAT32;
            case FLOAT64: return ARRAY_FLOAT64;
            default: return UNKNOWN;
        }
    }

    /**
     * Mapeia tipos Go para índices das tabelas de unificação
     */
    private int getUnificationIndex() {
        if (isInteger()) return 0;      // Inteiros
        if (isFloat()) return 1;        // Ponto flutuante
        if (this == BOOL) return 2;     // Booleano
        if (this == STRING) return 3;   // String
        if (isArray()) return 4;        // Arrays (corrigido para índice 4)
        return 5;                       // Tipos inválidos/desconhecidos (movido para 5)
    }


    // Tabela de unificação para operador '+' (adição/concatenação)
    // Em Go: apenas tipos idênticos podem ser somados, exceto strings que podem ser concatenadas
    private static final GoType[][] plusTable = {
        //           INT    FLOAT   BOOL    STRING  ARRAY   INVALID
        /* INT */    { INT,   NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* FLOAT */  { NO_TYPE, FLOAT64, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* BOOL */   { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* STRING */ { NO_TYPE, NO_TYPE, NO_TYPE, STRING,  NO_TYPE, NO_TYPE },
        /* ARRAY */  { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* INVALID */{ NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE }
    };


    // Tabela de unificação para operadores aritméticos (-, *, /, %)
    // Em Go: apenas tipos numéricos idênticos
    private static final GoType[][] arithmeticTable = {
        //           INT    FLOAT   BOOL    STRING  ARRAY   INVALID
        /* INT */    { INT,   NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* FLOAT */  { NO_TYPE, FLOAT64, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* BOOL */   { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* STRING */ { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* ARRAY */  { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* INVALID */{ NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE }
    };

    // Tabela de unificação para operadores de comparação (<, >, <=, >=, ==, !=)
    // Em Go: apenas tipos comparáveis idênticos
    private static final GoType[][] comparisonTable = {
        //           INT    FLOAT   BOOL    STRING  ARRAY   INVALID
        /* INT */    { BOOL,  NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* FLOAT */  { NO_TYPE, BOOL,   NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* BOOL */   { NO_TYPE, NO_TYPE, BOOL,   NO_TYPE, NO_TYPE, NO_TYPE },
        /* STRING */ { NO_TYPE, NO_TYPE, NO_TYPE, BOOL,   NO_TYPE, NO_TYPE },
        /* ARRAY */  { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* INVALID */{ NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE }
    };

    // Tabela de unificação para operadores lógicos (&&, ||)
    // Em Go: apenas operandos booleanos
    private static final GoType[][] logicalTable = {
        //           INT    FLOAT   BOOL    STRING  ARRAY   INVALID
        /* INT */    { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* FLOAT */  { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* BOOL */   { NO_TYPE, NO_TYPE, BOOL,    NO_TYPE, NO_TYPE, NO_TYPE },
        /* STRING */ { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* ARRAY */  { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* INVALID */{ NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE }
    };

    /**
     * Unifica tipos para operador de adição/concatenação
     */
    public GoType unifyPlus(GoType other) {
        return plusTable[this.getUnificationIndex()][other.getUnificationIndex()];
    }

    /**
     * Unifica tipos para operadores aritméticos (-, *, /, %)
     */
    public GoType unifyArithmetic(GoType other) {
        return arithmeticTable[this.getUnificationIndex()][other.getUnificationIndex()];
    }


    /**
     * Unifica tipos para operadores de comparação
     */
    public GoType unifyComparison(GoType other) {
        return comparisonTable[this.getUnificationIndex()][other.getUnificationIndex()];
    }

    /**
     * Unifica tipos para operadores lógicos
     */
    public GoType unifyLogical(GoType other) {
        return logicalTable[this.getUnificationIndex()][other.getUnificationIndex()];
    }

    /**
     * Verifica se dois tipos são compatíveis para operações
     * Em Go, tipos devem ser idênticos para a maioria das operações
     */
    public boolean isCompatibleWith(GoType other) {
        if (this == other)
            return true;

        // Em Go, tipos devem ser explicitamente convertidos
        // Apenas UNKNOWN é compatível com qualquer tipo (para casos onde não conseguimos inferir)
        if (this == UNKNOWN || other == UNKNOWN) {
            return true;
        }

        // Para arrays, verificar compatibilidade dos tipos de elemento
        if (this.isArray() && other.isArray()) {
            return this.getElementType().isCompatibleWith(other.getElementType());
        }

        return false;
    }

    /**
     * Verifica se o tipo pode ser usado em contexto booleano
     */
    public boolean isBooleanContext() {
        return this == BOOL;
    }

    @Override
    public String toString() {
        return typeName;
    }
}