package compiler.codegen;

import compiler.ast.AST;
import compiler.ast.NodeKind;
import compiler.typing.GoType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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

    // Cache para strings de formato do printf, para não as duplicar.
    private Map<GoType, String> printfFormatStrings;
    
    // Pilhas para gerir labels de break/continue em ciclos aninhados
    private Stack<String> loopPostLabels;
    private Stack<String> loopEndLabels;


    public GoCodegenVisitor() {
        this.irBuilder = new StringBuilder();
        this.headerBuilder = new StringBuilder();
        this.regCounter = 0;
        this.labelCounter = 0;
        this.strCounter = 0;
        this.symbolTable = new Stack<>();
        this.functionDeclarations = new HashMap<>();
        this.printfFormatStrings = new HashMap<>();
        this.loopPostLabels = new Stack<>();
        this.loopEndLabels = new Stack<>();
    }

    public String run(AST root) {
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
            case BREAK_NODE:        return visitBreakNode(node);
            case CONTINUE_NODE:     return visitContinueNode(node);
            case INC_DEC_STMT_NODE: return visitIncDecStmtNode(node);
            case VAR_DECL_NODE:     return visitVarDeclNode(node);
            case VAR_SPEC_NODE:     return visitVarSpecNode(node);
            case SHORT_VAR_DECL_NODE: return visitShortVarDeclNode(node);
            case ASSIGN_NODE:       return visitAssignNode(node);
            case RETURN_NODE:       return visitReturnNode(node);
            
            // Expressões
            case CALL_NODE:         return visitCallNode(node);
            case TYPE_CONV_NODE:    return visitTypeConvNode(node);
            case PLUS_NODE:         return visitBinaryOpNode(node, "add", null);
            case MINUS_NODE:        return visitBinaryOpNode(node, "sub", null);
            case TIMES_NODE:        return visitBinaryOpNode(node, "mul", null);
            case OVER_NODE:         return visitBinaryOpNode(node, "div", null);
            case MOD_NODE:          return visitBinaryOpNode(node, "rem", null);
            
            case EQUAL_NODE:        return visitBinaryOpNode(node, "cmp", "eq");
            case NOT_EQUAL_NODE:    return visitBinaryOpNode(node, "cmp", "ne");
            case LESS_NODE:         return visitBinaryOpNode(node, "cmp", "lt");
            case LESS_EQ_NODE:      return visitBinaryOpNode(node, "cmp", "le");
            case GREATER_NODE:      return visitBinaryOpNode(node, "cmp", "gt");
            case GREATER_EQ_NODE:   return visitBinaryOpNode(node, "cmp", "ge");
            
            // Literais e Identificadores
            case INT_VAL_NODE:      return visitIntValNode(node);
            case REAL_VAL_NODE:     return visitRealValNode(node);
            case BOOL_VAL_NODE:     return visitBoolValNode(node);
            case STR_VAL_NODE:      return visitStrValNode(node);
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
    
    private String createGlobalString(String value, String namePrefix) {
        String strName = "@" + namePrefix + "." + strCounter++;
        int len = value.length() + 1;
        headerBuilder.append(strName).append(" = private unnamed_addr constant [")
                     .append(len).append(" x i8] c\"").append(value).append("\\00\"\n");
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

    private String getOrCreateFormatString(GoType type) {
        if (printfFormatStrings.containsKey(type)) {
            return printfFormatStrings.get(type);
        }
        String formatSpecifier;
        switch (type) {
            case INT: case INT32: case BOOL: formatSpecifier = "%d\\0A"; break;
            case FLOAT64: formatSpecifier = "%f\\0A"; break;
            case STRING: formatSpecifier = "%s\\0A"; break;
            default: formatSpecifier = "Unsupported type\\0A";
        }
        String formatStrName = createGlobalString(formatSpecifier, ".fmt");
        printfFormatStrings.put(type, formatStrName);
        return formatStrName;
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
        
        if (returnType.equals("void")) {
             if (irBuilder.length() > 0 && !irBuilder.toString().trim().endsWith("ret") && !irBuilder.toString().trim().endsWith("br")) {
                 emit("ret void");
             }
        }
        
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
        if (irBuilder.length() > 0 && !irBuilder.toString().trim().endsWith("ret") && !irBuilder.toString().trim().endsWith("br")) {
            emit("br label %" + endLabel);
        }

        if (hasElse) {
            emitLabel(elseLabel);
            visit(node.getChild(2));
            if (irBuilder.length() > 0 && !irBuilder.toString().trim().endsWith("ret") && !irBuilder.toString().trim().endsWith("br")) {
                emit("br label %" + endLabel);
            }
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

        loopPostLabels.push(postLabel);
        loopEndLabels.push(endLabel);

        if (initNode != null) visit(initNode);
        emit("br label %" + condLabel);

        emitLabel(condLabel);
        String condValue = visit(condNode);
        emit("br i1 " + condValue + ", label %" + bodyLabel + ", label %" + endLabel);

        emitLabel(bodyLabel);
        visit(bodyNode);
        if (irBuilder.length() > 0 && !irBuilder.toString().trim().endsWith("ret") && !irBuilder.toString().trim().endsWith("br")) {
            emit("br label %" + postLabel);
        }

        emitLabel(postLabel);
        if (postNode != null) visit(postNode);
        emit("br label %" + condLabel);

        emitLabel(endLabel);

        loopPostLabels.pop();
        loopEndLabels.pop();
        return "";
    }

    private String visitBreakNode(AST node) {
        if (!loopEndLabels.isEmpty()) {
            emit("br label %" + loopEndLabels.peek());
        }
        return "";
    }

    private String visitContinueNode(AST node) {
        if (!loopPostLabels.isEmpty()) {
            emit("br label %" + loopPostLabels.peek());
        }
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
        String one = llvmType.equals("double") ? "1.0" : "1";
        
        String prefix = llvmType.equals("double") ? "f" : "";
        emit(newValue + " = " + prefix + op + " " + llvmType + " " + currentValue + ", " + one);

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

        if (funcName.equals("println")) {
            for (int i = 1; i < node.getChildCount(); i++) {
                AST argNode = node.getChild(i);
                GoType argType = argNode.getAnnotatedType();
                String argValue = visit(argNode);

                String formatStrName = getOrCreateFormatString(argType);
                int formatStrLen = 4;

                String formatStrPtr = newReg();
                emit(formatStrPtr + " = getelementptr inbounds [" + formatStrLen + " x i8], [" + formatStrLen + " x i8]* " + formatStrName + ", i64 0, i64 0");
                
                emit("call i32 (i8*, ...) @printf(i8* " + formatStrPtr + ", " + getLLVMType(argType) + " " + argValue + ")");
            }
            return "";
        }

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

    private String visitTypeConvNode(AST node) {
        GoType targetType = node.getAnnotatedType();
        AST exprNode = node.getChild(0);
        GoType sourceType = exprNode.getAnnotatedType();
        
        String sourceValue = visit(exprNode);
        String destReg = newReg();
        
        if (sourceType.isInteger() && targetType.isFloat()) {
            emit(destReg + " = sitofp " + getLLVMType(sourceType) + " " + sourceValue + " to " + getLLVMType(targetType));
        } else if (sourceType.isFloat() && targetType.isInteger()) {
            emit(destReg + " = fptosi " + getLLVMType(sourceType) + " " + sourceValue + " to " + getLLVMType(targetType));
        } else {
            return sourceValue;
        }
        
        return destReg;
    }

    private String visitBinaryOpNode(AST node, String op, String condition) {
        GoType operandGoType = node.getChild(0).getAnnotatedType();
        String operandType = getLLVMType(operandGoType);
        String left = visit(node.getChild(0));
        String right = visit(node.getChild(1));
        String destReg = newReg();
        
        String instruction;
        if (operandGoType.isFloat()) {
            String prefix = "f";
            String cmpPrefix = "fcmp";
            Map<String, String> floatConditions = Map.of("eq", "oeq", "ne", "one", "lt", "olt", "le", "ole", "gt", "ogt", "ge", "oge");
            
            if (op.equals("cmp")) {
                instruction = destReg + " = " + cmpPrefix + " " + floatConditions.get(condition) + " " + operandType + " " + left + ", " + right;
            } else {
                instruction = destReg + " = " + prefix + op + " " + operandType + " " + left + ", " + right;
            }
        } else {
            String cmpPrefix = "icmp";
            String arithOp = op;
            if (op.equals("rem")) arithOp = "srem";
            if (op.equals("div")) arithOp = "sdiv";
            
            if (op.equals("cmp")) {
                instruction = destReg + " = " + cmpPrefix + " " + condition + " " + operandType + " " + left + ", " + right;
            } else {
                instruction = destReg + " = " + arithOp + " nsw " + operandType + " " + left + ", " + right;
            }
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

    // --- Literais ---

    private String visitIntValNode(AST node) {
        return String.valueOf(node.intData);
    }
    
    private String visitRealValNode(AST node) {
        return String.valueOf((double)node.floatData);
    }

    private String visitBoolValNode(AST node) {
        return node.boolData ? "1" : "0";
    }

    private String visitStrValNode(AST node) {
        String strName = createGlobalString(node.text, ".str");
        int strLen = node.text.length() + 1;
        
        String ptrReg = newReg();
        emit(ptrReg + " = getelementptr inbounds [" + strLen + " x i8], [" + strLen + " x i8]* " + strName + ", i64 0, i64 0");
        return ptrReg;
    }
}