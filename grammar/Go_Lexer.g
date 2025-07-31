lexer grammar Go_Lexer;

// Whitespace
WS : [ \t\n\r]+ -> skip ;

// Letras e Dígitos
UNICODE_LETTER : \p{L} ;
UNICODE_DIGIT  : \p{Nd} ;
LETTER         : UNICODE_LETTER | '_' ;
DECIMAL_DIGIT  : [0-9] ;
BINARY_DIGIT   : [0-1] ;
OCTAL_DIGIT    : [0-7] ;
HEX_DIGIT      : [0-9a-fA-F] ; // Consolidated and used consistently

// Identifier
ID : LETTER (LETTER | UNICODE_DIGIT)* ;
// Keywords
BREAK    : 'break' ;
CASE     : 'case' ;
CHAN    : 'chan' ;
CONST   : 'const' ;
CONTINUE : 'continue' ;
DEFAULT  : 'default' ;
DEFER   : 'defer' ;
ELSE     : 'else' ;
FALLT    : 'fallthrough' ;
FOR      : 'for' ;
FUNC    : 'func' ;
GO      : 'go' ;
GOTO    : 'goto' ;
IMPORT  : 'import' ;
IF       : 'if' ;
INTERFACE : 'interface' ;
MAP     : 'map' ;
PACKAGE : 'package' ;
RANGE    : 'range' ;
RETURN   : 'return' ;
SELECT  : 'select' ;
STRUCT  : 'struct' ;
SWITCH   : 'switch' ;
TYPE    : 'type' ;
VAR     : 'var' ;

// Operadores e Pontuação
PLUS     : '+' ;
MINUS    : '-' ;
TIMES    : '*' ;
OVER     : '/' ;
MOD      : '%' ;
ASSIGN_PLUS : '+=' ;
ASSIGN_MINUS : '-=' ;
ASSIGN_TIMES : '*=' ;
ASSIGN_OVER  : '/=' ;
ASSIGN_MOD   : '%=' ;
AND      : '&&' ;
OR       : '||' ;
INC      : '++' ;
DEC      : '--' ;
EQUALS   : '==' ;
LTHAN    : '<' ;
GTHAN    : '>' ;
ASSIGN   : '=' ;
NOT      : '!' ;
NOTEQUAL : '!=' ;
LETHAN   : '<=' ;
GETHAN   : '>=' ;
S_ASSIGN : ':=' ;
PAR_INT  : '(' ;
PAR_END  : ')' ;
S_BRA_INT: '[' ;
S_BRA_END : ']' ;
C_BRA_INT : '{' ;
C_BRA_END : '}' ;
SEMICOLON : ';' ;
COLON     : ':' ;
COMMA     : ',' ;
DOT       : '.' ;
KW_TRUE  : 'true' ;
KW_FALSE : 'false' ;

// Literais inteiros
INT_LIT : DECIMAL_LIT | BINARY_LIT | OCTAL_LIT | HEX_LIT ;
DECIMAL_LIT : '0' | [1-9] ('_'? DECIMAL_DIGIT)* ;
BINARY_LIT  : '0' [bB] '_'? BINARY_DIGIT ('_'? BINARY_DIGIT)* ;
OCTAL_LIT   : '0' [oO]? '_'? OCTAL_DIGIT ('_'? OCTAL_DIGIT)* ;
HEX_LIT     : '0' [xX] '_'? HEX_DIGIT ('_'? HEX_DIGIT)* ;

// Literais de ponto flutuante
FLOAT_LIT : DECIMAL_FLOAT_LIT | HEX_FLOAT_LIT ;
DECIMAL_FLOAT_LIT : DECIMAL_DIGITS '.' DECIMAL_DIGITS? DECIMAL_EXPONENT?
                 | DECIMAL_DIGITS DECIMAL_EXPONENT
                 | '.' DECIMAL_DIGITS DECIMAL_EXPONENT? ;
DECIMAL_DIGITS : DECIMAL_DIGIT ('_'? DECIMAL_DIGIT)* ;
DECIMAL_EXPONENT : [eE] [+-]? DECIMAL_DIGITS ;
HEX_MANTISSA : '_'? HEX_DIGIT ('_'? HEX_DIGIT)* ('.' ('_'? HEX_DIGIT ('_'? HEX_DIGIT)*)?)?
            | '.' HEX_DIGIT ('_'? HEX_DIGIT)* ;
HEX_EXPONENT : [pP] [+-]? DECIMAL_DIGITS ;

// Built-in types (these are keywords)
INT     : 'int' ;
INT8    : 'int8' ;
INT16   : 'int16' ;
INT32   : 'int32' ;
INT64   : 'int64' ;
UINT    : 'uint' ;
UINT8   : 'uint8' ;
UINT16  : 'uint16' ;
UINT32  : 'uint32' ;
UINT64  : 'uint64' ;
BOOL    : 'bool' ;
STRING  : 'string' ;
FLOAT32 : 'float32' ;
FLOAT64 : 'float64' ;

// Comments
COMMENT_A     : '//' ~[\r\n]* -> skip; // Add -> skip
COMMENT_B     : '/*' .*? '*/' -> skip; // Use .*? (non-greedy any char) to match until next '*/'

STRINGF : '"' ( ESCAPE_SEQUENCE | ~["\\] )*? '"' ; // Non-greedy match
fragment ESCAPE_SEQUENCE : '\\' ( [abfnrtv\\'"] | HEX_DIGIT HEX_DIGIT | 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT | 'U' (HEX_DIGIT){8} );