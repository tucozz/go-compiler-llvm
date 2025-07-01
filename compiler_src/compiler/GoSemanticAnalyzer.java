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

    public boolean hasSemanticErrors() { return foundSemanticErrors; }
    public SymbolTable getSymbolTable() { return symbolTable; }
    public StringTable getStringTable() { return stringTable; }
    public FunctionTable getFunctionTable() { return functionTable; }

    private void reportSemanticError(int lineNumber, String message) {
        System.err.println("SEMANTIC ERROR (" + lineNumber + "): " + message);
        foundSemanticErrors = true;
    }

    // --- MÉTODOS DE DECLARAÇÃO ---

    @Override
    public Void visitVarDeclStatement(Go_Parser.VarDeclStatementContext ctx) {
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
                    "variable '" + varName + "' already declared at line " + existingEntry.getDeclarationLine() + ".");
            }
        }
        return super.visitVarMultiSpec(ctx);
    }

    @Override
    public Void visitConstDeclStatement(Go_Parser.ConstDeclStatementContext ctx) {
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
                "constant '" + constName + "' already declared at line " + symbolTable.getEntry(constName).getDeclarationLine() + ".");
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
                    "constant '" + constName + "' already declared at line " + existingEntry.getDeclarationLine() + ".");
            }
        }
        return super.visitConstMultiSpec(ctx);
    }

    @Override
    public Void visitTypeDeclStatement(Go_Parser.TypeDeclStatementContext ctx) {
        visit(ctx.typeSpecDecl());
        return null;
    }

    @Override
    public Void visitStructTypeDefinition(Go_Parser.StructTypeDefinitionContext ctx) {
        String typeName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(typeName, "struct " + typeName, lineNumber)) {
            reportSemanticError(lineNumber,
                "type '" + typeName + "' already declared at line " + symbolTable.getEntry(typeName).getDeclarationLine() + ".");
        }
        return super.visitStructTypeDefinition(ctx);
    }

    @Override
    public Void visitFieldDecl(Go_Parser.FieldDeclContext ctx) {
        String fieldName = ctx.ID().getText();
        String fieldType = ctx.typeSpec().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(fieldName, fieldType, lineNumber)) {
             reportSemanticError(lineNumber,
                "field '" + fieldName + "' already declared at line " + symbolTable.getEntry(fieldName).getDeclarationLine() + ".");
        }
        return super.visitFieldDecl(ctx);
    }

    @Override
    public Void visitParameterDeclaration(Go_Parser.ParameterDeclarationContext ctx) {
        String paramName = ctx.ID().getText();
        String paramType = ctx.typeSpec().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(paramName, paramType, lineNumber)) {
            reportSemanticError(lineNumber,
                "parameter '" + paramName + "' already declared at line " + symbolTable.getEntry(paramName).getDeclarationLine() + ".");
        }
        return super.visitParameterDeclaration(ctx);
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
            // Note: parameterList has #ParamList label, so we need to cast to get the parameter() method
            Go_Parser.ParamListContext paramListCtx = (Go_Parser.ParamListContext) ctx.parameterList();
            for (Go_Parser.ParameterContext paramCtx : paramListCtx.parameter()) {
                // The parameter context has #ParameterDeclaration label
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
            // Para simplificar, vamos assumir que todos os argumentos são do tipo correto
            // Em uma implementação real, precisaríamos determinar o tipo de cada expressão
            Go_Parser.ExprListContext exprListCtx = (Go_Parser.ExprListContext) ctx.expressionList();
            for (Go_Parser.ExprContext exprCtx : exprListCtx.expr()) {
                // Determinar tipo da expressão (simplificado)
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
    
    /**
     * Determina o tipo de uma expressão (versão simplificada)
     */
    private String determineExpressionType(Go_Parser.ExprContext ctx) {
        // Esta é uma implementação muito simplificada
        // Em uma versão completa, seria necessário analisar recursivamente a expressão
        
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

    // --- MÉTODOS DE USO/VERIFICAÇÃO ---

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
        if (!symbolTable.contains(varName)) {
            reportSemanticError(lineNumber, "variable '" + varName + "' was not declared.");
        }
        return super.visitIdExpr(ctx);
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

    // FIXED: ArrayAccessLvalue contains an arrayAccess rule, access it properly
    @Override
    public Void visitArrayAccessLvalue(Go_Parser.ArrayAccessLvalueContext ctx) {
        // This method is called for the lvalue rule with #ArrayAccessLvalue label
        // It contains an arrayAccess, so we need to visit it and let the 
        // visitArrayIndex method handle the ID checking
        return super.visitArrayAccessLvalue(ctx);
    }

    // FIXED: StructAccessLvalue contains a structAccess rule, access it properly  
    @Override
    public Void visitStructAccessLvalue(Go_Parser.StructAccessLvalueContext ctx) {
        // This method is called for the lvalue rule with #StructAccessLvalue label
        // It contains a structAccess, so we need to visit it and let the
        // visitStructFieldAccess method handle the ID checking
        return super.visitStructAccessLvalue(ctx);
    }

    // FIXED: Now accesses ID directly from ArrayIndexContext
    // Since #ArrayIndex is the direct label of the 'arrayAccess' rule
    @Override
    public Void visitArrayIndex(Go_Parser.ArrayIndexContext ctx) {
        // The 'arrayAccess' rule is 'ID S_BRA_INT expr S_BRA_END #ArrayIndex'
        // Since 'ArrayIndex' is the label OF THE RULE ITSELF 'arrayAccess',
        // the ID() is directly available in the context 'ctx'
        String varName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();
        if (!symbolTable.contains(varName)) {
            reportSemanticError(lineNumber, "variable '" + varName + "' was not declared.");
        }
        return super.visitArrayIndex(ctx);
    }

    // FIXED: Now accesses ID directly from StructFieldAccessContext
    // Since #StructFieldAccess is the direct label of the 'structAccess' rule
    @Override
    public Void visitStructFieldAccess(Go_Parser.StructFieldAccessContext ctx) {
        // The 'structAccess' rule is 'ID (DOT ID)+ #StructFieldAccess'
        // Since 'StructFieldAccess' is the label OF THE RULE ITSELF 'structAccess',
        // the ID(0) is directly available in the context 'ctx'
        String varName = ctx.ID(0).getText();
        int lineNumber = ctx.ID(0).getSymbol().getLine();
        if (!symbolTable.contains(varName)) {
            reportSemanticError(lineNumber, "variable '" + varName + "' was not declared.");
        }
        return super.visitStructFieldAccess(ctx);
    }
}