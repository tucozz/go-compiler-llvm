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

varDeclaration: VAR ID typeSpec (ASSIGN expr)? statementEnd;
constDeclaration: CONST ID typeSpec (ASSIGN expr)? statementEnd;

typeDeclaration: TYPE ID structType statementEnd;
structType: STRUCT C_BRA_INT (fieldDeclaration)* C_BRA_END;
fieldDeclaration: ID typeSpec statementEnd;

functionDeclaration: FUNC ID PAR_INT (parameterList)? PAR_END (typeSpec)? C_BRA_INT (statement)* C_BRA_END;
parameterList: parameter (COMMA parameter)*;
parameter: ID typeSpec;

typeSpec:
    INT | INT8 | INT16 | INT32 | INT64
    | UINT | UINT8 | UINT16 | UINT32 | UINT64
    | BOOL
    | STRING
    | FLOAT32 | FLOAT64
    | ID             // Para tipos definidos pelo usuário (structs, etc.)
    | S_BRA_INT S_BRA_END typeSpec // Para arrays, e.g., []int
;

statement:
    assignment
    | inc_dec_stmt
    | ifStmt
    | forStmt
    | returnStmt
    | block
    | exprStmt
;

exprStmt: expr statementEnd;

assignment:
    lvalue ASSIGN expr statementEnd
    | lvalue S_ASSIGN expr statementEnd
;

inc_dec_stmt: lvalue (INC | DEC) statementEnd;

ifStmt:
    IF expr C_BRA_INT (statement)* C_BRA_END (ELSEIF expr C_BRA_INT (statement)* C_BRA_END)* (ELSE C_BRA_INT (statement)* C_BRA_END)?
;

forStmt:
    FOR (forClause | forRangeClause | expr)? C_BRA_INT (statement)* C_BRA_END
;

forClause:
    exprStmt? SEMICOLON expr? SEMICOLON exprStmt?
;

forRangeClause:
    (ID (COMMA ID)?) S_ASSIGN RANGE expr
;

returnStmt: 'return' expr? statementEnd;

block:
    C_BRA_INT (statement)* C_BRA_END
;

expr:
    (PLUS | MINUS | NOT) expr
    | expr (TIMES | OVER | MOD) expr
    | expr (PLUS | MINUS) expr
    | expr relation_op expr
    | expr AND expr
    | expr OR expr
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

statementEnd: SEMICOLON?;