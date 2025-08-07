package compiler.checker;

import Go_Parser.Go_ParserBaseVisitor;
import Go_Parser.Go_Parser;
import compiler.ast.AST;
import compiler.ast.NodeKind;
import compiler.tables.ArrayInfo;
import compiler.tables.ArrayTable;
import compiler.tables.FunctionInfo;
import compiler.tables.FunctionTable;
import compiler.tables.StrTable;
import compiler.tables.VarTable;
import compiler.tables.VarTable.VarEntry;
import compiler.tables.StrTable.StrEntry;
import compiler.typing.GoType;
import compiler.typing.TypeTable;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * GoSemanticChecker agora constrói uma Abstract Syntax Tree (AST).
 * Cada método 'visit' retorna um nó da AST (ou uma sub-árvore).
 * A análise semântica (verificação de tipos, escopos, etc.) é feita durante a construção.
 */
public class GoSemanticChecker extends Go_ParserBaseVisitor<AST> {

    private final VarTable varTable;
    private final StrTable stringTable;
    private final FunctionTable functionTable;
    private final ArrayTable arrayTable;
    private final TypeTable typeTable;
    private boolean foundSemanticErrors;

    // Lista para manter todas as variáveis processadas para o relatório final
    private List<VarTable.VarEntry> allProcessedVariables;

    // Estado para rastreamento da função atual
    private GoType currentFunctionReturnType;
    private String currentFunctionName;

    // Estado para rastreamento de loops (para break/continue)
    private int loopDepth;

    public GoSemanticChecker() {
        this.varTable = new VarTable();
        this.stringTable = new StrTable();
        this.functionTable = new FunctionTable();
        this.arrayTable = new ArrayTable();
        this.typeTable = new TypeTable();
        this.foundSemanticErrors = false;
        this.currentFunctionReturnType = null;
        this.currentFunctionName = null;
        this.loopDepth = 0;
        this.allProcessedVariables = new ArrayList<>();
    }

    public boolean hasSemanticErrors() {
        return foundSemanticErrors;
    }
    
    public StrTable getStringTable() {
        return this.stringTable;
    }

    private void reportSemanticError(String message) {
        System.err.println("SEMANTIC ERROR: " + message);
        foundSemanticErrors = true;
    }

    // --- NÓ RAIZ DO PROGRAMA ---

    @Override
    public AST visitProgramRule(Go_Parser.ProgramRuleContext ctx) {
        AST programNode = new AST(NodeKind.PROGRAM_NODE, GoType.NO_TYPE);
        for (Go_Parser.StatementContext stmtCtx : ctx.statement()) {
            programNode.addChild(visit(stmtCtx));
        }
        return programNode;
    }

    // --- BLOCOS DE CÓDIGO ---

    @Override
    public AST visitBlockCode(Go_Parser.BlockCodeContext ctx) {
        varTable.enterScope();
        AST blockNode = new AST(NodeKind.BLOCK_NODE, GoType.NO_TYPE);
        if (ctx.statement() != null) {
            for (Go_Parser.StatementContext stmtCtx : ctx.statement()) {
                blockNode.addChild(visit(stmtCtx));
            }
        }
        varTable.exitScope();
        return blockNode;
    }

    // --- DECLARAÇÕES ---

    @Override
    public AST visitVarSpecification(Go_Parser.VarSpecificationContext ctx) {
        AST varListNode = new AST(NodeKind.VAR_LIST_NODE, GoType.NO_TYPE);
        GoType type = visit(ctx.typeSpec()).type;

        String[] identifiers = ctx.identifierList().getText().split(",");
        int line = ctx.getStart().getLine();

        for (String varName : identifiers) {
            varName = varName.trim();
            if (!varTable.addVariable(varName, type, line)) {
                reportSemanticError("Variable '" + varName + "' already declared in this scope.");
            } else {
                VarEntry entry = varTable.lookup(varName);
                if (entry != null) allProcessedVariables.add(entry);
                varListNode.addChild(new AST(NodeKind.VAR_DECL_NODE, varName, type));
            }
        }
        return varListNode;
    }
    
    // --- ATRIBUIÇÕES ---

    @Override
    public AST visitSimpleAssignStatement(Go_Parser.SimpleAssignStatementContext ctx) {
        if (ctx.lvalue() == null || ctx.expr() == null) {
            return null;
        }

        AST lvalueNode = visit(ctx.lvalue());
        AST rvalueNode = visit(ctx.expr());

        if (lvalueNode == null || rvalueNode == null) return null;

        if (!rvalueNode.type.isCompatibleWith(lvalueNode.type)) {
            reportSemanticError("Type mismatch: cannot assign " + rvalueNode.type + " to " + lvalueNode.type);
        }

        if (lvalueNode.kind == NodeKind.VAR_USE_NODE) {
            VarEntry entry = varTable.lookup(lvalueNode.stringData);
            if (entry != null && entry.isConstant()) {
                reportSemanticError("Cannot assign to constant '" + lvalueNode.stringData + "'");
            }
        }

        return AST.newSubtree(NodeKind.ASSIGN_NODE, GoType.NO_TYPE, lvalueNode, rvalueNode);
    }

