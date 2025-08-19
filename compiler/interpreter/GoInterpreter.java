package compiler.interpreter;

import compiler.ast.AST;
import compiler.ast.NodeKind;
import compiler.typing.GoType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoInterpreter {

    private final Memory memory;
    private final OperandStack stack;
    private final Map<String, AST> functionDeclarations;

    public GoInterpreter() {
        this.memory = new Memory();
        this.stack = new OperandStack();
        this.functionDeclarations = new HashMap<>();
    }

    public void execute(AST root) {
        if (root == null) return;
        try {
            visit(root); // Fase 1: Registar todas as funções
            AST mainFuncNode = functionDeclarations.get("main");
            if (mainFuncNode != null) {
                // Fase 2: Executar a função 'main'
                AST mainCallNode = AST.call(AST.id("main", 0, 0), new ArrayList<>(), 0, 0);
                visitCallNode(mainCallNode);
            } else {
                System.err.println("Erro de execução: função 'main' não definida.");
            }
        } catch (ReturnException e) {
            System.err.println("Erro de execução: 'return' no escopo global.");
        } catch (Exception e) {
            System.err.println("❌ Erro durante a execução: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }

    private void visit(AST node) {
        if (node == null) return;

        switch (node.kind) {
            case PROGRAM_NODE:      visitProgramNode(node); break;
            case BLOCK_NODE:        visitBlockNode(node); break;
            case INT_VAL_NODE:      visitIntValNode(node); break;
            case REAL_VAL_NODE:     visitRealValNode(node); break;
            case BOOL_VAL_NODE:     visitBoolValNode(node); break;
            case STR_VAL_NODE:      visitStrValNode(node); break;
            case ID_NODE:           visitIdNode(node); break;
            case CONST_DECL_NODE:       visitConstDeclNode(node); break;
            case CONST_SPEC_NODE:       visitConstSpecNode(node); break;
            case VAR_DECL_NODE:         visitVarDeclNode(node); break;
            case VAR_SPEC_NODE:         visitVarSpecNode(node); break;
            case SHORT_VAR_DECL_NODE:   visitShortVarDeclNode(node); break;
            case ASSIGN_NODE:           visitAssignNode(node); break;
            case PLUS_NODE:         visitPlusNode(node); break;
            case MINUS_NODE:        visitMinusNode(node); break;
            case TIMES_NODE:        visitTimesNode(node); break;
            case OVER_NODE:         visitOverNode(node); break;
            case MOD_NODE:          visitModNode(node); break;
            case EQUAL_NODE:        visitEqualNode(node); break;
            case NOT_EQUAL_NODE:    visitNotEqualNode(node); break;
            case LESS_NODE:         visitLessNode(node); break;
            case GREATER_NODE:      visitGreaterNode(node); break;
            case LESS_EQ_NODE:      visitLessEqNode(node); break;
            case GREATER_EQ_NODE:   visitGreaterEqNode(node); break;
            case AND_NODE:          visitAndNode(node); break;
            case OR_NODE:           visitOrNode(node); break;
            case NOT_NODE:          visitNotNode(node); break;
            case UNARY_MINUS_NODE:  visitUnaryMinusNode(node); break;
            case IF_NODE:           visitIfNode(node); break;
            case FOR_CLAUSE_NODE:   visitForClauseNode(node); break;
            case FUNC_DECL_NODE:    visitFuncDeclNode(node); break;
            case CALL_NODE:         visitCallNode(node); break;
            case RETURN_NODE:       visitReturnNode(node); break;
            case BREAK_NODE:        throw new BreakException();
            case CONTINUE_NODE:     throw new ContinueException();
            default:
                for (AST child : node.getChildren()) {
                    visit(child);
                }
                break;
        }
    }

    private void visitProgramNode(AST node) { for (AST child : node.getChildren()) visit(child); }
    private void visitBlockNode(AST node) { for (AST child : node.getChildren()) visit(child); }
    private void visitIntValNode(AST node) { stack.pushInt(node.intData); }
    private void visitRealValNode(AST node) { stack.pushFloat(node.floatData); }
    private void visitBoolValNode(AST node) { stack.pushBool(node.boolData); }
    private void visitStrValNode(AST node) { stack.pushString(node.text); }
    private void visitIdNode(AST node) {
        Object value = memory.fetch(node.text);
        GoType type = node.getAnnotatedType();
        if (type == GoType.INT) stack.pushInt((Integer) value);
        else if (type == GoType.FLOAT64) stack.pushFloat((Float) value);
        else if (type == GoType.BOOL) stack.pushBool((Boolean) value);
        else if (type == GoType.STRING) stack.pushString((String) value);
    }
    private void visitConstDeclNode(AST node) { for (AST child : node.getChildren()) visit(child); }
    private void visitConstSpecNode(AST node) {
        List<AST> idNodes = new ArrayList<>();
        AST exprListNode = null;
        for (AST child : node.getChildren()) {
            if (child.kind == NodeKind.ID_NODE) idNodes.add(child);
            else if (child.kind == NodeKind.EXPR_LIST_NODE) exprListNode = child;
        }
        if (exprListNode != null) {
            for (int i = 0; i < idNodes.size(); i++) {
                visit(exprListNode.getChild(i));
                popAndDeclare(idNodes.get(i).text, idNodes.get(i).getAnnotatedType());
            }
        }
    }
    private void visitVarDeclNode(AST node) { for (AST child : node.getChildren()) visit(child); }
    private void visitVarSpecNode(AST node) {
        List<AST> idNodes = new ArrayList<>();
        AST exprListNode = null;
        for (AST child : node.getChildren()) {
            if (child.kind == NodeKind.ID_NODE) idNodes.add(child);
            else if (child.kind == NodeKind.EXPR_LIST_NODE) exprListNode = child;
        }
        if (exprListNode != null) {
            for (int i = 0; i < idNodes.size(); i++) {
                visit(exprListNode.getChild(i));
                popAndDeclare(idNodes.get(i).text, idNodes.get(i).getAnnotatedType());
            }
        } else {
            for (AST idNode : idNodes) {
                memory.declare(idNode.text, getZeroValue(idNode.getAnnotatedType()));
            }
        }
    }
    private void visitShortVarDeclNode(AST node) {
        AST idListNode = node.getChild(0);
        AST exprListNode = node.getChild(1);
        for (int i = 0; i < idListNode.getChildCount(); i++) {
            visit(exprListNode.getChild(i));
            popAndDeclare(idListNode.getChild(i).text, idListNode.getChild(i).getAnnotatedType());
        }
    }
    private void visitAssignNode(AST node) {
        visit(node.getChild(1));
        popAndUpdate(node.getChild(0).text, node.getChild(0).getAnnotatedType());
    }

    private void visitPlusNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getAnnotatedType();
        if (type == GoType.STRING) {
            String right = stack.popString(); String left = stack.popString(); stack.pushString(left + right);
        } else if (type == GoType.FLOAT64) {
            float right = stack.popFloat(); float left = stack.popFloat(); stack.pushFloat(left + right);
        } else {
            int right = stack.popInt(); int left = stack.popInt(); stack.pushInt(left + right);
        }
    }
    private void visitMinusNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getAnnotatedType();
        if (type == GoType.FLOAT64) {
            float right = stack.popFloat(); float left = stack.popFloat(); stack.pushFloat(left - right);
        } else {
            int right = stack.popInt(); int left = stack.popInt(); stack.pushInt(left - right);
        }
    }
    private void visitTimesNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getAnnotatedType();
        if (type == GoType.FLOAT64) {
            float right = stack.popFloat(); float left = stack.popFloat(); stack.pushFloat(left * right);
        } else {
            int right = stack.popInt(); int left = stack.popInt(); stack.pushInt(left * right);
        }
    }
    private void visitOverNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getAnnotatedType();
        if (type == GoType.FLOAT64) {
            float right = stack.popFloat(); float left = stack.popFloat(); stack.pushFloat(right == 0.0f ? 0.0f : left / right);
        } else {
            int right = stack.popInt(); int left = stack.popInt(); stack.pushInt(right == 0 ? 0 : left / right);
        }
    }
    private void visitModNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        int right = stack.popInt(); int left = stack.popInt(); stack.pushInt(right == 0 ? 0 : left % right);
    }
    private void visitEqualNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getChild(0).getAnnotatedType();
        if (type == GoType.STRING) { String r = stack.popString(); String l = stack.popString(); stack.pushBool(l.equals(r)); }
        else if (type == GoType.FLOAT64) { float r = stack.popFloat(); float l = stack.popFloat(); stack.pushBool(l == r); }
        else if (type == GoType.BOOL) { boolean r = stack.popBool(); boolean l = stack.popBool(); stack.pushBool(l == r); }
        else { int r = stack.popInt(); int l = stack.popInt(); stack.pushBool(l == r); }
    }
    private void visitNotEqualNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getChild(0).getAnnotatedType();
        if (type == GoType.STRING) { String r = stack.popString(); String l = stack.popString(); stack.pushBool(!l.equals(r)); }
        else if (type == GoType.FLOAT64) { float r = stack.popFloat(); float l = stack.popFloat(); stack.pushBool(l != r); }
        else if (type == GoType.BOOL) { boolean r = stack.popBool(); boolean l = stack.popBool(); stack.pushBool(l != r); }
        else { int r = stack.popInt(); int l = stack.popInt(); stack.pushBool(l != r); }
    }
    private void visitLessNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getChild(0).getAnnotatedType();
        if (type == GoType.FLOAT64) { float r = stack.popFloat(); float l = stack.popFloat(); stack.pushBool(l < r); }
        else { int r = stack.popInt(); int l = stack.popInt(); stack.pushBool(l < r); }
    }
    private void visitGreaterNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getChild(0).getAnnotatedType();
        if (type == GoType.FLOAT64) { float r = stack.popFloat(); float l = stack.popFloat(); stack.pushBool(l > r); }
        else { int r = stack.popInt(); int l = stack.popInt(); stack.pushBool(l > r); }
    }
    private void visitLessEqNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getChild(0).getAnnotatedType();
        if (type == GoType.FLOAT64) { float r = stack.popFloat(); float l = stack.popFloat(); stack.pushBool(l <= r); }
        else { int r = stack.popInt(); int l = stack.popInt(); stack.pushBool(l <= r); }
    }
    private void visitGreaterEqNode(AST node) {
        visit(node.getChild(0));
        visit(node.getChild(1));
        GoType type = node.getChild(0).getAnnotatedType();
        if (type == GoType.FLOAT64) { float r = stack.popFloat(); float l = stack.popFloat(); stack.pushBool(l >= r); }
        else { int r = stack.popInt(); int l = stack.popInt(); stack.pushBool(l >= r); }
    }
    private void visitAndNode(AST node) {
        visit(node.getChild(0));
        if (!stack.popBool()) {
            stack.pushBool(false);
            return;
        }
        visit(node.getChild(1));
    }
    private void visitOrNode(AST node) {
        visit(node.getChild(0));
        if (stack.popBool()) {
            stack.pushBool(true);
            return;
        }
        visit(node.getChild(1));
    }
    private void visitNotNode(AST node) {
        visit(node.getChild(0));
        stack.pushBool(!stack.popBool());
    }
    private void visitUnaryMinusNode(AST node) {
        visit(node.getChild(0));
        GoType type = node.getAnnotatedType();
        if (type == GoType.INT) stack.pushInt(-stack.popInt());
        else if (type == GoType.FLOAT64) stack.pushFloat(-stack.popFloat());
    }
    private void visitIfNode(AST node) {
        visit(node.getChild(0));
        boolean condition = stack.popBool();
        if (condition) {
            visit(node.getChild(1));
        } else if (node.getChildCount() > 2) {
            visit(node.getChild(2));
        }
    }
    private void visitForClauseNode(AST node) {
        memory.enterScope();
        AST init = node.getChild(0);
        AST cond = node.getChild(1);
        AST post = node.getChild(2);
        AST body = node.getChild(3);
        visit(init);
        while (true) {
            try {
                if (cond != null) {
                    visit(cond);
                    if (!stack.popBool()) break;
                }
                visit(body);
                visit(post);
            } catch (BreakException e) {
                break;
            } catch (ContinueException e) {
                visit(post);
            }
        }
        memory.exitScope();
    }
    private void visitFuncDeclNode(AST node) {
        String funcName = node.getChild(0).text;
        functionDeclarations.put(funcName, node);
    }
    private void visitCallNode(AST node) {
        String funcName = node.getChild(0).text;
        if (isBuiltIn(funcName)) {
            handleBuiltIn(funcName, node);
            return;
        }
        AST funcDeclNode = functionDeclarations.get(funcName);
        if (funcDeclNode == null) {
            System.err.println("Erro de execução: chamada a função não definida '" + funcName + "'");
            return;
        }
        for (int i = 1; i < node.getChildCount(); i++) {
            visit(node.getChild(i));
        }
        List<Object> argValues = new ArrayList<>();
        for (int i = node.getChildCount() - 1; i >= 1; i--) {
            GoType argType = node.getChild(i).getAnnotatedType();
            if (argType == GoType.INT) argValues.add(0, stack.popInt());
            else if (argType == GoType.FLOAT64) argValues.add(0, stack.popFloat());
            else if (argType == GoType.BOOL) argValues.add(0, stack.popBool());
            else if (argType == GoType.STRING) argValues.add(0, stack.popString());
        }
        memory.enterScope();
        AST paramListNode = funcDeclNode.getChild(1);
        for (int i = 0; i < paramListNode.getChildCount(); i++) {
            String paramName = paramListNode.getChild(i).getChild(0).text;
            memory.declare(paramName, argValues.get(i));
        }
        AST bodyNode = funcDeclNode.getChild(3);
        try {
            visit(bodyNode);
        } catch (ReturnException e) {
            if (e.returnValue != null) {
                GoType returnType = funcDeclNode.getChild(2).getAnnotatedType();
                if (returnType == GoType.INT) stack.pushInt((Integer) e.returnValue);
                else if (returnType == GoType.FLOAT64) stack.pushFloat((Float) e.returnValue);
                else if (returnType == GoType.BOOL) stack.pushBool((Boolean) e.returnValue);
                else if (returnType == GoType.STRING) stack.pushString((String) e.returnValue);
            }
        } finally {
            memory.exitScope();
        }
    }
    private void visitReturnNode(AST node) {
        Object returnValue = null;
        if (node.hasChildren()) {
            visit(node.getChild(0));
            GoType returnType = node.getChild(0).getAnnotatedType();
            if (returnType == GoType.INT) returnValue = stack.popInt();
            else if (returnType == GoType.FLOAT64) returnValue = stack.popFloat();
            else if (returnType == GoType.BOOL) returnValue = stack.popBool();
            else if (returnType == GoType.STRING) returnValue = stack.popString();
        }
        throw new ReturnException(returnValue);
    }
    private boolean isBuiltIn(String funcName) {
        return funcName.equals("println");
    }
    private void handleBuiltIn(String funcName, AST callNode) {
        if ("println".equals(funcName)) {
            List<Object> valuesToPrint = new ArrayList<>();
            for (int i = 1; i < callNode.getChildCount(); i++) {
                visit(callNode.getChild(i));
            }
            for (int i = callNode.getChildCount() - 1; i >= 1; i--) {
                GoType argType = callNode.getChild(i).getAnnotatedType();
                if (argType == GoType.INT) valuesToPrint.add(0, stack.popInt());
                else if (argType == GoType.FLOAT64) valuesToPrint.add(0, stack.popFloat());
                else if (argType == GoType.BOOL) valuesToPrint.add(0, stack.popBool());
                else if (argType == GoType.STRING) valuesToPrint.add(0, stack.popString());
                else valuesToPrint.add(0, "valor desconhecido");
            }
            for (int i = 0; i < valuesToPrint.size(); i++) {
                System.out.print(valuesToPrint.get(i));
                if (i < valuesToPrint.size() - 1) {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }
    private void popAndDeclare(String varName, GoType type) {
        if (type == GoType.INT) memory.declare(varName, stack.popInt());
        else if (type == GoType.FLOAT64) memory.declare(varName, stack.popFloat());
        else if (type == GoType.BOOL) memory.declare(varName, stack.popBool());
        else if (type == GoType.STRING) memory.declare(varName, stack.popString());
    }
    private void popAndUpdate(String varName, GoType type) {
        if (type == GoType.INT) memory.update(varName, stack.popInt());
        else if (type == GoType.FLOAT64) memory.update(varName, stack.popFloat());
        else if (type == GoType.BOOL) memory.update(varName, stack.popBool());
        else if (type == GoType.STRING) memory.update(varName, stack.popString());
    }
    private Object getZeroValue(GoType type) {
        if (type == GoType.INT) return 0;
        if (type == GoType.FLOAT64) return 0.0f;
        if (type == GoType.BOOL) return false;
        if (type == GoType.STRING) return "";
        return null;
    }
}
