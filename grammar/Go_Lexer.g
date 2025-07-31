lexer grammar Go_Lexer;

// Whitespace
WS : [ \t\n\r]+ -> skip ;

// Comments
COMMENT_A     : '//' ~[\r\n]* -> skip ;
COMMENT_B     : '/*' ~[*/]* '*/' -> skip ;

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
BYTE    : 'byte' ;
RUNE    : 'rune' ;
KW_TRUE  : 'true' ;
KW_FALSE : 'false' ;

// Operadores e Pontuação
ASSIGN_PLUS : '+=' ;
ASSIGN_MINUS : '-=' ;
ASSIGN_TIMES : '*=' ;
ASSIGN_OVER  : '/=' ;
ASSIGN_MOD   : '%=' ;
S_ASSIGN : ':=' ;
EQUALS   : '==' ;
NOTEQUAL : '!=' ;
LETHAN   : '<=' ;
GETHAN   : '>=' ;
AND      : '&&' ;
OR       : '||' ;
INC      : '++' ;
DEC      : '--' ;
PLUS     : '+' ;
MINUS    : '-' ;
TIMES    : '*' ;
OVER     : '/' ;
MOD      : '%' ;
LTHAN    : '<' ;
GTHAN    : '>' ;
ASSIGN   : '=' ;
NOT      : '!' ;
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

// Literais (DEVEM vir ANTES dos identificadores)
FLOAT_LIT : DECIMAL_FLOAT_LIT | HEX_FLOAT_LIT ;
INT_LIT : DECIMAL_LIT | BINARY_LIT | OCTAL_LIT | HEX_LIT ;
STRING_LIT : INTERPRETED_STRING_LIT | RAW_STRING_LIT ;

// Identifier (DEVE vir DEPOIS de keywords, tipos e literais)
ID : LETTER [a-zA-Z0-9_]* ;

// Fragments para literais (NÃO geram tokens)
fragment DECIMAL_LIT : '0' | [1-9] ('_'? DECIMAL_DIGIT)* ;
fragment BINARY_LIT  : '0' [bB] '_'? BINARY_DIGIT ('_'? BINARY_DIGIT)* ;
fragment OCTAL_LIT   : '0' [oO]? '_'? OCTAL_DIGIT ('_'? OCTAL_DIGIT)* ;
fragment HEX_LIT     : '0' [xX] '_'? HEX_DIGIT ('_'? HEX_DIGIT)* ;

fragment DECIMAL_FLOAT_LIT : DECIMAL_DIGITS '.' DECIMAL_DIGITS? DECIMAL_EXPONENT?
                          | DECIMAL_DIGITS DECIMAL_EXPONENT
                          | '.' DECIMAL_DIGITS DECIMAL_EXPONENT? ;
fragment HEX_FLOAT_LIT : '0' [xX] HEX_MANTISSA HEX_EXPONENT ;

fragment DECIMAL_DIGITS : DECIMAL_DIGIT ('_'? DECIMAL_DIGIT)* ;
fragment DECIMAL_EXPONENT : [eE] [+-]? DECIMAL_DIGITS ;
fragment HEX_MANTISSA : '_'? HEX_DIGIT ('_'? HEX_DIGIT)* ('.' ('_'? HEX_DIGIT ('_'? HEX_DIGIT)*)?)?
                     | '.' HEX_DIGIT ('_'? HEX_DIGIT)* ;
fragment HEX_EXPONENT : [pP] [+-]? DECIMAL_DIGITS ;

fragment INTERPRETED_STRING_LIT : '"' ( ~["\\\r\n] | ESCAPE_SEQUENCE )* '"' ;
fragment RAW_STRING_LIT : '`' ~[`]* '`' ;
fragment ESCAPE_SEQUENCE : '\\' ( [abfnrtv\\"'0] | 'x' HEX_DIGIT HEX_DIGIT ) ;