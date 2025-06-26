parser grammar Go_Parser;

options {
  tokenVocab = Go_Lexer;
}

// -------------------------------------------------------------------
// Regra de Início
// -------------------------------------------------------------------

// Programa: sequência de declarações de nível superior
program: topLevelDecl* EOF;

// -------------------------------------------------------------------
// Declarações de Nível Superior
// -------------------------------------------------------------------

topLevelDecl:
    varDecl     #TopLevelVarDecl
    | constDecl #TopLevelConstDecl
    | typeDecl  #TopLevelTypeDecl
    | funcDecl  #TopLevelFuncDecl
;

// Declarações (agora sem SEMICOLON explícito, usa statementEnd)
varDecl:
    VAR varSpec statementEnd #VarDeclaration
;

constDecl:
    CONST constSpec statementEnd #ConstDeclaration
;

typeDecl:
    TYPE typeSpecDecl statementEnd #TypeDeclaration
;

funcDecl:
    FUNC ID PAR_INT (parameterList)? PAR_END (typeSpec)? block #FunctionDeclaration
;

// -------------------------------------------------------------------
// Especificações para Declarações
// -------------------------------------------------------------------

varSpec:
    ID typeSpec (ASSIGN expr)?              #VarSingleSpec // Ex: var x int = 10
    | ID (COMMA ID)* typeSpec ASSIGN exprList #VarMultiSpec  // Ex: var x, y int = 10, 20
;

constSpec:
    ID typeSpec (ASSIGN expr)?              #ConstSingleSpec // Ex: const PI = 3.14
    | ID (COMMA ID)* typeSpec ASSIGN exprList #ConstMultiSpec  // Ex: const A, B = 1, 2
;

typeSpecDecl:
    ID structType #StructTypeDefinition
;

structType:
    STRUCT C_BRA_INT (fieldDecl)* C_BRA_END #StructLiteral
;

fieldDecl:
    ID typeSpec statementEnd #FieldDeclaration // Campo da struct também usa statementEnd
;

parameterList:
    parameter (COMMA parameter)*
;

parameter:
    ID typeSpec #ParameterDeclaration
;

// ATENÇÃO: Corrigido o erro (122) para 'typeSpec'
// Todas as alternativas agora têm rótulos
typeSpec:
    INT        #TypeInt
    | INT8     #TypeInt8
    | INT16    #TypeInt16
    | INT32    #TypeInt32
    | INT64    #TypeInt64
    | UINT     #TypeUint
    | UINT8    #TypeUint8
    | UINT16   #TypeUint16
    | UINT32   #TypeUint32
    | UINT64   #TypeUint64
    | BOOL     #TypeBool
    | STRING   #TypeString
    | FLOAT32  #TypeFloat32
    | FLOAT64  #TypeFloat64
    | ID             #CustomType // Para tipos definidos pelo usuário
    | S_BRA_INT S_BRA_END typeSpec #ArrayType // Para arrays, e.g., []int
;

// -------------------------------------------------------------------
// Instruções (Statements)
// -------------------------------------------------------------------

stmt:
    simpleStmt      #SimpleStatement
    | block         #BlockStatement
    | ifStmt        #IfStatement
    | switchStmt    #SwitchStatement
    | forStmt       #ForStatement
    | returnStmt    #ReturnStatement
    | continueStmt  #ContinueStatement
    | fallthroughStmt #FallthroughStatement
;

simpleStmt:
    emptyStmt   #EmptyStatement // Representa um ';' explícito sozinho
    | exprStmt  #ExpressionStatement
    | assignStmt #AssignmentStatement
    | incDecStmt #IncDecStatement
;

emptyStmt:
    SEMICOLON #ExplicitSemicolon
;

// A expressão como instrução termina com statementEnd
exprStmt:
    expr statementEnd #ExpressionOnlyStatement
;

// Atribuições agora usam statementEnd
assignStmt:
    lvalue ASSIGN expr statementEnd     #AssignOpStatement
    | lvalue S_ASSIGN expr statementEnd #ShortAssignOpStatement
;

// Inc/Dec agora usam statementEnd
incDecStmt:
    lvalue (INC | DEC) statementEnd #IncDecOperationStatement
;

ifStmt:
    IF expr block (ELSEIF expr block)* (ELSE block)? #IfElseStatement
