lexer grammar Go_Lexer;

WS : [ \t\n\r]+ -> skip ;

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

POS_INT : DIGITS | '0x' HEXDIGITS | '0X' HEXDIGITS | '0b' BINDIGITS | '0B' BINDIGITS | '0o' OCTDIGITS | '0' OCTDIGITS ; 
NEG_INT : '-' (DIGITS | '0x' HEXDIGITS | '0X' HEXDIGITS | '0b' BINDIGITS | '0B' BINDIGITS | '0o' OCTDIGITS | '0' OCTDIGITS) ; 
POS_REAL : DIGITS '.' DIGITS (('e' | 'E') ('+' | '-')? DIGITS)? ; 
NEG_REAL : '-' DIGITS '.' DIGITS (('e' | 'E') ('+' | '-')? DIGITS)? ;

ID            : [a-zA-Z_][a-zA-Z0-9_]* ;
COMMENT_A     : '//' ~[\n]* ;
COMMENT_B     : '/*' ~[*/]* '*/';
STRUCT_ACCESS : ID ('.' ID)* ;
DIGITS : [0-9]+ ; 
HEXDIGITS : [0-9a-fA-F]+ ; 
BINDIGITS : [0-1]+ ; 
OCTDIGITS : [0-7]+ ;

STRINGF   : '"' ( ESCAPE | UTF8CHAR | ~["\\\r\n])* '"' ;
ESCAPE    : '\\' (["\\ntbrf] | 'u' HEX4 | 'U' HEX8) ;
HEXDIGIT  : [0-9a-fA-F] ;
HEX4      : HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT ;
HEX8      : HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT ;
UTF8CHAR  : [\u0001-\u007F] | [\u0080-\u07FF] | [\u0800-\uFFFF] | [\u{10000}-\u{10FFFF}] ;