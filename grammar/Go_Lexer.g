lexer grammar Go_Lexer;

WS : [ \t\n]+ -> skip ;

KW_BEGIN   : 'begin' ;
KW_BOOL    : 'bool' ;
KW_ELSE    : 'else' ;
KW_END     : 'end' ;
KW_FALSE   : 'false' ;
KW_IF      : 'if' ;
KW_INT     : 'int' ;
KW_PROGRAM : 'program' ;
KW_READ    : 'read' ;
KW_REAL    : 'real' ;
KW_REPEAT  : 'repeat' ;
KW_STRING  : 'string' ;
KW_THEN    : 'then' ;
KW_TRUE    : 'true' ;
KW_UNTIL   : 'until' ;
KW_VAR     : 'var' ;
KW_WRITE   : 'write' ;

ASSIGN  : ':=' ;
EQUALS  : '=' ;
PLUS    : '+' ;
MINUS   : '-' ;
TIMES   : '*' ;
OVER    : '/' ;
PAR_INT : '(' ;
PAR_END : ')' ;
COMMA   : ';' ;

fragment DIGITS : [0-9]+ ;

POS_INT : DIGITS ;
NEG_INT : '-' DIGITS;
POS_REAL : DIGITS '.' DIGITS ;
NEG_REAL : '-' DIGITS '.' DIGITS ;

STRING  : '"' ~["]* '"' ;
ID      : [a-zA-Z]+ ;
COMMENT : '{' ~[}]* '}' ;