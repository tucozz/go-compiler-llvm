lexer grammar Go_Lexer;

WS : [ \t\n]+ -> skip ;

IF       : 'if' ;
ELSEIF   : 'else if' ;
ELSE     : 'else' ;
SWITCH   : 'switch' ;
CASE     : 'case' ;
DEFAULT  : 'default' ;
FOR      : 'for' ;
CONTINUE : 'continue' ;
BREAK    : 'break' ;
FALLT    : 'fallthrough' ;
RANGE    : 'range' ;

FUNC    : 'func' ;
VAR     : 'var' ;
CONST   : 'const' ;
TYPE    : 'type' ;
STRUCT  : 'struct' ;
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
KW_TRUE  : 'true' ;
KW_FALSE : 'false' ;

PLUS      : '+' ;
MINUS     : '-' ;
TIMES     : '*' ;
OVER      : '/' ;
MOD       : '%' ;
INC       : '++' ;
DEC       : '--' ;
PAR_INT   : '(' ;
PAR_END   : ')' ;
S_BRA_INT : '[' ; 
S_BRA_END : ']' ;
C_BRA_INT : '{' ;
C_BRA_END : '}' ;
SEMICOLON : ';' ;
COMMA     : ',' ;

STRINGF       : '"' ~["]* '"' ;
ID            : [a-zA-Z_][a-zA-Z0-9_]* ;
COMMENT_A     : '//' ~[\n]* ;
COMMENT_B     : '/*' ~[*/]* '*/';
STRUCT_ACCESS : ID ('.' ID)* ;


fragment DIGITS : [0-9]+ ;

POS_INT  : DIGITS ;
NEG_INT  : '-' DIGITS;
POS_REAL : DIGITS '.' DIGITS ;
NEG_REAL : '-' DIGITS '.' DIGITS ;