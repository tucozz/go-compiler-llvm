parser grammar Go_Parser;

options {
  tokenVocab = Go_Lexer;
}

// === PROGRAM STRUCTURE ===
program: 
    (declaration | functionDeclaration)* EOF                               #ProgramRule
;

// === DECLARATIONS ===
declaration:
    constDeclaration                                                        #ConstDeclarationStmt
    | typeDeclaration                                                       #TypeDeclarationStmt  
    | varDeclaration                                                        #VarDeclarationStmt
;

constDeclaration:
    CONST (constSpec | PAR_INT (constSpec statementEnd)* PAR_END) statementEnd   #ConstDecl
;

constSpec:
    identifierList (typeSpec)? ASSIGN expressionList                       #ConstSpecification
;

typeDeclaration:
    TYPE (typeSpec | PAR_INT (typeSpec statementEnd)* PAR_END) statementEnd      #TypeDecl
;

varDeclaration:
    VAR (varSpec | PAR_INT (varSpec statementEnd)* PAR_END) statementEnd         #VarDecl
;

varSpec:
    identifierList typeSpec (ASSIGN expressionList)?                       #VarSpecification
;

expressionList:
    expr (COMMA expr)*                                                      #ExprList
;

functionDeclaration: 
    FUNC ID signature block                                                 #FunctionDecl
;

parameterList: 
    parameter (COMMA parameter)*                                            #ParamList
;

parameter: 
    ID typeSpec                                                             #ParameterDeclaration
;

// === FUNCTION SIGNATURES ===
signature:
    parameters result?                                                      #FunctionSignature
;

parameters:
    PAR_INT (parameterList)? PAR_END                                        #ParametersDeclaration
;

result:
    typeSpec                                                                #ResultSingleType
    | parameters                                                            #ResultParameters
;

// === FUNCTION TYPES ===
functionType:
    FUNC signature                                                          #FunctionTypeDefinition
;
typeSpec:
    INT                                                                     #TypeInt
    | INT8                                                                  #TypeInt8
    | INT16                                                                 #TypeInt16
    | INT32                                                                 #TypeInt32
    | INT64                                                                 #TypeInt64
    | UINT                                                                  #TypeUint
    | UINT8                                                                 #TypeUint8
    | UINT16                                                                #TypeUint16
    | UINT32                                                                #TypeUint32
    | UINT64                                                                #TypeUint64
    | BOOL                                                                  #TypeBool
    | STRING                                                                #TypeString
    | FLOAT32                                                               #TypeFloat32
    | FLOAT64                                                               #TypeFloat64
    | BYTE                                                                  #TypeByte
    | RUNE                                                                  #TypeRune
    | ID                                                                    #CustomType
    | S_BRA_INT S_BRA_END typeSpec                                          #ArrayType
    | functionType                                                          #FunctionTypeSpec
;

statement:
    declaration                                                             #DeclarationStatement
    | simpleStmt                                                            #SimpleStatement
    | ifStmt                                                                #IfStatement
    | forStmt                                                               #ForStatement
    | returnStmt                                                            #ReturnStatement
    | block                                                                 #BlockStatement
;

// === SIMPLE STATEMENTS ===
simpleStmt:
    assignment                                                              #AssignmentSimpleStmt
    | shortDeclaration                                                      #ShortDeclSimpleStmt
    | inc_dec_stmt                                                          #IncDecSimpleStmt
    | expr statementEnd                                                     #ExpressionSimpleStmt
;

shortDeclaration:
    identifierList S_ASSIGN expressionList                                 #ShortVariableDecl
;

identifierList:
    ID (COMMA ID)*                                                          #IdentifierListRule
;

assignment:
    lvalue ASSIGN expr statementEnd                                         #AssignOpStatement
;

inc_dec_stmt: 
    lvalue (INC | DEC) statementEnd                                         #IncDecOperationStatement
;

ifStmt:
    IF expr block (ELSE (ifStmt | block))?                                  #IfElseStatement
;

forStmt:
    FOR (forClause | forRangeClause | expr)? block                          #ForLoopStatement
;

forClause:
    simpleStmt? SEMICOLON expr? SEMICOLON simpleStmt?                       #ForClassicClause
;

forRangeClause:
    (ID (COMMA ID)?) S_ASSIGN RANGE expr                                    #ForRangeClauseExpr
;

returnStmt: 
    RETURN expr? statementEnd                                               #ReturnStatementWithExpr
;

block:
    C_BRA_INT (statement)* C_BRA_END                                        #BlockCode
;

expr:
    (PLUS | MINUS | NOT) expr                                               #UnaryPrefixExpr
    | expr (TIMES | OVER | MOD) expr                                        #MultiplyDivideModExpr
    | expr (PLUS | MINUS) expr                                              #AddSubExpr
    | expr relation_op expr                                                 #ComparisonExpr
    | expr AND expr                                                         #LogicalANDExpr
    | expr OR expr                                                          #LogicalORExpr
    | primaryExpr (INC | DEC)?                                              #PrimaryOrPostfixExpr
;

primaryExpr:
    ID                                                                      #IdExpr
    | INT_LIT                                                               #IntLiteral
    | FLOAT_LIT                                                             #FloatLiteral
    | STRING_LIT                                                            #StringLiteral
    | KW_TRUE                                                               #TrueLiteral
    | KW_FALSE                                                              #FalseLiteral
    | PAR_INT expr PAR_END                                                  #ParenthesizedExpr
    | functionCall                                                          #FuncCallExpr
    | arrayAccess                                                           #ArrayAccessExpr
;

lvalue:
    ID                                                                      #IdLvalue
    | arrayAccess                                                           #ArrayAccessLvalue
;

functionCall: 
    ID PAR_INT (expressionList)? PAR_END                                    #CallExpression
;

arrayAccess: 
    ID S_BRA_INT expr S_BRA_END                                             #ArrayIndex
;

relation_op:
    EQUALS                                                                  #EqualsOperator
    | NOTEQUAL                                                              #NotEqualsOperator
    | GTHAN                                                                 #GreaterThanOperator
    | LTHAN                                                                 #LessThanOperator
    | GETHAN                                                                #GreaterThanEqualsOperator
    | LETHAN                                                                #LessThanEqualsOperator
;


statementEnd: SEMICOLON?;