;

switchStmt:
    SWITCH (expr)? C_BRA_INT (caseClause)* (defaultClause)? C_BRA_END #SwitchStatementFull
;

caseClause:
    CASE exprList COLON (stmt)* #CaseCondition
;

defaultClause:
    DEFAULT COLON (stmt)* #DefaultCase
;

forStmt:
    FOR (forClause | forRangeClause | expr)? block #ForLoopStatement // 'expr' para for condition { }
;

// forClause AINDA MANTÉM OS SEMICOLONS EXPLÍCITOS (parte da sintaxe do Go)
forClause:
    simpleStmt? SEMICOLON expr? SEMICOLON simpleStmt? #ForClassicClause
;

// ATENÇÃO: Corrigido o erro (124) de conflito com 'forRangeClause'
forRangeClause:
    (lvalue (COMMA lvalue)?) S_ASSIGN RANGE expr #ForRangeClauseExpr // Renomeado o rótulo
;

returnStmt:
    RETURN expr? statementEnd #ReturnStatementWithExpr
;

block:
    C_BRA_INT (stmt)* C_BRA_END #BlockCode
;

continueStmt:
    CONTINUE statementEnd #ContinueJump
;

fallthroughStmt:
    FALLT statementEnd #FallthroughJump
;

// -------------------------------------------------------------------
// Expressões (Estilo "OR" para precedência)
// -------------------------------------------------------------------

expr:
    orExpr
;

orExpr:
    andExpr (OR andExpr)* #LogicalORExpr
;

andExpr:
    relationExpr (AND relationExpr)* #LogicalANDExpr
;

// ATENÇÃO: Corrigido o erro (124) de conflito com 'relationOp'
relationExpr:
    addSubtractExpr (relationOp addSubtractExpr)* #ComparisonExpr // Renomeado o rótulo
;

addSubtractExpr:
    mulDivModExpr ((PLUS | MINUS) mulDivModExpr)* #AddSubExpr
;

// ATENÇÃO: Corrigido o erro (124) de conflito com 'mulDivModExpr'
mulDivModExpr:
    unaryOpExpr ((TIMES | OVER | MOD) unaryOpExpr)* #MultiplyDivideModExpr // Renomeado o rótulo
;

unaryOpExpr:
    (PLUS | MINUS | NOT) unaryOpExpr #UnaryPrefixExpr // Unários pré-fixados
    | primaryExpr (INC | DEC)? #PrimaryOrPostfixExpr // Expressões primárias e pós-fixados
;

primaryExpr:
    ID          #IdExpr
    | POS_INT   #IntLiteral
    | NEG_INT   #NegIntLiteral
    | POS_REAL  #RealLiteral
    | NEG_REAL  #NegRealLiteral
    | STRINGF   #StringLiteral
    | KW_TRUE   #TrueLiteral
    | KW_FALSE  #FalseLiteral
    | PAR_INT expr PAR_END #ParenthesizedExpr
    | functionCall #FuncCallExpr
    | arrayAccess  #ArrayAccessExpr
    | structAccess #StructAccessExpr
;

lvalue:
    ID          #IdLvalue
    | arrayAccess #ArrayAccessLvalue
    | structAccess #StructAccessLvalue
;

// -------------------------------------------------------------------
// Regras Auxiliares de Expressão
// -------------------------------------------------------------------

functionCall:
    ID PAR_INT (exprList)? PAR_END #CallExpression
;

exprList:
    expr (COMMA expr)* #ExpressionList
;

arrayAccess:
    ID S_BRA_INT expr S_BRA_END #ArrayIndex
;

structAccess:
    ID (DOT ID)+ #StructFieldAccess
;

// 'relationOp' (com 'Op' no final) é o nome da regra.
// Os rótulos das alternativas são para diferenciar cada operador.
relationOp:
    EQUALS      #EqualsOperator
    | NOTEQUAL  #NotEqualsOperator
    | GTHAN     #GreaterThanOperator
    | LTHAN     #LessThanOperator
    | GETHAN    #GreaterThanEqualsOperator
    | LETHAN    #LessThanEqualsOperator
;

// -------------------------------------------------------------------
// Regra para o fim de uma instrução/declaração (permite ASI)
// -------------------------------------------------------------------
statementEnd: SEMICOLON?;