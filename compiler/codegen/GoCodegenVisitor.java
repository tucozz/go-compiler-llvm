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

    // Classe interna para representar uma entrada na tabela de símbolos
    private static class SymbolTableEntry {
        final boolean isConstant;
        final String value; // Para constantes, é o valor literal. Para variáveis, é o ponteiro.
        final GoType type;

        SymbolTableEntry(boolean isConstant, String value, GoType type) {
            this.isConstant = isConstant;
            this.value = value;
            this.type = type;
        }
    }

    private StringBuilder irBuilder;
    private StringBuilder headerBuilder;

    private int regCounter;
    private int labelCounter;
    private int strCounter;

    // Tabela de símbolos agora armazena SymbolTableEntry
    private Stack<Map<String, SymbolTableEntry>> symbolTable;
    
    private Map<String, AST> functionDeclarations;
    private Map<GoType, String> printfFormatStrings;
    
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
            case FOR_COND_NODE:     return visitForCondNode(node);
            case BREAK_NODE:        return visitBreakNode(node);
            case CONTINUE_NODE:     return visitContinueNode(node);
            case INC_DEC_STMT_NODE: return visitIncDecStmtNode(node);
            case CONST_DECL_NODE:   return visitConstDeclNode(node); // <-- NOVO
            case CONST_SPEC_NODE:   return visitConstSpecNode(node); // <-- NOVO
            case VAR_DECL_NODE:     return visitVarDeclNode(node);
            case VAR_SPEC_NODE:     return visitVarSpecNode(node);
            case SHORT_VAR_DECL_NODE: return visitShortVarDeclNode(node);
            case ASSIGN_NODE:       return visitAssignNode(node);
            case RETURN_NODE:       return visitReturnNode(node);
            
            // Expressões
            case CALL_NODE:         return visitCallNode(node);
            case TYPE_CONV_NODE:    return visitTypeConvNode(node);
            case UNARY_MINUS_NODE:  return visitUnaryMinusNode(node);
            case NOT_NODE:          return visitNotNode(node);
            case AND_NODE:          return visitAndNode(node);
            case OR_NODE:           return visitOrNode(node);
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
            case STR_VAL_NODE: {
                String strName = createGlobalString(node.text, ".str");
                int strLen = node.text.length() + 1;
                String ptrReg = newReg();
                emit(ptrReg + " = getelementptr inbounds [" + strLen + " x i8], [" + strLen + " x i8]* " + strName + ", i64 0, i64 0");
                return ptrReg;
            }
            case ID_NODE:           return visitIdNode(node);
            case INDEX_NODE:       return visitIndexNode(node);

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
            case ARRAY_INT: return "[10 x i32]*"; // Array de 10 inteiros (tamanho fixo por enquanto)
            case ARRAY_BOOL: return "[10 x i1]*"; // Array de 10 booleanos
            case ARRAY_FLOAT64: return "[10 x double]*"; // Array de 10 floats
            case ARRAY_STRING: return "[10 x i8*]*"; // Array de 10 ponteiros para string
            case VOID: return "void";
            default: return "void";
        }
    }

    private String getLLVMTypeForAlloc(GoType goType) {
        switch (goType) {
            case INT: case INT32: return "i32";
            case BOOL: return "i1";
            case FLOAT64: return "double";
            case STRING: return "i8*";
            case ARRAY_INT: return "[10 x i32]"; // Array de 10 inteiros (sem ponteiro para alocação)
            case ARRAY_BOOL: return "[10 x i1]"; // Array de 10 booleanos
            case ARRAY_FLOAT64: return "[10 x double]"; // Array de 10 floats
            case ARRAY_STRING: return "[10 x i8*]"; // Array de 10 ponteiros para string
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
            GoType paramType = paramIdNode.getAnnotatedType();
            String llvmType = getLLVMType(paramType);
            String pointerName = "%" + paramName + ".addr";

            if (paramType.isArray()) {
                // Para arrays, o parâmetro já é um ponteiro, então apenas armazena diretamente
                symbolTable.peek().put(paramName, new SymbolTableEntry(false, "%" + i, paramType));
            } else {
                // Para tipos simples, aloca espaço e armazena o valor
                emit(pointerName + " = alloca " + llvmType);
                emit("store " + llvmType + " %" + i + ", " + llvmType + "* " + pointerName);
                symbolTable.peek().put(paramName, new SymbolTableEntry(false, pointerName, paramType));
            }
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
    
    private String visitForCondNode(AST node) {
        AST condNode = node.getChild(0);
        AST bodyNode = node.getChild(1);

        String condLabel = newLabel("loop.cond");
        String bodyLabel = newLabel("loop.body");
        String endLabel = newLabel("loop.end");

        loopPostLabels.push(condLabel);
        loopEndLabels.push(endLabel);

        emit("br label %" + condLabel);

        emitLabel(condLabel);
        if (condNode != null) {
            String condValue = visit(condNode);
            emit("br i1 " + condValue + ", label %" + bodyLabel + ", label %" + endLabel);
        } else {
            emit("br label %" + bodyLabel);
        }

        emitLabel(bodyLabel);
        visit(bodyNode);
        if (irBuilder.length() > 0 && !irBuilder.toString().trim().endsWith("ret") && !irBuilder.toString().trim().endsWith("br")) {
            emit("br label %" + condLabel);
        }

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
        SymbolTableEntry entry = symbolTable.peek().get(varName);
        String pointerName = entry.value;
        String llvmType = getLLVMType(entry.type);
        
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

    private String visitConstDeclNode(AST node) {
        for (AST spec : node.getChildren()) visit(spec);
        return "";
    }
    
    private String visitConstSpecNode(AST node) {
        AST exprListNode = node.getChild(node.getChildCount() - 1);
        int exprIndex = 0;
        for (AST idNode : node.getChildren()) {
            if (idNode.kind != NodeKind.ID_NODE) continue;
            
            String constName = idNode.text;
            GoType constType = idNode.getAnnotatedType();
            
            // Evaluate the constant expression at compile time
            String value = evaluateConstantExpression(exprListNode.getChild(exprIndex));
            
            symbolTable.peek().put(constName, new SymbolTableEntry(true, value, constType));
            exprIndex++;
        }
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
            GoType varType = idNode.getAnnotatedType();
            String llvmType = getLLVMType(varType);
            String pointerName = "%" + varName + ".addr";

            if (varType.isArray()) {
                // Para arrays, aloca espaço na pilha
                emit(pointerName + " = alloca " + getLLVMTypeForAlloc(varType));
            } else {
                // Para tipos simples, aloca normalmente
                emit(pointerName + " = alloca " + llvmType);
            }
            
            symbolTable.peek().put(varName, new SymbolTableEntry(false, pointerName, varType));

            if (exprListNode != null && exprIndex < exprListNode.getChildCount()) {
                String value = visit(exprListNode.getChild(exprIndex));
                if (varType.isArray()) {
                    // Para arrays, o valor deve ser tratado especialmente
                    // Por enquanto, apenas ignora a inicialização de arrays
                } else {
                    emit("store " + llvmType + " " + value + ", " + llvmType + "* " + pointerName);
                }
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
            GoType varType = idNode.getAnnotatedType();
            String llvmType = getLLVMType(varType);
            String pointerName = "%" + varName + ".addr";

            emit(pointerName + " = alloca " + llvmType);
            symbolTable.peek().put(varName, new SymbolTableEntry(false, pointerName, varType));

            String value = visit(exprNode);
            emit("store " + llvmType + " " + value + ", " + llvmType + "* " + pointerName);
        }
        return "";
    }

    private String visitAssignNode(AST node) {
        System.out.println("DEBUG: Assignment node has " + node.getChildCount() + " children");
        for (int i = 0; i < node.getChildCount(); i++) {
            AST child = node.getChild(i);
            if (child != null) {
                System.out.println("DEBUG: Child " + i + " kind: " + child.kind + ", text: " + child.text);
            } else {
                System.out.println("DEBUG: Child " + i + " is null");
            }
        }
        
        AST lvalueNode = node.getChild(0);
        AST rvalueNode = node.getChild(1);
        
        if (lvalueNode == null || rvalueNode == null) {
            System.out.println("DEBUG: Skipping assignment with null children");
            return "";
        }
        
        if (lvalueNode.kind == NodeKind.INDEX_NODE) {
            // Atribuição para elemento de array: arr[i] = value
            AST arrayNode = lvalueNode.getChild(0);
            AST indexNode = lvalueNode.getChild(1);
            
            if (arrayNode == null || indexNode == null) {
                System.out.println("DEBUG: Skipping array assignment with null array or index");
                return "";
            }
            
            // Visita o array e o índice
            String arrayPtr = visit(arrayNode);
            String indexValue = visit(indexNode);
            
            // Visita o valor a ser atribuído
            String value = visit(rvalueNode);
            
            // Obtém o tipo do elemento
            GoType arrayType = arrayNode.getAnnotatedType();
            GoType elementType = arrayType.getElementType();
            String elementLLVMType = getLLVMType(elementType);
            
            // Remove o '*' do final do tipo do array
            String arrayBaseType = getLLVMType(arrayType).replace("*", "");
            
            // Gera getelementptr para obter o ponteiro do elemento
            String elementPtr = newReg();
            emit(elementPtr + " = getelementptr inbounds " + arrayBaseType + ", " + 
                 getLLVMType(arrayType) + " " + arrayPtr + ", i64 0, i32 " + indexValue);
            
            // Armazena o valor no elemento
            emit("store " + elementLLVMType + " " + value + ", " + elementLLVMType + "* " + elementPtr);
        } else {
            // Atribuição normal para variável
            String varName = lvalueNode.text;
            SymbolTableEntry entry = symbolTable.peek().get(varName);
            String pointerName = entry.value;
            String llvmType = getLLVMType(entry.type);

            String value = visit(rvalueNode);
            emit("store " + llvmType + " " + value + ", " + llvmType + "* " + pointerName);
        }
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

    private String visitUnaryMinusNode(AST node) {
        GoType type = node.getAnnotatedType();
        String llvmType = getLLVMType(type);
        String value = visit(node.getChild(0));
        String destReg = newReg();

        if (type.isFloat()) {
            emit(destReg + " = fsub " + llvmType + " 0.0, " + value);
        } else { // Integer
            emit(destReg + " = sub nsw " + llvmType + " 0, " + value);
        }
        return destReg;
    }

    private String visitNotNode(AST node) {
        String value = visit(node.getChild(0));
        String destReg = newReg();
        emit(destReg + " = xor i1 " + value + ", 1");
        return destReg;
    }

    private String visitAndNode(AST node) {
        String entryLabel = newLabel("and.entry");
        String evalRhsLabel = newLabel("and.eval_rhs");
        String endLabel = newLabel("and.end");
        
        emit("br label %" + entryLabel);
        emitLabel(entryLabel);
        String lhsValue = visit(node.getChild(0));
        emit("br i1 " + lhsValue + ", label %" + evalRhsLabel + ", label %" + endLabel);

        emitLabel(evalRhsLabel);
        String rhsValue = visit(node.getChild(1));
        emit("br label %" + endLabel);

        emitLabel(endLabel);
        String destReg = newReg();
        emit(destReg + " = phi i1 [ false, %" + entryLabel + " ], [ " + rhsValue + ", %" + evalRhsLabel + " ]");
        return destReg;
    }

    private String visitOrNode(AST node) {
        String entryLabel = newLabel("or.entry");
        String evalRhsLabel = newLabel("or.eval_rhs");
        String endLabel = newLabel("or.end");

        emit("br label %" + entryLabel);
        emitLabel(entryLabel);
        String lhsValue = visit(node.getChild(0));
        emit("br i1 " + lhsValue + ", label %" + endLabel + ", label %" + evalRhsLabel);

        emitLabel(evalRhsLabel);
        String rhsValue = visit(node.getChild(1));
        emit("br label %" + endLabel);

        emitLabel(endLabel);
        String destReg = newReg();
        emit(destReg + " = phi i1 [ true, %" + entryLabel + " ], [ " + rhsValue + ", %" + evalRhsLabel + " ]");
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
        SymbolTableEntry entry = symbolTable.peek().get(varName);

        if (entry.isConstant) {
            return entry.value; // Retorna o valor literal diretamente
        } else {
            // Para arrays, retorna o ponteiro diretamente
            if (entry.type.isArray()) {
                return entry.value;
            } else {
                // Para variáveis simples, carrega o valor do ponteiro
                String pointerName = entry.value;
                String llvmType = getLLVMType(entry.type);
                String destReg = newReg();
                emit(destReg + " = load " + llvmType + ", " + llvmType + "* " + pointerName);
                return destReg;
            }
        }
    }

    private String visitIndexNode(AST node) {
        AST arrayNode = node.getChild(0); // Nó do array
        AST indexNode = node.getChild(1); // Nó do índice
        
        // Visita o nó do array para obter seu ponteiro
        String arrayPtr = visit(arrayNode);
        
        // Visita o nó do índice para obter o valor do índice
        String indexValue = visit(indexNode);
        
        // Obtém o tipo do elemento do array
        GoType arrayType = arrayNode.getAnnotatedType();
        GoType elementType = arrayType.getElementType();
        String elementLLVMType = getLLVMType(elementType);
        
        // Remove o '*' do final do tipo do array para obter o tipo base
        String arrayBaseType = getLLVMType(arrayType).replace("*", "");
        
        // Gera getelementptr para acessar o elemento
        String elementPtr = newReg();
        emit(elementPtr + " = getelementptr inbounds " + arrayBaseType + ", " + 
             getLLVMType(arrayType) + " " + arrayPtr + ", i64 0, i32 " + indexValue);
        
        // Carrega o valor do elemento
        String elementValue = newReg();
        emit(elementValue + " = load " + elementLLVMType + ", " + elementLLVMType + "* " + elementPtr);
        
        return elementValue;
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
        String strName = createGlobalString(node.text, ".const.str");
        int strLen = node.text.length() + 1;
        
        String ptrReg = newReg();
        emit(ptrReg + " = getelementptr inbounds [" + strLen + " x i8], [" + strLen + " x i8]* " + strName + ", i64 0, i64 0");
        return ptrReg;
    }

    // --- Constant Expression Evaluator ---

    private String evaluateConstantExpression(AST node) {
        if (node == null) return "0";

        switch (node.kind) {
            case INT_VAL_NODE: return String.valueOf(node.intData);
            case REAL_VAL_NODE: return String.valueOf((double)node.floatData);
            case BOOL_VAL_NODE: return node.boolData ? "1" : "0";
            case STR_VAL_NODE: {
                String strName = createGlobalString(node.text, ".str");
                int strLen = node.text.length() + 1;
                String ptrReg = newReg();
                emit(ptrReg + " = getelementptr inbounds [" + strLen + " x i8], [" + strLen + " x i8]* " + strName + ", i64 0, i64 0");
                return ptrReg;
            }
            case ID_NODE: {
                SymbolTableEntry entry = symbolTable.peek().get(node.text);
                if (entry != null && entry.isConstant) {
                    return entry.value;
                }
                throw new RuntimeException("Undefined constant: " + node.text);
            }
            case PLUS_NODE: {
                String left = evaluateConstantExpression(node.getChild(0));
                String right = evaluateConstantExpression(node.getChild(1));
                GoType type = node.getAnnotatedType();
                if (type.isFloat()) {
                    double l = Double.parseDouble(left);
                    double r = Double.parseDouble(right);
                    return String.valueOf(l + r);
                } else {
                    long l = Long.parseLong(left);
                    long r = Long.parseLong(right);
                    return String.valueOf(l + r);
                }
            }
            case MINUS_NODE: {
                String left = evaluateConstantExpression(node.getChild(0));
                String right = evaluateConstantExpression(node.getChild(1));
                GoType type = node.getAnnotatedType();
                if (type.isFloat()) {
                    double l = Double.parseDouble(left);
                    double r = Double.parseDouble(right);
                    return String.valueOf(l - r);
                } else {
                    long l = Long.parseLong(left);
                    long r = Long.parseLong(right);
                    return String.valueOf(l - r);
                }
            }
            case TIMES_NODE: {
                String left = evaluateConstantExpression(node.getChild(0));
                String right = evaluateConstantExpression(node.getChild(1));
                GoType type = node.getAnnotatedType();
                if (type.isFloat()) {
                    double l = Double.parseDouble(left);
                    double r = Double.parseDouble(right);
                    return String.valueOf(l * r);
                } else {
                    long l = Long.parseLong(left);
                    long r = Long.parseLong(right);
                    return String.valueOf(l * r);
                }
            }
            case OVER_NODE: {
                String left = evaluateConstantExpression(node.getChild(0));
                String right = evaluateConstantExpression(node.getChild(1));
                GoType type = node.getAnnotatedType();
                if (type.isFloat()) {
                    double l = Double.parseDouble(left);
                    double r = Double.parseDouble(right);
                    return String.valueOf(l / r);
                } else {
                    long l = Long.parseLong(left);
                    long r = Long.parseLong(right);
                    return String.valueOf(l / r);
                }
            }
            case MOD_NODE: {
                String left = evaluateConstantExpression(node.getChild(0));
                String right = evaluateConstantExpression(node.getChild(1));
                long l = Long.parseLong(left);
                long r = Long.parseLong(right);
                return String.valueOf(l % r);
            }
            case UNARY_MINUS_NODE: {
                String value = evaluateConstantExpression(node.getChild(0));
                GoType type = node.getAnnotatedType();
                if (type.isFloat()) {
                    double v = Double.parseDouble(value);
                    return String.valueOf(-v);
                } else {
                    long v = Long.parseLong(value);
                    return String.valueOf(-v);
                }
            }
            case NOT_NODE: {
                String value = evaluateConstantExpression(node.getChild(0));
                boolean v = Boolean.parseBoolean(value);
                return v ? "0" : "1";
            }
            case TYPE_CONV_NODE: {
                String value = evaluateConstantExpression(node.getChild(0));
                GoType sourceType = node.getChild(0).getAnnotatedType();
                GoType targetType = node.getAnnotatedType();
                if (sourceType.isInteger() && targetType.isFloat()) {
                    long v = Long.parseLong(value);
                    return String.valueOf((double)v);
                } else if (sourceType.isFloat() && targetType.isInteger()) {
                    double v = Double.parseDouble(value);
                    return String.valueOf((long)v);
                }
                return value;
            }
            default:
                throw new RuntimeException("Unsupported constant expression: " + node.kind);
        }
    }
}
