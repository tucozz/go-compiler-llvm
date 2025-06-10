parser grammar Go_Parser;

options {
  tokenVocab = Go_Lexer;
}

program: (declaration | statement)* EOF;

declaration:
    varDeclaration
    | constDeclaration
    | typeDeclaration
    | functionDeclaration
;

varDeclaration: VAR ID typeSpec (ASSIGN expr)? SEMICOLON; 
constDeclaration: CONST ID typeSpec (ASSIGN expr)? SEMICOLON;

// Atualmente, apenas structs como tipo composto
typeDeclaration: TYPE ID structType SEMICOLON;
structType: STRUCT C_BRA_INT (fieldDeclaration)* C_BRA_END;
fieldDeclaration: ID typeSpec SEMICOLON;

functionDeclaration: FUNC ID PAR_INT (parameterList)? PAR_END (typeSpec)? C_BRA_INT (statement)* C_BRA_END;
parameterList: parameter (COMMA parameter)*;
parameter: ID typeSpec; // Ex: x int, y string

typeSpec:
    INT | INT8 | INT16 | INT32 | INT64
    | UINT | UINT8 | UINT16 | UINT32 | UINT64
    | BOOL
    | STRING
    | FLOAT32 | FLOAT64
    | ID
    | S_BRA_INT S_BRA_END typeSpec
;

statement:
    assignment
    | inc_dec_stmt
    | ifStmt
    | forStmt
    | returnStmt
    | block
    | simpleStmt SEMICOLON 
;

simpleStmt:
    expr // Uma expressão que pode ser uma instrução, como uma chamada de função
;

assignment:
    lvalue ASSIGN expr SEMICOLON
    | lvalue S_ASSIGN expr SEMICOLON
;

inc_dec_stmt: lvalue (INC | DEC) SEMICOLON;

ifStmt:
    IF expr C_BRA_INT (statement)* C_BRA_END (ELSEIF expr C_BRA_INT (statement)* C_BRA_END)* (ELSE C_BRA_INT (statement)* C_BRA_END)?
;

forStmt:
    FOR (forClause | forRangeClause | expr)? C_BRA_INT (statement)* C_BRA_END
;

forClause:
    simpleStmt? SEMICOLON expr? SEMICOLON simpleStmt?
;

forRangeClause:
    (ID (COMMA ID)?) S_ASSIGN RANGE expr
;

returnStmt:
    'return' expr? SEMICOLON
;

block:
    C_BRA_INT (statement)* C_BRA_END
;

expr:
    orExpr
;

orExpr:
    andExpr (OR andExpr)*
;

andExpr:
    relationExpr (AND relationExpr)*
;

relationExpr:
    addSubtractExpr (relation_op addSubtractExpr)*
;

addSubtractExpr:
    mulDivModExpr ((PLUS | MINUS) mulDivModExpr)*
;

mulDivModExpr:
    unaryOpExpr ((TIMES | OVER | MOD) unaryOpExpr)*
;

unaryOpExpr:
    (PLUS | MINUS | NOT) unaryOpExpr
    | primaryExpr (INC | DEC)?
;

primaryExpr:
    ID
    | POS_INT
    | NEG_INT 
    | POS_REAL
    | NEG_REAL
    | STRINGF
    | KW_TRUE
    | KW_FALSE
    | PAR_INT expr PAR_END
    | functionCall
    | arrayAccess
    | structAccess
;

lvalue:
    ID
    | arrayAccess
    | structAccess
;

functionCall: ID PAR_INT (expressionList)? PAR_END;
expressionList: expr (COMMA expr)*;

arrayAccess: ID S_BRA_INT expr S_BRA_END;

structAccess: ID (DOT ID)+;

relation_op:
    EQUALS
    | NOTEQUAL
    | GTHAN
    | LTHAN
    | GETHAN
    | LETHAN
;