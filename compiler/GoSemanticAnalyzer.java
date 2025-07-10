package compiler;

import Go_Parser.Go_ParserBaseVisitor;
import Go_Parser.Go_Parser;
import compiler.tables.SymbolTable;
import compiler.tables.StringTable;
import compiler.tables.SymbolTableEntry;
import compiler.typing.GoType;
import compiler.typing.TypeTable;
import compiler.tables.FunctionTable;
import compiler.tables.FunctionInfo;
import java.util.List;
import java.util.ArrayList;

import java.util.ArrayList;
import java.util.List;

public class GoSemanticAnalyzer extends Go_ParserBaseVisitor<Void> {

    private SymbolTable symbolTable;
    private StringTable stringTable;
    private FunctionTable functionTable;
    private TypeTable typeTable;
    private boolean foundSemanticErrors;

    public GoSemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.stringTable = new StringTable();
        this.functionTable = new FunctionTable();
        this.typeTable = new TypeTable();
        this.foundSemanticErrors = false;
    }

    public boolean hasSemanticErrors() {
        return foundSemanticErrors;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public StringTable getStringTable() {
        return stringTable;
    }

    public FunctionTable getFunctionTable() {
        return functionTable;
    }

    public TypeTable getTypeTable() {
        return typeTable;
    }

    private void reportSemanticError(int lineNumber, String message) {
        System.err.println("SEMANTIC ERROR (" + lineNumber + "): " + message);
        foundSemanticErrors = true;
    }

    // --- CONSTRUÇÕES ESPECÍFICAS DA LINGUAGEM GO ---

    // Visita a regra program: (statement)* EOF
    @Override
    public Void visitProgramRule(Go_Parser.ProgramRuleContext ctx) {
        // Visitar apenas os statements que são relevantes para análise semântica
        for (Go_Parser.StatementContext stmtCtx : ctx.statement()) {
            visit(stmtCtx);
        }
        return null;
    }

    // --- MÉTODOS DE DECLARAÇÃO ---

    // Visita a regra varDeclaration: VAR varSpec statementEnd
    @Override
    public Void visitVarDecl(Go_Parser.VarDeclContext ctx) {
        visit(ctx.varSpec());
        return null;
    }

    // Visita a regra varSpec: ID typeSpec (ASSIGN expr)?
    @Override
    public Void visitVarSingleSpec(Go_Parser.VarSingleSpecContext ctx) {
        String varName = ctx.ID().getText();
        String varTypeStr = ctx.typeSpec().getText();
        GoType varType = GoType.fromString(varTypeStr);
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(varName, varTypeStr, lineNumber)) {
            SymbolTableEntry existingEntry = symbolTable.getEntry(varName);
            reportSemanticError(lineNumber,
                    "variable '" + varName + "' already declared at line " + existingEntry.getDeclarationLine() + ".");
        } else {
            // Adicionar à tabela de tipos
            typeTable.addVariable(varName, varType);
        }
        return super.visitVarSingleSpec(ctx);
    }

    // Visita a regra varSpec: ID (COMMA ID)* typeSpec ASSIGN expressionList
    @Override
    public Void visitVarMultiSpec(Go_Parser.VarMultiSpecContext ctx) {
        String varTypeStr = ctx.typeSpec().getText();
        GoType varType = GoType.fromString(varTypeStr);

        for (int i = 0; i < ctx.ID().size(); i++) {
            String varName = ctx.ID(i).getText();
            int lineNumber = ctx.ID(i).getSymbol().getLine();

            if (!symbolTable.addEntry(varName, varTypeStr, lineNumber)) {
                SymbolTableEntry existingEntry = symbolTable.getEntry(varName);
                reportSemanticError(lineNumber,
                        "variable '" + varName + "' already declared at line " + existingEntry.getDeclarationLine()
                                + ".");
            } else {
                // Adicionar à tabela de tipos
                typeTable.addVariable(varName, varType);  // <-- NOVA LINHA
            }
        }
        return super.visitVarMultiSpec(ctx);
    }

    // Visita a regra constDeclaration: CONST constSpec statementEnd
    @Override
    public Void visitConstDecl(Go_Parser.ConstDeclContext ctx) {
        visit(ctx.constSpec());
        return null;
    }

    // Visita a regra constSpec: ID typeSpec (ASSIGN expr)?
    @Override
    public Void visitConstSingleSpec(Go_Parser.ConstSingleSpecContext ctx) {
        String constName = ctx.ID().getText();
        String constTypeStr = ctx.typeSpec().getText();
        GoType constType = GoType.fromString(constTypeStr);
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(constName, constTypeStr, lineNumber)) {
            reportSemanticError(lineNumber,
                    "constant '" + constName + "' already declared at line "
                            + symbolTable.getEntry(constName).getDeclarationLine() + ".");
        } else {
            // Adicionar à tabela de tipos
            typeTable.addVariable(constName, constType);
        }
        return super.visitConstSingleSpec(ctx);
    }

    // Visita a regra constSpec: ID (COMMA ID)* typeSpec ASSIGN expressionList
    @Override
    public Void visitConstMultiSpec(Go_Parser.ConstMultiSpecContext ctx) {
        String constTypeStr = ctx.typeSpec().getText();
        GoType constType = GoType.fromString(constTypeStr);

        for (int i = 0; i < ctx.ID().size(); i++) {
            String constName = ctx.ID(i).getText();
            int lineNumber = ctx.ID(i).getSymbol().getLine();

            if (!symbolTable.addEntry(constName, constTypeStr, lineNumber)) {
                SymbolTableEntry existingEntry = symbolTable.getEntry(constName);
                reportSemanticError(lineNumber,
                        "constant '" + constName + "' already declared at line " + existingEntry.getDeclarationLine()
                                + ".");
            } else {
                // Adicionar à tabela de tipos
                typeTable.addVariable(constName, constType);  // <-- NOVA LINHA
            }
        }
        return super.visitConstMultiSpec(ctx);
    }

    // Visita a regra shortDeclaration: lvalue S_ASSIGN expr statementEnd
    @Override
    public Void visitShortDecl(Go_Parser.ShortDeclContext ctx) {
        // Declaração curta: lvalue S_ASSIGN expr
        String varName = ctx.lvalue().getText();
        int lineNumber = ctx.lvalue().getStart().getLine();

        // Visitar expressão primeiro para determinar o tipo
        visit(ctx.expr());

        // Tentar inferir o tipo da expressão
        GoType inferredType = determineExpressionGoType(ctx.expr());
        String varTypeStr = inferredType.getTypeName();

        if (!symbolTable.addEntry(varName, varTypeStr, lineNumber)) {
            SymbolTableEntry existingEntry = symbolTable.getEntry(varName);
            reportSemanticError(lineNumber,
                    "variable '" + varName + "' already declared at line " + existingEntry.getDeclarationLine() + ".");
        } else {
            // Adicionar à tabela de tipos
            typeTable.addVariable(varName, inferredType);
        }

        return null;
    }

    // --- ANÁLISE DE FUNÇÕES ---

    // Visita a regra functionDeclaration: FUNC ID PAR_INT (parameterList)? PAR_END (typeSpec)? C_BRA_INT (statement)* C_BRA_END
    @Override
    public Void visitFunctionDecl(Go_Parser.FunctionDeclContext ctx) {
        String funcName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        // Coletar parâmetros
        List<String> paramNames = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();

        if (ctx.parameterList() != null) {
            // Visitar cada parâmetro
            Go_Parser.ParamListContext paramListCtx = (Go_Parser.ParamListContext) ctx.parameterList();
            for (Go_Parser.ParameterContext paramCtx : paramListCtx.parameter()) {
                Go_Parser.ParameterDeclarationContext paramDeclCtx = (Go_Parser.ParameterDeclarationContext) paramCtx;
                String paramName = paramDeclCtx.ID().getText();
                String paramType = paramDeclCtx.typeSpec().getText();
                paramNames.add(paramName);
                paramTypes.add(paramType);
            }
        }

        // Determinar tipo de retorno
        String returnType = "void";
        if (ctx.typeSpec() != null) {
            returnType = ctx.typeSpec().getText();
        }

        // Adicionar função à tabela
        if (!functionTable.addFunction(funcName, paramNames, paramTypes, returnType, lineNumber)) {
            FunctionInfo existing = functionTable.getFunction(funcName);
            reportSemanticError(lineNumber,
                    "function '" + funcName + "' already declared at line " + existing.getDeclarationLine() + ".");
        } else {
            // Marcar como definida (já que tem corpo)
            functionTable.markAsDefined(funcName);
        }

        return super.visitFunctionDecl(ctx);
    }

    // Visita a regra parameter: ID typeSpec
    @Override
    public Void visitParameterDeclaration(Go_Parser.ParameterDeclarationContext ctx) {
        String paramName = ctx.ID().getText();
        String paramType = ctx.typeSpec().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(paramName, paramType, lineNumber)) {
            reportSemanticError(lineNumber,
                    "parameter '" + paramName + "' already declared at line "
                            + symbolTable.getEntry(paramName).getDeclarationLine() + ".");
        }
        return super.visitParameterDeclaration(ctx);
    }

    // Visita a regra functionCall: ID PAR_INT (expressionList)? PAR_END
    @Override
    public Void visitCallExpression(Go_Parser.CallExpressionContext ctx) {
        String funcName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        // Verificar se função existe
        if (!functionTable.hasFunction(funcName)) {
            reportSemanticError(lineNumber, "function '" + funcName + "' is not declared.");
            return super.visitCallExpression(ctx);
        }

        // Coletar tipos de argumentos
        List<GoType> argTypes = new ArrayList<>();
        if (ctx.expressionList() != null) {
            Go_Parser.ExprListContext exprListCtx = (Go_Parser.ExprListContext) ctx.expressionList();
            for (Go_Parser.ExprContext exprCtx : exprListCtx.expr()) {
                GoType argType = determineExpressionGoType(exprCtx);
                argTypes.add(argType);
            }
        }

        // Verificar compatibilidade da chamada
        FunctionInfo func = functionTable.getFunction(funcName);
        if (!func.isCallCompatible(argTypes)) {
            reportSemanticError(lineNumber,
                    "function call '" + funcName + "' has incompatible arguments. Expected: " + func.getSignature());
        }

        return super.visitCallExpression(ctx);
    }

    // Visita a regra assignment: lvalue ASSIGN expr statementEnd
    @Override
    public Void visitAssignOpStatement(Go_Parser.AssignOpStatementContext ctx) {
        // Para atribuição (=), apenas verificar se a variável existe
        // A verificação será feita automaticamente pelo visitIdLvalue
        return super.visitAssignOpStatement(ctx);
    }

    /**
     * Determina o tipo de uma expressão (versão simplificada)
     */
    private String determineExpressionType(Go_Parser.ExprContext ctx) {
        // Verificar se é um literal
        if (ctx instanceof Go_Parser.PrimaryOrPostfixExprContext) {
            Go_Parser.PrimaryOrPostfixExprContext primary = (Go_Parser.PrimaryOrPostfixExprContext) ctx;
            if (primary.primaryExpr() instanceof Go_Parser.IntLiteralContext) {
                return "int";
            } else if (primary.primaryExpr() instanceof Go_Parser.StringLiteralContext) {
                return "string";
            } else if (primary.primaryExpr() instanceof Go_Parser.TrueLiteralContext ||
                    primary.primaryExpr() instanceof Go_Parser.FalseLiteralContext) {
                return "bool";
            } else if (primary.primaryExpr() instanceof Go_Parser.RealLiteralContext) {
                return "float64";
            } else if (primary.primaryExpr() instanceof Go_Parser.IdExprContext) {
                // Buscar tipo da variável na tabela de símbolos
                String varName = ((Go_Parser.IdExprContext) primary.primaryExpr()).ID().getText();
                SymbolTableEntry entry = symbolTable.getEntry(varName);
                if (entry != null) {
                    return entry.getType();
                }
                return "unknown";
            }
        }
        // Para outros tipos de expressão, retornar um tipo padrão
        return "unknown";
    }

    @Override
    public Void visitIfElseStatement(Go_Parser.IfElseStatementContext ctx) {
        // Verificar condição principal do IF
        String condType = determineExpressionType(ctx.expr(0));
        if (!condType.equals("bool") && !condType.equals("unknown")) {
            // Tentar obter linha da expressão
            reportSemanticError(1, "if condition must be boolean, got: " + condType);
        }

        // Verificar condições dos ELSE IF (se existirem)
        if (ctx.expr().size() > 1) {
            for (int i = 1; i < ctx.expr().size(); i++) {
                String elseIfCondType = determineExpressionType(ctx.expr(i));
                if (!elseIfCondType.equals("bool") && !elseIfCondType.equals("unknown")) {
                    reportSemanticError(1, "else if condition must be boolean, got: " + elseIfCondType);
                }
            }
        }
        // Visitar todos os filhos automaticamente
        return super.visitIfElseStatement(ctx);
    }

    // Visita a regra primaryExpr: STRINGF
    @Override
    public Void visitStringLiteral(Go_Parser.StringLiteralContext ctx) {
        String stringValueWithQuotes = ctx.STRINGF().getText();
        String stringContent = stringValueWithQuotes.substring(1, stringValueWithQuotes.length() - 1);
        stringTable.addString(stringContent);
        return super.visitStringLiteral(ctx);
    }

    // Visita a regra primaryExpr: ID
    @Override
    public Void visitIdExpr(Go_Parser.IdExprContext ctx) {
        String varName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        // Ignorar palavras-chave e construções especiais da linguagem Go
        if (isGoKeyword(varName)) {
            return null; // Não processar como variável
        }

        // Verificar se é uma função conhecida
        if (functionTable.hasFunction(varName)) {
            return null; // Função existe, OK
        }

        // Verificar se variável foi declarada
        if (!symbolTable.contains(varName)) {
            reportSemanticError(lineNumber, "variable '" + varName + "' was not declared.");
        }
        return super.visitIdExpr(ctx);
    }

    /**
     * Verifica se um identificador é uma palavra-chave ou construção especial do Go
     */
    private boolean isGoKeyword(String identifier) {
        // Palavras-chave da linguagem Go que não devem ser tratadas como variáveis
        String[] goKeywords = {
                "package", "import", "func", "var", "const", "type",
                "if", "else", "for", "switch", "case", "default", "return",
                "break", "continue", "go", "defer", "select", "chan", "range",
                "interface", "map", "make", "new", "append", "len", "cap",
                "copy", "delete", "panic", "recover", "print", "println",
                "true", "false", "nil", "iota",
                "main", "fmt" // Nomes especiais comuns
        };

        for (String keyword : goKeywords) {
            if (keyword.equals(identifier)) {
                return true;
            }
        }
        return false;
    }

    // Visita a regra lvalue: ID
    @Override
    public Void visitIdLvalue(Go_Parser.IdLvalueContext ctx) {
        String varName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();
        if (!symbolTable.contains(varName)) {
            reportSemanticError(lineNumber, "variable '" + varName + "' was not declared.");
        }
        return super.visitIdLvalue(ctx);
    }

    // Visita a regra lvalue: arrayAccess
    @Override
    public Void visitArrayAccessLvalue(Go_Parser.ArrayAccessLvalueContext ctx) {
        return super.visitArrayAccessLvalue(ctx);
    }

    // Visita a regra arrayAccess: ID S_BRA_INT expr S_BRA_END
    @Override
    public Void visitArrayIndex(Go_Parser.ArrayIndexContext ctx) {
        String varName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();
        if (!symbolTable.contains(varName)) {
            reportSemanticError(lineNumber, "variable '" + varName + "' was not declared.");
        }
        return super.visitArrayIndex(ctx);
    }

    // Visita a regra expr: expr (PLUS | MINUS) expr
    @Override
    public Void visitAddSubExpr(Go_Parser.AddSubExprContext ctx) {
        GoType leftType = determineExpressionGoType(ctx.expr(0));
        GoType rightType = determineExpressionGoType(ctx.expr(1));
        
        String operator = ctx.getChild(1).getText(); // + ou -
        GoType resultType;
        
        if (operator.equals("+")) {
            resultType = leftType.unifyPlus(rightType);
        } else {
            resultType = leftType.unifyArithmetic(rightType);
        }
        
        if (resultType == GoType.NO_TYPE) {
            int lineNumber = ctx.getStart().getLine();
            reportSemanticError(lineNumber, 
                "invalid operation: " + operator + " between " + leftType + " and " + rightType);
        }
        
        return super.visitAddSubExpr(ctx);
    }

    // Visita a regra expr: expr (TIMES | OVER | MOD) expr
    @Override
    public Void visitMultiplyDivideModExpr(Go_Parser.MultiplyDivideModExprContext ctx) {
        GoType leftType = determineExpressionGoType(ctx.expr(0));
        GoType rightType = determineExpressionGoType(ctx.expr(1));
        
        GoType resultType = leftType.unifyArithmetic(rightType);
        
        if (resultType == GoType.NO_TYPE) {
            int lineNumber = ctx.getStart().getLine();
            String operator = ctx.getChild(1).getText();
            reportSemanticError(lineNumber, 
                "invalid operation: " + operator + " between " + leftType + " and " + rightType);
        }
        
        return super.visitMultiplyDivideModExpr(ctx);
    }

    // Visita a regra expr: expr relation_op expr
    @Override
    public Void visitComparisonExpr(Go_Parser.ComparisonExprContext ctx) {
        GoType leftType = determineExpressionGoType(ctx.expr(0));
        GoType rightType = determineExpressionGoType(ctx.expr(1));
        
        GoType resultType = leftType.unifyComparison(rightType);
        
        if (resultType == GoType.NO_TYPE) {
            int lineNumber = ctx.getStart().getLine();
            String operator = ctx.relation_op().getText();
            reportSemanticError(lineNumber, 
                "invalid comparison: " + operator + " between " + leftType + " and " + rightType);
        }
        
        return super.visitComparisonExpr(ctx);
    }

    // Visita a regra expr: expr AND expr
    @Override
    public Void visitLogicalANDExpr(Go_Parser.LogicalANDExprContext ctx) {
        GoType leftType = determineExpressionGoType(ctx.expr(0));
        GoType rightType = determineExpressionGoType(ctx.expr(1));
        
        GoType resultType = leftType.unifyLogical(rightType);
        
        if (resultType == GoType.NO_TYPE) {
            int lineNumber = ctx.getStart().getLine();
            reportSemanticError(lineNumber, 
                "invalid logical operation: && between " + leftType + " and " + rightType);
        }
        
        return super.visitLogicalANDExpr(ctx);
    }

    // Visita a regra expr: expr OR expr
    @Override
    public Void visitLogicalORExpr(Go_Parser.LogicalORExprContext ctx) {
        GoType leftType = determineExpressionGoType(ctx.expr(0));
        GoType rightType = determineExpressionGoType(ctx.expr(1));
        
        GoType resultType = leftType.unifyLogical(rightType);
        
        if (resultType == GoType.NO_TYPE) {
            int lineNumber = ctx.getStart().getLine();
            reportSemanticError(lineNumber, 
                "invalid logical operation: || between " + leftType + " and " + rightType);
        }
        
        return super.visitLogicalORExpr(ctx);
    }

    // Visita a regra expr: (PLUS | MINUS | NOT) expr
    @Override
    public Void visitUnaryPrefixExpr(Go_Parser.UnaryPrefixExprContext ctx) {
        GoType exprType = determineExpressionGoType(ctx.expr());
        String operator = ctx.getChild(0).getText();
        
        // Verificar se operador unário é válido para o tipo
        if (operator.equals("+") || operator.equals("-")) {
            if (!exprType.isNumeric()) {
                int lineNumber = ctx.getStart().getLine();
                reportSemanticError(lineNumber, 
                    "invalid operation: unary " + operator + " on " + exprType);
            }
        } else if (operator.equals("!")) {
            if (!exprType.isBooleanContext()) {
                int lineNumber = ctx.getStart().getLine();
                reportSemanticError(lineNumber, 
                    "invalid operation: unary ! on " + exprType + " (expected bool)");
            }
        }
        
        return super.visitUnaryPrefixExpr(ctx);
    }

    /**
     * Determina o tipo GoType de uma expressão (versão melhorada)
     */
    private GoType determineExpressionGoType(Go_Parser.ExprContext ctx) {
        if (ctx instanceof Go_Parser.PrimaryOrPostfixExprContext) {
            Go_Parser.PrimaryOrPostfixExprContext primary = (Go_Parser.PrimaryOrPostfixExprContext) ctx;
            if (primary.primaryExpr() instanceof Go_Parser.IntLiteralContext) {
                return GoType.INT;
            } else if (primary.primaryExpr() instanceof Go_Parser.StringLiteralContext) {
                return GoType.STRING;
            } else if (primary.primaryExpr() instanceof Go_Parser.TrueLiteralContext ||
                    primary.primaryExpr() instanceof Go_Parser.FalseLiteralContext) {
                return GoType.BOOL;
            } else if (primary.primaryExpr() instanceof Go_Parser.RealLiteralContext) {
                return GoType.FLOAT64;
            } else if (primary.primaryExpr() instanceof Go_Parser.IdExprContext) {
                // Buscar tipo da variável na tabela de tipos
                String varName = ((Go_Parser.IdExprContext) primary.primaryExpr()).ID().getText();
                GoType varType = typeTable.getVariableType(varName);
                return varType != null ? varType : GoType.UNKNOWN;
            }
        } else if (ctx instanceof Go_Parser.AddSubExprContext) {
            // Para expressões binárias, calcular o tipo resultado
            Go_Parser.AddSubExprContext addSub = (Go_Parser.AddSubExprContext) ctx;
            GoType leftType = determineExpressionGoType(addSub.expr(0));
            GoType rightType = determineExpressionGoType(addSub.expr(1));
            String operator = addSub.getChild(1).getText();
            
            if (operator.equals("+")) {
                return leftType.unifyPlus(rightType);
            } else {
                return leftType.unifyArithmetic(rightType);
            }
        } else if (ctx instanceof Go_Parser.MultiplyDivideModExprContext) {
            Go_Parser.MultiplyDivideModExprContext multDiv = (Go_Parser.MultiplyDivideModExprContext) ctx;
            GoType leftType = determineExpressionGoType(multDiv.expr(0));
            GoType rightType = determineExpressionGoType(multDiv.expr(1));
            return leftType.unifyArithmetic(rightType);
        } else if (ctx instanceof Go_Parser.ComparisonExprContext) {
            return GoType.BOOL; // Comparações sempre retornam bool
        } else if (ctx instanceof Go_Parser.LogicalANDExprContext || ctx instanceof Go_Parser.LogicalORExprContext) {
            return GoType.BOOL; // Operações lógicas sempre retornam bool
        }
        
        return GoType.UNKNOWN;
    }
}