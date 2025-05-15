lexer grammar Go_Lexer;

WS : [ \t\n]+ -> skip ;

KW_PACKAGE : 'package' ;
KW_IMPORT  : 'import' ;

KW_IF       : 'if' ;
KW_ELSEIF   : 'else if' ;
KW_ELSE     : 'else' ;
KW_SWITCH   : 'switch' ;
KW_CASE     : 'case' ;
KW_DEFAULT  : 'default' ;
KW_FOR      : 'for' ;
KW_CONTINUE : 'conrinue' ;
KW_BREAK    : 'break' ;
KW_RANGE    : 'range' ;

KW_FUNC    : 'func' ;
KW_VAR     : 'var' ;
KW_INT     : 'int' ;
KW_INT8    : 'int8' ;
KW_INT16   : 'int16' ;
KW_INT32   : 'int32' ;
KW_INT64   : 'int64' ;
KW_UINT    : 'uint' ;
KW_UINT8   : 'uint8' ;
KW_UINT16  : 'uint16' ;
KW_UINT32  : 'uint32' ;
KW_UINT64  : 'uint64' ;
KW_BOOL    : 'bool' ;
KW_STRING  : 'string' ;
KW_FLOAT32 : 'float32' ;
KW_FLOAT64 : 'float64' ;

ASSIGN   : '=' ;
S_ASSIGN : ':=' ;
EQUALS   : '==' ;
NOTEQUAL : '!=' ;
GTHAN    : '>' ;
LTHAN    : '<' ;
GETHAN   : '>=' ;
LETHAN   : '<=' ;
AND      : '&&' ;
OR       : '||' ;
NOT      : '!' ;
KW_TRUE    : 'true' ;
KW_FALSE   : 'false' ;

PLUS     : '+' ;
MINUS    : '-' ;
TIMES    : '*' ;
OVER     : '/' ;
MOD      : '%' ;
INC      : '++' ;
DEC      : '--' ;
PAR_INT  : '(' ;
PAR_END  : ')' ;
COMMA    : ';' ;

fragment DIGITS : [0-9]+ ;

POS_INT : DIGITS ;
NEG_INT : '-' DIGITS;
POS_REAL : DIGITS '.' DIGITS ;
NEG_REAL : '-' DIGITS '.' DIGITS ;

STRING    : '"' ~["]* '"' ;
ID        : [a-zA-Z]+ ;
COMMENT_A : '//' ~[\n]* ;
COMMENT_B : '/*' ~[*/]* '*/';