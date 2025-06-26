package compiler;

import Go_Parser.Go_ParserBaseVisitor;
import Go_Parser.Go_Parser;
import compiler.tables.SymbolTable;
import compiler.tables.StringTable;
import compiler.tables.SymbolTableEntry;

public class GoSemanticAnalyzer extends Go_ParserBaseVisitor<Void> {

    private SymbolTable symbolTable;
    private StringTable stringTable;
    private boolean foundSemanticErrors;

    public GoSemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.stringTable = new StringTable();
        this.foundSemanticErrors = false;
    }

    public boolean hasSemanticErrors() { return foundSemanticErrors; }
    public SymbolTable getSymbolTable() { return symbolTable; }
    public StringTable getStringTable() { return stringTable; }

    private void reportSemanticError(int lineNumber, String message) {
        System.err.println("SEMANTIC ERROR (" + lineNumber + "): " + message);
        foundSemanticErrors = true;
    }

    // --- MÉTODOS DE DECLARAÇÃO ---

    @Override
    public Void visitVarDeclaration(Go_Parser.VarDeclarationContext ctx) {
        visit(ctx.varSpec());
        return null; // A visitação dos filhos é feita pelo visit(ctx.varSpec())
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
    public Void visitConstDeclaration(Go_Parser.ConstDeclarationContext ctx) {
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
    public Void visitTypeDeclaration(Go_Parser.TypeDeclarationContext ctx) {
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
    public Void visitFieldDeclaration(Go_Parser.FieldDeclarationContext ctx) {
        String fieldName = ctx.ID().getText();
        String fieldType = ctx.typeSpec().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(fieldName, fieldType, lineNumber)) {
             reportSemanticError(lineNumber,
                "field '" + fieldName + "' already declared at line " + symbolTable.getEntry(fieldName).getDeclarationLine() + ".");
        }
        return super.visitFieldDeclaration(ctx);
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

    @Override
    public Void visitFunctionDeclaration(Go_Parser.FunctionDeclarationContext ctx) {
        String funcName = ctx.ID().getText();
        String returnType = ctx.typeSpec() != null ? ctx.typeSpec().getText() : "void";
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.addEntry(funcName, "func (" + returnType + ")", lineNumber)) {
             reportSemanticError(lineNumber,
                "function '" + funcName + "' already declared at line " + symbolTable.getEntry(funcName).getDeclarationLine() + ".");
        }
        return super.visitFunctionDeclaration(ctx);
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

    @Override
    public Void visitCallExpression(Go_Parser.CallExpressionContext ctx) {
        String funcName = ctx.ID().getText();
        int lineNumber = ctx.ID().getSymbol().getLine();

        if (!symbolTable.contains(funcName)) {
            reportSemanticError(lineNumber, "function or variable '" + funcName + "' was not declared.");
        }
        return super.visitCallExpression(ctx);
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