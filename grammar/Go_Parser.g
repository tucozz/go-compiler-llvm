parser grammar Go_Parser;

options {
  tokenVocab = Go_Lexer;
}

// Programa: sequência de declarações de nível superior
program: topLevelDecl* EOF;

// Declarações de nível superior
topLevelDecl:
    varDecl
    | constDecl
    | typeDecl
    | funcDecl
;

// Declarações
varDecl:
    VAR varSpec SEMICOLON
;

constDecl:
    CONST constSpec SEMICOLON
;

typeDecl:
    TYPE typeSpecDecl SEMICOLON
;

funcDecl:
    FUNC ID PAR_INT (parameterList)? PAR_END (typeSpec)? block
;

// Especificações para declarações
varSpec:
    ID typeSpec (ASSIGN expr)?          # varSingle
    | ID (COMMA ID)* ASSIGN exprList    # varMulti
;

constSpec:
    ID typeSpec (ASSIGN expr)?          # constSingle
    | ID (COMMA ID)* ASSIGN exprList    # constMulti
;

typeSpecDecl:
    ID structType
;

structType:
    STRUCT C_BRA_INT (fieldDecl)* C_BRA_END
;

fieldDecl:
    ID typeSpec SEMICOLON
;

parameterList:
    parameter (COMMA parameter)*
;

parameter:
    ID typeSpec
;

typeSpec:
    INT | INT8 | INT16 | INT32 | INT64
    | UINT | UINT8 | UINT16 | UINT32 | UINT64
    | BOOL
    | STRING
    | FLOAT32 | FLOAT64
    | ID
    | S_BRA_INT S_BRA_END typeSpec
;

// Instruções
stmt:
    simpleStmt
    | block
    | ifStmt
    | switchStmt
    | forStmt
    | returnStmt
    | continueStmt
    | fallthroughStmt
;

simpleStmt:
    emptyStmt
    | exprStmt
    | assignStmt
    | incDecStmt
;

emptyStmt:
    SEMICOLON
;

exprStmt:
    expr SEMICOLON
;

assignStmt:
    lvalue ASSIGN expr SEMICOLON
    | lvalue S_ASSIGN expr SEMICOLON
;

incDecStmt:
    lvalue (INC | DEC) SEMICOLON
;

ifStmt:
    IF expr block (ELSEIF expr block)* (ELSE block)?
;

switchStmt:
    SWITCH (expr)? C_BRA_INT (caseClause)* (defaultClause)? C_BRA_END
;

caseClause:
    CASE exprList COLON (stmt)*
;

defaultClause:
    DEFAULT COLON (stmt)*
;

forStmt:
    FOR (forClause | forRangeClause | expr)? block
;

forClause:
    simpleStmt? SEMICOLON expr? SEMICOLON simpleStmt?
;

forRangeClause:
    (lvalue (COMMA lvalue)?) S_ASSIGN RANGE expr
;

returnStmt:
    RETURN expr? SEMICOLON
;

block:
    C_BRA_INT (stmt)* C_BRA_END
;

continueStmt:
    CONTINUE
;

fallthroughStmt:
    FALLT
;

// Expressões
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
    addSubtractExpr (relationOp addSubtractExpr)*
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

functionCall:
    ID PAR_INT (exprList)? PAR_END
;

exprList:
    expr (COMMA expr)*
;

arrayAccess:
    ID S_BRA_INT expr S_BRA_END
;

structAccess:
    ID (DOT ID)+
;

relationOp:
    EQUALS
    | NOTEQUAL
    | GTHAN
    | LTHAN
    | GETHAN
    | LETHAN
;