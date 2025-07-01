parser grammar Go_Parser;

options {
  tokenVocab = Go_Lexer;
}

program: 
    (statement)* EOF #ProgramRule
;

declaration:
    varDeclaration 
    | constDeclaration
    | typeDeclaration 
    | functionDeclaration 
;

varDeclaration:
    VAR varSpec statementEnd #VarDecl
;

constDeclaration:
    CONST constSpec statementEnd #ConstDecl
;

varSpec:
    ID typeSpec (ASSIGN expr)?              #VarSingleSpec
    | ID (COMMA ID)* typeSpec ASSIGN expressionList #VarMultiSpec
;

constSpec:
    ID typeSpec (ASSIGN expr)?              #ConstSingleSpec
    | ID (COMMA ID)* typeSpec ASSIGN expressionList #ConstMultiSpec
;

expressionList:
    expr (COMMA expr)* #ExprList
;

typeDeclaration: 
    TYPE typeSpecDecl statementEnd #TypeDecl
;

typeSpecDecl:
    ID structType #StructTypeDefinition
;

structType: 
    STRUCT C_BRA_INT (fieldDeclaration)* C_BRA_END #StructLiteral
;

fieldDeclaration: 
    ID typeSpec statementEnd #FieldDecl
;

functionDeclaration: 
    FUNC ID PAR_INT (parameterList)? PAR_END (typeSpec)? C_BRA_INT (statement)* C_BRA_END #FunctionDecl
;

parameterList: 
    parameter (COMMA parameter)* #ParamList
;

parameter: 
    ID typeSpec #ParameterDeclaration
;

typeSpec:
    INT #TypeInt
    | INT8 #TypeInt8
    | INT16 #TypeInt16
    | INT32 #TypeInt32
    | INT64 #TypeInt64
    | UINT #TypeUint
    | UINT8 #TypeUint8
    | UINT16 #TypeUint16
    | UINT32 #TypeUint32
    | UINT64 #TypeUint64
    | BOOL #TypeBool
    | STRING #TypeString
    | FLOAT32 #TypeFloat32
    | FLOAT64 #TypeFloat64
    | ID #CustomType
    | S_BRA_INT S_BRA_END typeSpec #ArrayType
;

statement:
    declaration #DeclarationStatement
    | assignment #AssignmentStatement
    | inc_dec_stmt #IncDecStatement
    | ifStmt #IfStatement
    | forStmt #ForStatement
    | returnStmt #ReturnStatement
    | block #BlockStatement
    | exprStmt #ExpressionStatement
;

exprStmt: 
    expr statementEnd #ExpressionOnlyStatement
;

assignment:
    lvalue ASSIGN expr statementEnd #AssignOpStatement
    | lvalue S_ASSIGN expr statementEnd #ShortAssignOpStatement
;

inc_dec_stmt: 
    lvalue (INC | DEC) statementEnd #IncDecOperationStatement
;

ifStmt:
    IF expr C_BRA_INT (statement)* C_BRA_END (ELSEIF expr C_BRA_INT (statement)* C_BRA_END)* (ELSE C_BRA_INT (statement)* C_BRA_END)? #IfElseStatement
;

forStmt:
    FOR (forClause | forRangeClause | expr)? C_BRA_INT (statement)* C_BRA_END #ForLoopStatement
;

forClause:
    exprStmt? SEMICOLON expr? SEMICOLON exprStmt? #ForClassicClause
;

forRangeClause:
    (ID (COMMA ID)?) S_ASSIGN RANGE expr #ForRangeClauseExpr
;

returnStmt: 
    'return' expr? statementEnd #ReturnStatementWithExpr
;

block:
    C_BRA_INT (statement)* C_BRA_END #BlockCode
;

expr:
    (PLUS | MINUS | NOT) expr #UnaryPrefixExpr
    | expr (TIMES | OVER | MOD) expr #MultiplyDivideModExpr
    | expr (PLUS | MINUS) expr #AddSubExpr
    | expr relation_op expr #ComparisonExpr
    | expr AND expr #LogicalANDExpr
    | expr OR expr #LogicalORExpr
    | primaryExpr (INC | DEC)? #PrimaryOrPostfixExpr
;

primaryExpr:
    ID #IdExpr
    | POS_INT #IntLiteral
    | NEG_INT #NegIntLiteral
    | POS_REAL #RealLiteral
    | NEG_REAL #NegRealLiteral
    | STRINGF #StringLiteral
    | KW_TRUE #TrueLiteral
    | KW_FALSE #FalseLiteral
    | PAR_INT expr PAR_END #ParenthesizedExpr
    | functionCall #FuncCallExpr
    | arrayAccess #ArrayAccessExpr
    | structAccess #StructAccessExpr
;

lvalue:
    ID #IdLvalue
    | arrayAccess #ArrayAccessLvalue
    | structAccess #StructAccessLvalue
;

functionCall: 
    ID PAR_INT (expressionList)? PAR_END #CallExpression
;

arrayAccess: 
    ID S_BRA_INT expr S_BRA_END #ArrayIndex
;

structAccess: 
    ID (DOT ID)+ #StructFieldAccess
;

relation_op:
    EQUALS #EqualsOperator
    | NOTEQUAL #NotEqualsOperator
    | GTHAN #GreaterThanOperator
    | LTHAN #LessThanOperator
    | GETHAN #GreaterThanEqualsOperator
    | LETHAN #LessThanEqualsOperator
;

statementEnd: SEMICOLON?;