package compiler;

import Go_Parser.Go_ParserBaseVisitor;
import Go_Parser.Go_Parser;
import compiler.tables.SymbolTable;
import compiler.tables.StringTable;
import compiler.tables.SymbolTableEntry;
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
    private boolean foundSemanticErrors;

    public GoSemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.stringTable = new StringTable();
        this.functionTable = new FunctionTable();
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

    private void reportSemanticError(int lineNumber, String message) {
        System.err.println("SEMANTIC ERROR (" + lineNumber + "): " + message);
        foundSemanticErrors = true;
    }

    // --- CONSTRUÇÕES ESPECÍFICAS DA LINGUAGEM GO ---

    /**
     * Override do visitador de programa para controlar melhor a visitação
     */
    @Override
    public Void visitProgramRule(Go_Parser.ProgramRuleContext ctx) {
        // Visitar apenas os statements que são relevantes para análise semântica
        for (Go_Parser.StatementContext stmtCtx : ctx.statement()) {
            visit(stmtCtx);
        }
        return null;
    }

    // --- MÉTODOS DE DECLARAÇÃO ---

    @Override
    public Void visitVarDecl(Go_Parser.VarDeclContext ctx) {
        visit(ctx.varSpec());
        return null;
    }

    @Override
    public Void visitVarSingleSpec(Go_Parser.VarSingleSpecContext ctx) {
        String varName = ctx.ID().getText();
        String varType = ctx.typeSpec().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(varName, varType, lineNumber)) {
            SymbolTableEntry existingEntry = symbolTable.getEntry(varName);
            reportSemanticError(lineNumber,
                    "variable '" + varName + "' already declared at line " + existingEntry.getDeclarationLine() + ".");
        }
        return super.visitVarSingleSpec(ctx);
    }

    @Override
    public Void visitVarMultiSpec(Go_Parser.VarMultiSpecContext ctx) {
        String varType = ctx.typeSpec().getText();
        for (int i = 0; i < ctx.ID().size(); i++) {
            String varName = ctx.ID(i).getText();
            int lineNumber = ctx.ID(i).getSymbol().getLine();

            if (!symbolTable.addEntry(varName, varType, lineNumber)) {
                SymbolTableEntry existingEntry = symbolTable.getEntry(varName);
                reportSemanticError(lineNumber,
                        "variable '" + varName + "' already declared at line " + existingEntry.getDeclarationLine()
                                + ".");
            }
        }
        return super.visitVarMultiSpec(ctx);
    }

    @Override
    public Void visitConstDecl(Go_Parser.ConstDeclContext ctx) {
        visit(ctx.constSpec());
        return null;
    }

    @Override
    public Void visitConstSingleSpec(Go_Parser.ConstSingleSpecContext ctx) {
        String constName = ctx.ID().getText();
        String constType = ctx.typeSpec().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(constName, constType, lineNumber)) {
            reportSemanticError(lineNumber,
                    "constant '" + constName + "' already declared at line "
                            + symbolTable.getEntry(constName).getDeclarationLine() + ".");
        }
        return super.visitConstSingleSpec(ctx);
    }

    @Override
    public Void visitConstMultiSpec(Go_Parser.ConstMultiSpecContext ctx) {
        String constType = ctx.typeSpec().getText();
        for (int i = 0; i < ctx.ID().size(); i++) {
            String constName = ctx.ID(i).getText();
            int lineNumber = ctx.ID(i).getSymbol().getLine();

            if (!symbolTable.addEntry(constName, constType, lineNumber)) {
                SymbolTableEntry existingEntry = symbolTable.getEntry(constName);
                reportSemanticError(lineNumber,
                        "constant '" + constName + "' already declared at line " + existingEntry.getDeclarationLine()
                                + ".");
            }
        }
        return super.visitConstMultiSpec(ctx);
    }

    @Override
    public Void visitShortDecl(Go_Parser.ShortDeclContext ctx) {
        // Declaração curta: lvalue S_ASSIGN expr
        String varName = ctx.lvalue().getText();
        String varType = "short"; // Tipo genérico para declaração curta
        int lineNumber = ctx.lvalue().getStart().getLine();

        if (!symbolTable.addEntry(varName, varType, lineNumber)) {
            SymbolTableEntry existingEntry = symbolTable.getEntry(varName);
            reportSemanticError(lineNumber,
                    "variable '" + varName + "' already declared at line " + existingEntry.getDeclarationLine() + ".");
        }

        // Visitar expressão de inicialização
        visit(ctx.expr());
        return null;
    }

    // --- ANÁLISE DE FUNÇÕES ---

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
        List<String> argTypes = new ArrayList<>();
        if (ctx.expressionList() != null) {
            Go_Parser.ExprListContext exprListCtx = (Go_Parser.ExprListContext) ctx.expressionList();
            for (Go_Parser.ExprContext exprCtx : exprListCtx.expr()) {
                String argType = determineExpressionType(exprCtx);
                argTypes.add(argType);
            }
        }

        // Verificar compatibilidade da chamada
        if (!functionTable.isValidCall(funcName, argTypes)) {
            FunctionInfo func = functionTable.getFunction(funcName);
            reportSemanticError(lineNumber,
                    "function call '" + funcName + "' has incompatible arguments. Expected: " + func.getSignature());
        }

        return super.visitCallExpression(ctx);
    }

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

    @Override
    public Void visitStringLiteral(Go_Parser.StringLiteralContext ctx) {
        String stringValueWithQuotes = ctx.STRINGF().getText();
        String stringContent = stringValueWithQuotes.substring(1, stringValueWithQuotes.length() - 1);
        stringTable.addString(stringContent);
        return super.visitStringLiteral(ctx);
    }

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

    @Override
    public Void visitIdLvalue(Go_Parser.IdLvalueContext ctx) {
        String varName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();
        if (!symbolTable.contains(varName)) {
            reportSemanticError(lineNumber, "variable '" + varName + "' was not declared.");
        }
        return super.visitIdLvalue(ctx);
    }

    @Override
    public Void visitArrayAccessLvalue(Go_Parser.ArrayAccessLvalueContext ctx) {
        return super.visitArrayAccessLvalue(ctx);
    }

    @Override
    public Void visitArrayIndex(Go_Parser.ArrayIndexContext ctx) {
        String varName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();
        if (!symbolTable.contains(varName)) {
            reportSemanticError(lineNumber, "variable '" + varName + "' was not declared.");
        }
        return super.visitArrayIndex(ctx);
    }
}