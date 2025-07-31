lexer grammar Go_Lexer;

// Whitespace
WS : [ \t\n\r]+ -> skip ;

// Comments
COMMENT_A     : '//' ~[\r\n]*;
COMMENT_B     : '/*' ~[*/]* '*/';

// Letras e Dígitos (fragmentos - não geram tokens)
fragment LETTER         : [a-zA-Z_] ;
fragment DECIMAL_DIGIT  : [0-9] ;
fragment BINARY_DIGIT   : [0-1] ;
fragment OCTAL_DIGIT    : [0-7] ;
fragment HEX_DIGIT      : [0-9a-fA-F] ;

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
HEX_FLOAT_LIT : '0' [xX] HEX_MANTISSA HEX_EXPONENT ;
DECIMAL_DIGITS : DECIMAL_DIGIT ('_'? DECIMAL_DIGIT)* ;
DECIMAL_EXPONENT : [eE] [+-]? DECIMAL_DIGITS ;
HEX_MANTISSA : '_'? HEX_DIGIT ('_'? HEX_DIGIT)* ('.' ('_'? HEX_DIGIT ('_'? HEX_DIGIT)*)?)?
            | '.' HEX_DIGIT ('_'? HEX_DIGIT)* ;
HEX_EXPONENT : [pP] [+-]? DECIMAL_DIGITS ;

// Tipos primitivos do Go
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
BYTE    : 'byte' ;       // alias para uint8
RUNE    : 'rune' ;       // alias para int32

// Identifier
ID : LETTER [a-zA-Z0-9_]* ;

// String literals
STRING_LIT : INTERPRETED_STRING_LIT | RAW_STRING_LIT ;
// String interpretada (com escape sequences básicas)
INTERPRETED_STRING_LIT : '"' ( ~["\\\r\n] | ESCAPE_SEQUENCE )* '"' ;
// String crua (sem escape sequences)
RAW_STRING_LIT : '`' ~[`]* '`' ;
// Escape sequences básicas do Go (versão minimalista)
ESCAPE_SEQUENCE : '\\' ( [abfnrtv\\"'0] | 'x' HEX_DIGIT HEX_DIGIT ) ;