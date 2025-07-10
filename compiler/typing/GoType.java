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
    NO_TYPE("no_type"); // Para operações inválidas

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
     * Mapeia tipos Go para índices das tabelas de unificação
     */
    private int getUnificationIndex() {
        if (isInteger()) return 0;      // Inteiros
        if (isFloat()) return 1;        // Ponto flutuante
        if (this == BOOL) return 2;     // Booleano
        if (this == STRING) return 3;   // String
        return 4;                       // Tipos inválidos/desconhecidos
    }


    // Tabela de unificação para operador '+' (adição/concatenação)
    private static final GoType[][] plusTable = {
        //           INT    FLOAT   BOOL    STRING  INVALID
        /* INT */    { INT,     FLOAT64, NO_TYPE, STRING, NO_TYPE },
        /* FLOAT */  { FLOAT64, FLOAT64, NO_TYPE, STRING, NO_TYPE },
        /* BOOL */   { NO_TYPE, NO_TYPE, NO_TYPE, STRING, NO_TYPE },
        /* STRING */ { STRING,  STRING,  STRING,  STRING, NO_TYPE },
        /* INVALID */{ NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE }
    };


    // Tabela de unificação para operadores aritméticos (-, *, /, %)
    private static final GoType[][] arithmeticTable = {
        //           INT    FLOAT   BOOL    STRING  INVALID
        /* INT */    { INT,     FLOAT64, NO_TYPE, NO_TYPE, NO_TYPE },
        /* FLOAT */  { FLOAT64, FLOAT64, NO_TYPE, NO_TYPE, NO_TYPE },
        /* BOOL */   { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* STRING */ { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* INVALID */{ NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE }
    };

    // Tabela de unificação para operadores de comparação (<, >, <=, >=, ==, !=)
    private static final GoType[][] comparisonTable = {
        //           INT    FLOAT   BOOL    STRING  INVALID
        /* INT */    { BOOL,    BOOL,    NO_TYPE, NO_TYPE, NO_TYPE },
        /* FLOAT */  { BOOL,    BOOL,    NO_TYPE, NO_TYPE, NO_TYPE },
        /* BOOL */   { NO_TYPE, NO_TYPE, BOOL,    NO_TYPE, NO_TYPE },
        /* STRING */ { NO_TYPE, NO_TYPE, NO_TYPE, BOOL,    NO_TYPE },
        /* INVALID */{ NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE }
    };

    // Tabela de unificação para operadores lógicos (&&, ||)
    private static final GoType[][] logicalTable = {
        //           INT    FLOAT   BOOL    STRING  INVALID
        /* INT */    { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* FLOAT */  { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* BOOL */   { NO_TYPE, NO_TYPE, BOOL,    NO_TYPE, NO_TYPE },
        /* STRING */ { NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE },
        /* INVALID */{ NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE, NO_TYPE }
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
     */
    public boolean isCompatibleWith(GoType other) {
        if (this == other)
            return true;

        // Compatibilidade entre tipos numéricos
        if (this.isNumeric() && other.isNumeric()) {
            return true;
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