    // --- EXPRESSÕES ---

    @Override
    public AST visitAddSubExpr(Go_Parser.AddSubExprContext ctx) {
        AST left = visit(ctx.expr(0));
        AST right = visit(ctx.expr(1));

        String op = ctx.getChild(1).getText();
        NodeKind kind = op.equals("+") ? NodeKind.PLUS_NODE : NodeKind.MINUS_NODE;

        GoType resultType = typeTable.getBinaryOperationResultType(left.type, right.type, op);
        if (resultType == GoType.UNKNOWN) {
            reportSemanticError("Invalid operation: " + left.type + " " + op + " " + right.type);
        }
        
        return AST.newSubtree(kind, resultType, left, right);
    }

    @Override
    public AST visitMultiplyDivideModExpr(Go_Parser.MultiplyDivideModExprContext ctx) {
        AST left = visit(ctx.expr(0));
        AST right = visit(ctx.expr(1));

        String op = ctx.getChild(1).getText();
        NodeKind kind;
        if (op.equals("*")) {
            kind = NodeKind.TIMES_NODE;
        } else if (op.equals("/")) {
            kind = NodeKind.OVER_NODE;
        } else {
            kind = NodeKind.MOD_NODE;
        }

        GoType resultType = typeTable.getBinaryOperationResultType(left.type, right.type, op);
        if (resultType == GoType.UNKNOWN) {
            reportSemanticError("Invalid operation: " + left.type + " " + op + " " + right.type);
        }

        return AST.newSubtree(kind, resultType, left, right);
    }

    @Override
    public AST visitComparisonExpr(Go_Parser.ComparisonExprContext ctx) {
        AST left = visit(ctx.expr(0));
        AST right = visit(ctx.expr(1));

        String op = ctx.relation_op().getText();
        NodeKind kind;
        switch(op) {
            case "==": kind = NodeKind.EQ_NODE; break;
            case "!=": kind = NodeKind.NEQ_NODE; break;
            case "<":  kind = NodeKind.LT_NODE; break;
            case "<=": kind = NodeKind.LEQ_NODE; break;
            case ">":  kind = NodeKind.GT_NODE; break;
            case ">=": kind = NodeKind.GEQ_NODE; break;
            default: throw new IllegalStateException("Unknown comparison operator: " + op);
        }

        return AST.newSubtree(kind, GoType.BOOL, left, right);
    }

    @Override
    public AST visitLogicalANDExpr(Go_Parser.LogicalANDExprContext ctx) {
        AST left = visit(ctx.expr(0));
        AST right = visit(ctx.expr(1));

        if (left.type != GoType.BOOL || right.type != GoType.BOOL) {
            reportSemanticError("Logical AND (&&) operator requires boolean operands, but got " + left.type + " and " + right.type);
        }

        return AST.newSubtree(NodeKind.AND_NODE, GoType.BOOL, left, right);
    }
    
    @Override
    public AST visitLogicalORExpr(Go_Parser.LogicalORExprContext ctx) {
        AST left = visit(ctx.expr(0));
        AST right = visit(ctx.expr(1));

        if (left.type != GoType.BOOL || right.type != GoType.BOOL) {
            reportSemanticError("Logical OR (||) operator requires boolean operands, but got " + left.type + " and " + right.type);
        }

        return AST.newSubtree(NodeKind.OR_NODE, GoType.BOOL, left, right);
    }
    
    // NOVO MÉTODO IMPLEMENTADO
    @Override
    public AST visitUnaryPrefixExpr(Go_Parser.UnaryPrefixExprContext ctx) {
        AST operand = visit(ctx.expr());
        String op = ctx.getChild(0).getText();
        NodeKind kind;
        GoType resultType = operand.type;

        switch(op) {
            case "+":
                kind = NodeKind.UNARY_PLUS_NODE;
                if (!operand.type.isNumeric()) {
                    reportSemanticError("Unary plus operator (+) requires a numeric operand, but got " + operand.type);
                    resultType = GoType.UNKNOWN;
                }
                break;
            case "-":
                kind = NodeKind.UNARY_MINUS_NODE;
                if (!operand.type.isNumeric()) {
                    reportSemanticError("Unary minus operator (-) requires a numeric operand, but got " + operand.type);
                    resultType = GoType.UNKNOWN;
                }
                break;
            case "!":
                kind = NodeKind.NOT_NODE;
                if (operand.type != GoType.BOOL) {
                    reportSemanticError("Logical NOT operator (!) requires a boolean operand, but got " + operand.type);
                }
                resultType = GoType.BOOL; // O resultado de '!' é sempre booleano
                break;
            default:
                throw new IllegalStateException("Unknown unary operator: " + op);
        }
        return AST.newSubtree(kind, resultType, operand);
    }

