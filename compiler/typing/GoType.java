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
    AUTO("auto"); // Para inferência de tipo

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

    @Override
    public String toString() {
        return typeName;
    }
}
