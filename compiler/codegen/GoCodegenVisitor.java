package compiler.codegen;

import compiler.ast.AST;
import compiler.ast.NodeKind;
import compiler.typing.GoType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * GoCodegenVisitor gera código LLVM IR manualmente percorrendo a AST.
 * A saída é uma string de texto que pode ser salva em um ficheiro .ll.
 */
public class GoCodegenVisitor {

    // StringBuilder para construir o código LLVM IR.
    private StringBuilder irBuilder;
    private StringBuilder headerBuilder; // Para declarações globais e de funções

    // Contadores para gerar nomes únicos.
    private int regCounter; // Para registadores virtuais (%0, %1, ...)
    private int labelCounter; // Para labels (entry, if.then, loop, ...)
    private int strCounter; // Para strings globais (@.str.0, @.str.1, ...)

    // Tabela de símbolos para mapear variáveis do código fonte para
    // seus ponteiros na pilha LLVM. Ex: "x" -> "%x.addr"
    private Stack<Map<String, String>> symbolTable;
    
    // Mapa para informações de funções
    private Map<String, AST> functionDeclarations;


    public GoCodegenVisitor() {
        this.irBuilder = new StringBuilder();
        this.headerBuilder = new StringBuilder();
        this.regCounter = 0;
        this.labelCounter = 0;
        this.strCounter = 0;
        this.symbolTable = new Stack<>();
        this.functionDeclarations = new HashMap<>();
    }

    public String run(AST root) {
        // Declaração de funções built-in como 'printf' do C
        headerBuilder.append("declare i32 @printf(i8*, ...)\n\n");

        for (AST child : root.getChildren()) {
            if (child.kind == NodeKind.FUNC_DECL_NODE) {
                String funcName = child.getChild(0).text;
                functionDeclarations.put(funcName, child);
            }
        }

        visit(root);

        return headerBuilder.toString() + irBuilder.toString();
    }

    private String visit(AST node) {
        if (node == null) return "";

        switch (node.kind) {
            case PROGRAM_NODE:      return visitProgramNode(node);
            case FUNC_DECL_NODE:    return visitFuncDeclNode(node);
            case BLOCK_NODE:        return visitBlockNode(node);
            
            // Statements
            case IF_NODE:           return visitIfNode(node);
            case FOR_CLAUSE_NODE:   return visitForClauseNode(node);
            case INC_DEC_STMT_NODE: return visitIncDecStmtNode(node);
            case VAR_DECL_NODE:     return visitVarDeclNode(node);
            case VAR_SPEC_NODE:     return visitVarSpecNode(node);
            case SHORT_VAR_DECL_NODE: return visitShortVarDeclNode(node);
            case ASSIGN_NODE:       return visitAssignNode(node);
            case RETURN_NODE:       return visitReturnNode(node);
            
            // Expressões
            case CALL_NODE:         return visitCallNode(node);
            case PLUS_NODE:         return visitBinaryOpNode(node, "add", null);
            case MINUS_NODE:        return visitBinaryOpNode(node, "sub", null);
            case TIMES_NODE:        return visitBinaryOpNode(node, "mul", null);
            case OVER_NODE:         return visitBinaryOpNode(node, "sdiv", null);
            
            case EQUAL_NODE:        return visitBinaryOpNode(node, "icmp", "eq");
            case NOT_EQUAL_NODE:    return visitBinaryOpNode(node, "icmp", "ne");
            case LESS_NODE:         return visitBinaryOpNode(node, "icmp", "slt");
            case LESS_EQ_NODE:      return visitBinaryOpNode(node, "icmp", "sle");
            case GREATER_NODE:      return visitBinaryOpNode(node, "icmp", "sgt");
            case GREATER_EQ_NODE:   return visitBinaryOpNode(node, "icmp", "sge");
            
            // Literais e Identificadores
            case INT_VAL_NODE:      return visitIntValNode(node);
            case BOOL_VAL_NODE:     return visitBoolValNode(node);
            case ID_NODE:           return visitIdNode(node);

            default:
                for (AST child : node.getChildren()) {
                    visit(child);
                }
                break;
        }
        return "";
    }

    // --- Métodos Auxiliares ---

    private String newReg() { return "%" + regCounter++; }
    private String newLabel(String prefix) { return prefix + "." + labelCounter++; }
    private String newStrConstant(String str) {
        String strName = "@.str." + strCounter++;
        int len = str.length() + 1;
        headerBuilder.append(strName).append(" = private unnamed_addr constant [")
                     .append(len).append(" x i8] c\"").append(str).append("\\00\"\n");
        return strName;
    }

    private void emit(String instruction) { irBuilder.append("\t").append(instruction).append("\n"); }
    private void emitLabel(String label) { irBuilder.append(label).append(":\n"); }

