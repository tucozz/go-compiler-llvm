parser grammar Go_Parser;

options {
  tokenVocab = EZLexer;
}

unary_op:
    INC
|   DEC
|   NOT
|   MINUS
;

binary_op:
    OR
|   AND
;

relation_op:
    EQUALS
|   NOTEQUAL
|   GTHAN
|   LTHAN
|   GETHAN
|   LETHAN
;

basic_op:
    PLUS
|   MINUS
|   TIMES
|   LTHAN
|   OVER
|   MOD
;

assignment:
    expr ASSIGN expr SEMICOLON
    expr S_ASSIGN expr SEMICOLON
;

inc_dec_stmt:
    expr (INC | DEC) SEMICOLON
;

expr:
    expr relation_op expr
|   expr basic_op expr
|   expr binary_op expr
;

stmt: 
    assignment
|   inc_dec_stmt
;