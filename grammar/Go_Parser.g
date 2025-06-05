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
|   expr
;

stmt: 
    assignment
|   inc_dec_stmt
;

for_stmt:
    FOR
;

item:
    ID (type)? ASSIGN (STRINGF | POS_INT | NEG_INT | POS_REAL | NEG_REAL)
;

const_decl:
    CONST ID (type)? ASSIGN (STRINGF | POS_INT | NEG_INT | POS_REAL | NEG_REAL) SEMICOLON # constSimple
|   CONST PAR_INT (item SEMICOLON)+ PAR_END # constGroup
|   CONST ID (COMMA ID)* ASSIGN (STRINGF | POS_INT | NEG_INT | POS_REAL | NEG_REAL) (COMMA (STRINGF | POS_INT | NEG_INT | POS_REAL | NEG_REAL))* SEMICOLON # constMulti
;


var_decl:
    VAR ID (type)? ASSIGN (STRINGF | POS_INT | NEG_INT | POS_REAL | NEG_REAL) SEMICOLON # varSimple
|   VAR PAR_INT (item SEMICOLON)+ PAR_END # varGroup
|   VAR ID (COMMA ID)* ASSIGN (STRINGF | POS_INT | NEG_INT | POS_REAL | NEG_REAL) (COMMA (STRINGF | POS_INT | NEG_INT | POS_REAL | NEG_REAL))* SEMICOLON # varMulti
;


type:
    INT
|   INT8
|   INT16
|   INT32
|   INT64
|   UINT
|   UINT8
|   UINT16
|   UINT32
|   UINT64
|   BOOL
|   STRING
|   FLOAT32
|   FLOAT64
;

declaration:
    const_decl
    var_decl
;