    private String getLLVMType(GoType goType) {
        switch (goType) {
            case INT: case INT32: return "i32";
            case BOOL: return "i1";
            case FLOAT64: return "double";
            case STRING: return "i8*";
            case VOID: return "void";
            default: return "void";
        }
    }

    // --- Visitantes da AST ---

    private String visitProgramNode(AST node) {
        for (AST child : node.getChildren()) {
            visit(child);
        }
        return "";
    }

    private String visitFuncDeclNode(AST node) {
        String funcName = node.getChild(0).text;
        AST paramListNode = node.getChild(1);
        AST resultNode = node.getChild(2);
        AST bodyNode = node.getChild(3);
        String returnType = getLLVMType(resultNode.type);
        
        // CORREÇÃO 1: Inicializa o contador de registos com o número de parâmetros.
        regCounter = paramListNode.getChildCount();

        List<String> paramDefs = new ArrayList<>();
        for (AST paramNode : paramListNode.getChildren()) {
            AST paramIdNode = paramNode.getChild(0);
            String paramType = getLLVMType(paramIdNode.getAnnotatedType());
            paramDefs.add(paramType);
        }
        String paramsString = String.join(", ", paramDefs);

        irBuilder.append("\ndefine ").append(returnType).append(" @").append(funcName)
                 .append("(").append(paramsString).append(") {\n");
        
        symbolTable.push(new HashMap<>());
        emitLabel("entry");

        for (int i = 0; i < paramListNode.getChildCount(); i++) {
            AST paramNode = paramListNode.getChild(i);
            AST paramIdNode = paramNode.getChild(0);
            String paramName = paramIdNode.text;
            String paramType = getLLVMType(paramIdNode.getAnnotatedType());
            String pointerName = "%" + paramName + ".addr";

            emit(pointerName + " = alloca " + paramType);
            emit("store " + paramType + " %" + i + ", " + paramType + "* " + pointerName);
            symbolTable.peek().put(paramName, pointerName);
        }

        visit(bodyNode);

        // CORREÇÃO 2: Removemos o 'ret' de fallback.
        // O 'visitReturnNode' é agora o único responsável por emitir 'ret'.
        // Se uma função não-void não tiver 'return', é um erro semântico que
        // o checker deve apanhar. Se for void, o LLVM permite que termine sem 'ret'.
        
        irBuilder.append("}\n");
        symbolTable.pop();
        return "";
    }

    private String visitBlockNode(AST node) {
        symbolTable.push(new HashMap<>(symbolTable.peek()));
        for (AST child : node.getChildren()) {
            visit(child);
        }
        symbolTable.pop();
        return "";
    }
    
    // --- Statements ---

    private String visitIfNode(AST node) {
        String condValue = visit(node.getChild(0));
        String thenLabel = newLabel("if.then");
        String elseLabel = newLabel("if.else");
        String endLabel = newLabel("if.end");
        boolean hasElse = node.getChildCount() > 2;

        if (hasElse) {
            emit("br i1 " + condValue + ", label %" + thenLabel + ", label %" + elseLabel);
        } else {
            emit("br i1 " + condValue + ", label %" + thenLabel + ", label %" + endLabel);
        }

        emitLabel(thenLabel);
        visit(node.getChild(1));
        emit("br label %" + endLabel);

        if (hasElse) {
            emitLabel(elseLabel);
            visit(node.getChild(2));
            emit("br label %" + endLabel);
        }

        emitLabel(endLabel);
        return "";
    }

    private String visitForClauseNode(AST node) {
        AST initNode = node.getChild(0);
        AST condNode = node.getChild(1);
        AST postNode = node.getChild(2);
        AST bodyNode = node.getChild(3);

        String condLabel = newLabel("loop.cond");
        String bodyLabel = newLabel("loop.body");
        String postLabel = newLabel("loop.post");
        String endLabel = newLabel("loop.end");

        if (initNode != null) visit(initNode);
        emit("br label %" + condLabel);

        emitLabel(condLabel);
        String condValue = visit(condNode);
        emit("br i1 " + condValue + ", label %" + bodyLabel + ", label %" + endLabel);

        emitLabel(bodyLabel);
        visit(bodyNode);
        emit("br label %" + postLabel);

        emitLabel(postLabel);
        if (postNode != null) visit(postNode);
        emit("br label %" + condLabel);

        emitLabel(endLabel);
        return "";
    }

    private String visitIncDecStmtNode(AST node) {
        AST lvalueNode = node.getChild(0);
        AST opNode = node.getChild(1);
        String varName = lvalueNode.text;
        String pointerName = symbolTable.peek().get(varName);
        String llvmType = getLLVMType(lvalueNode.getAnnotatedType());
        
        String currentValue = newReg();
        emit(currentValue + " = load " + llvmType + ", " + llvmType + "* " + pointerName);

        String newValue = newReg();
        String op = opNode.text.equals("++") ? "add" : "sub";
        emit(newValue + " = " + op + " nsw " + llvmType + " " + currentValue + ", 1");

        emit("store " + llvmType + " " + newValue + ", " + llvmType + "* " + pointerName);
        return "";
    }