    @Override
    public AST visitParenthesizedExpr(Go_Parser.ParenthesizedExprContext ctx) {
        return visit(ctx.expr());
    }

    // --- LITERAIS E IDENTIFICADORES ---

    @Override
    public AST visitIdExpr(Go_Parser.IdExprContext ctx) {
        String varName = ctx.ID().getText();
        VarEntry varEntry = varTable.lookup(varName);

        if (varEntry == null) {
            reportSemanticError("Undefined variable '" + varName + "'");
            return new AST(NodeKind.VAR_USE_NODE, varName, GoType.UNKNOWN);
        }

        return new AST(NodeKind.VAR_USE_NODE, varName, varEntry.getType());
    }

    @Override
    public AST visitIntLiteral(Go_Parser.IntLiteralContext ctx) {
        int value = Integer.parseInt(ctx.getText());
        return new AST(NodeKind.INT_VAL_NODE, value, GoType.INT);
    }

    @Override
    public AST visitFloatLiteral(Go_Parser.FloatLiteralContext ctx) {
        float value = Float.parseFloat(ctx.getText());
        return new AST(NodeKind.FLOAT_VAL_NODE, value, GoType.FLOAT64);
    }

    @Override
    public AST visitStringLiteral(Go_Parser.StringLiteralContext ctx) {
        String rawValue = ctx.getText();
        String actualValue = rawValue.substring(1, rawValue.length() - 1);
        
        StrEntry entry = stringTable.addString(actualValue);

        int strIndex = entry.getId(); 
        
        return new AST(NodeKind.STR_VAL_NODE, strIndex, GoType.STRING);
    }

    @Override
    public AST visitTrueLiteral(Go_Parser.TrueLiteralContext ctx) {
        return new AST(NodeKind.BOOL_VAL_NODE, 1, GoType.BOOL);
    }

    @Override
    public AST visitFalseLiteral(Go_Parser.FalseLiteralContext ctx) {
        return new AST(NodeKind.BOOL_VAL_NODE, 0, GoType.BOOL);
    }
    
    // --- TIPOS ---
    
    @Override public AST visitTypeInt(Go_Parser.TypeIntContext ctx) { return new AST(null, GoType.INT); }
    @Override public AST visitTypeInt8(Go_Parser.TypeInt8Context ctx) { return new AST(null, GoType.INT8); }
    @Override public AST visitTypeInt16(Go_Parser.TypeInt16Context ctx) { return new AST(null, GoType.INT16); }
    @Override public AST visitTypeInt32(Go_Parser.TypeInt32Context ctx) { return new AST(null, GoType.INT32); }
    @Override public AST visitTypeInt64(Go_Parser.TypeInt64Context ctx) { return new AST(null, GoType.INT64); }
    @Override public AST visitTypeFloat32(Go_Parser.TypeFloat32Context ctx) { return new AST(null, GoType.FLOAT32); }
    @Override public AST visitTypeFloat64(Go_Parser.TypeFloat64Context ctx) { return new AST(null, GoType.FLOAT64); }
    @Override public AST visitTypeBool(Go_Parser.TypeBoolContext ctx) { return new AST(null, GoType.BOOL); }
    @Override public AST visitTypeString(Go_Parser.TypeStringContext ctx) { return new AST(null, GoType.STRING); }

    // --- ESTRUTURAS DE CONTROLE ---

    @Override
    public AST visitIfElseStatement(Go_Parser.IfElseStatementContext ctx) {
        AST condition = visit(ctx.expr());

        if (condition.type != GoType.BOOL) {
            reportSemanticError("If condition must be a boolean expression, but got " + condition.type);
        }

        AST thenBlock = visit(ctx.block(0));
        AST elseBlock = null;
        if (ctx.block(1) != null) {
            elseBlock = visit(ctx.block(1));
        }

        return AST.newSubtree(NodeKind.IF_NODE, GoType.NO_TYPE, condition, thenBlock, elseBlock);
    }
    
    // --- MÉTODO DE RELATÓRIO ---
    public void printReport() {
        System.out.println("\n=== RELATÓRIO DA ANÁLISE SEMÂNTICA ===");

        if (foundSemanticErrors) {
            System.out.println("❌ Erros semânticos encontrados!");
        } else {
            System.out.println("✅ Nenhum erro semântico encontrado.");
        }

        System.out.println("\n=== VARIABLE TABLE ===");
        if (allProcessedVariables.isEmpty()) {
            System.out.println("No variables declared.");
        } else {
            for (VarEntry entry : allProcessedVariables) {
                System.out.println("  " + entry.toString());
            }
        }

        functionTable.printTable();
        arrayTable.printTable();

        System.out.println("\n=== STRING TABLE ===");
        stringTable.printTable();
    }
}
