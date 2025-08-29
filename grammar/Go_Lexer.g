lexer grammar Go_Lexer;

// Whitespace
WS : [ \t\n\r]+ -> skip ;

// Comments
COMMENT_A     : '//' ~[\r\n]* -> skip ;
COMMENT_B     : '/*' ~[*/]* '*/' -> skip ;

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
S_ASSIGN : ':=' ;
OR       : '||' ;
AND      : '&&' ;
EQUALS   : '==' ;
NOTEQUAL : '!=' ;
LETHAN   : '<=' ;
GETHAN   : '>=' ;
LTHAN    : '<' ;
GTHAN    : '>' ;
PLUS     : '+' ;
MINUS    : '-' ;
INC      : '++' ;
DEC      : '--' ;
TIMES    : '*' ;
OVER     : '/' ;
MOD      : '%' ;
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

// Literais básicos simplificados
INT_LIT : DIGITS | '0b' BINDIGITS | '0o' OCTDIGITS | '0x' HEXDIGITS | '0B' BINDIGITS | '0O' OCTDIGITS | '0X' HEXDIGITS ;
FLOAT_LIT : DIGITS '.' DIGITS | DIGITS '.' DIGITS [eE] [+-]? DIGITS | DIGITS [eE] [+-]? DIGITS | '0x' HEXDIGITS '.' HEXDIGITS 'p' [+-]? DIGITS | '0X' HEXDIGITS '.' HEXDIGITS 'P' [+-]? DIGITS ;
STRING_LIT : '"' ( ~["\\\r\n] | '\\' [abfnrtv\\"'0] | '\\x' [0-9a-fA-F] [0-9a-fA-F] )* '"' ;

// Regras básicas conforme solicitado
ID : [a-zA-Z_][a-zA-Z0-9_]* ;
DIGITS : [0-9]+ ; 
HEXDIGITS : [0-9a-fA-F]+ ; 
BINDIGITS : [0-1]+ ; 
OCTDIGITS : [0-7]+ ;