    private String visitVarDeclNode(AST node) {
        for (AST spec : node.getChildren()) visit(spec);
        return "";
    }
    
    private String visitVarSpecNode(AST node) {
        AST exprListNode = null;
        if (node.getChild(node.getChildCount() - 1).kind == NodeKind.EXPR_LIST_NODE) {
            exprListNode = node.getChild(node.getChildCount() - 1);
        }
        int exprIndex = 0;
        for (AST idNode : node.getChildren()) {
            if (idNode.kind != NodeKind.ID_NODE) continue;
            
            String varName = idNode.text;
            String llvmType = getLLVMType(idNode.getAnnotatedType());
            String pointerName = "%" + varName + ".addr";

            emit(pointerName + " = alloca " + llvmType);
            symbolTable.peek().put(varName, pointerName);

            if (exprListNode != null && exprIndex < exprListNode.getChildCount()) {
                String value = visit(exprListNode.getChild(exprIndex));
                emit("store " + llvmType + " " + value + ", " + llvmType + "* " + pointerName);
                exprIndex++;
            }
        }
        return "";
    }
    
    private String visitShortVarDeclNode(AST node) {
        AST idListNode = node.getChild(0);
        AST exprListNode = node.getChild(1);

        for (int i = 0; i < idListNode.getChildCount(); i++) {
            AST idNode = idListNode.getChild(i);
            AST exprNode = exprListNode.getChild(i);
            String varName = idNode.text;
            String llvmType = getLLVMType(idNode.getAnnotatedType());
            String pointerName = "%" + varName + ".addr";

            emit(pointerName + " = alloca " + llvmType);
            symbolTable.peek().put(varName, pointerName);

            String value = visit(exprNode);
            emit("store " + llvmType + " " + value + ", " + llvmType + "* " + pointerName);
        }
        return "";
    }

    private String visitAssignNode(AST node) {
        String varName = node.getChild(0).text;
        String pointerName = symbolTable.peek().get(varName);
        String llvmType = getLLVMType(node.getChild(0).getAnnotatedType());

        String value = visit(node.getChild(1));
        emit("store " + llvmType + " " + value + ", " + llvmType + "* " + pointerName);
        return "";
    }

    private String visitReturnNode(AST node) {
        if (node.hasChildren()) {
            String returnType = getLLVMType(node.getChild(0).getAnnotatedType());
            String exprResult = visit(node.getChild(0));
            emit("ret " + returnType + " " + exprResult);
        } else {
            emit("ret void");
        }
        return "";
    }

    // --- Expressões ---

    private String visitCallNode(AST node) {
        String funcName = node.getChild(0).text;
        
        List<String> argValues = new ArrayList<>();
        for (int i = 1; i < node.getChildCount(); i++) {
            argValues.add(visit(node.getChild(i)));
        }

        AST funcDecl = functionDeclarations.get(funcName);
        AST paramListNode = funcDecl.getChild(1);
        List<String> typedArgs = new ArrayList<>();
        for (int i = 0; i < argValues.size(); i++) {
            AST paramNode = paramListNode.getChild(i).getChild(0);
            String paramType = getLLVMType(paramNode.getAnnotatedType());
            typedArgs.add(paramType + " " + argValues.get(i));
        }
        String argsString = String.join(", ", typedArgs);

        String returnType = getLLVMType(node.getAnnotatedType());
        
        if (returnType.equals("void")) {
            emit("call void @" + funcName + "(" + argsString + ")");
            return "";
        } else {
            String destReg = newReg();
            emit(destReg + " = call " + returnType + " @" + funcName + "(" + argsString + ")");
            return destReg;
        }
    }

    private String visitBinaryOpNode(AST node, String op, String condition) {
        String operandType = getLLVMType(node.getChild(0).getAnnotatedType());
        String left = visit(node.getChild(0));
        String right = visit(node.getChild(1));
        String destReg = newReg();
        
        String instruction;
        if (op.equals("icmp")) {
            instruction = destReg + " = " + op + " " + condition + " " + operandType + " " + left + ", " + right;
        } else {
            instruction = destReg + " = " + op + " nsw " + operandType + " " + left + ", " + right;
        }
        emit(instruction);
        return destReg;
    }

    private String visitIdNode(AST node) {
        String varName = node.text;
        String pointerName = symbolTable.peek().get(varName);
        String llvmType = getLLVMType(node.getAnnotatedType());
        String destReg = newReg();

        emit(destReg + " = load " + llvmType + ", " + llvmType + "* " + pointerName);
        return destReg;
    }

    private String visitIntValNode(AST node) {
        return String.valueOf(node.intData);
    }
    
    private String visitBoolValNode(AST node) {
        return node.boolData ? "1" : "0";
    }
}
