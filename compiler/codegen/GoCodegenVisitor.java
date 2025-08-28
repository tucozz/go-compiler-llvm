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

    // Define um tamanho fixo para buffers de string alocados na pilha.
    private static final int STRING_BUFFER_SIZE = 1024;

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
    private Map<String, SymbolTableEntry> globalSymbols; // Para variáveis globais
    
    private Map<String, AST> functionDeclarations;
    private Map<GoType, String> printfFormatStrings;
    private Map<GoType, String> scanfFormatStrings; // Cache para strings de formato do scanf
    private Map<String, Integer> stringLengths; // Cache para comprimentos de strings

    private Stack<String> loopPostLabels;
    private Stack<String> loopEndLabels;
    private boolean lastInstructionWasTerminator;


    public GoCodegenVisitor() {
        this.irBuilder = new StringBuilder();
        this.headerBuilder = new StringBuilder();
        this.regCounter = 0;
        this.labelCounter = 0;
        this.strCounter = 0;
        this.symbolTable = new Stack<>();
        this.globalSymbols = new HashMap<>();
        this.functionDeclarations = new HashMap<>();
        this.printfFormatStrings = new HashMap<>();
        this.scanfFormatStrings = new HashMap<>(); // Inicializa o mapa do scanf
        this.stringLengths = new HashMap<>(); // Inicializa o mapa dos comprimentos
        this.loopPostLabels = new Stack<>();
        this.loopEndLabels = new Stack<>();
        this.lastInstructionWasTerminator = false;
    }

    public String run(AST root) {
        // Declara as funções da biblioteca C que serão usadas
        headerBuilder.append("declare i32 @printf(i8*, ...)\n");
        headerBuilder.append("declare i32 @scanf(i8*, ...)\n\n");

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
            case CONST_DECL_NODE:   return visitConstDeclNode(node);
            case CONST_SPEC_NODE:   return visitConstSpecNode(node);
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
                int strLen = stringLengths.get(strName);
                String ptrReg = newReg();
                emit(ptrReg + " = getelementptr inbounds [" + strLen + " x i8], [" + strLen + " x i8]* " + strName + ", i64 0, i64 0");
                return ptrReg;
            }
            case ID_NODE:           return visitIdNode(node);
            case INDEX_NODE:        return visitIndexNode(node);
            case COMPOSITE_LITERAL_NODE: return visitCompositeLiteralNode(node);
            
            // Lists and specifications (mostly just visit children)
            case EXPR_LIST_NODE:
            case IDENTIFIER_LIST_NODE:
            case PARAM_LIST_NODE:
            case EXPR_STMT_NODE:
                for (AST child : node.getChildren()) {
                    visit(child);
                }
                return "";

            default:
                System.err.println("WARNING: Unhandled node type: " + node.kind + " (text: " + node.text + ")");
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
        String escapedValue = value.replace("\\", "\\5C"); // LLVM requer que a barra invertida seja escapada
        // Calculate the actual length after escaping, plus 1 for null terminator
        int len = escapedValue.length() + 1;
        headerBuilder.append(strName).append(" = private unnamed_addr constant [")
                     .append(len).append(" x i8] c\"").append(escapedValue).append("\\00\"\n");
        
        // Store the length for later use
        stringLengths.put(strName, len);
        
        return strName;
    }

    private void emit(String instruction) { 
        irBuilder.append("\t").append(instruction).append("\n");
        // Check if this is a terminator instruction
        String trimmed = instruction.trim().toLowerCase();
        lastInstructionWasTerminator = trimmed.startsWith("ret ") || trimmed.startsWith("br ");
    }
    private void emitLabel(String label) { 
        irBuilder.append(label).append(":\n");
        lastInstructionWasTerminator = false; // Labels reset the terminator flag
    }

    private String getLLVMType(GoType goType) {
        switch (goType) {
            case INT: case INT32: return "i32";
            case BOOL: return "i1";
            case FLOAT64: return "double";
            case STRING: return "i8*"; // Semanticamente, uma string é um ponteiro para char
            case ARRAY_INT: return "[10 x i32]*";
            case ARRAY_BOOL: return "[10 x i1]*";
            case ARRAY_FLOAT64: return "[10 x double]*";
            case ARRAY_STRING: return "[10 x i8*]*";
            case VOID: return "void";
            default: return "void";
        }
    }

    /**
     * CORREÇÃO: Retorna o tipo de dado para uma instrução 'alloca'.
     * Para strings, alocamos um buffer de tamanho fixo.
     */
    private String getLLVMTypeForAlloc(GoType goType) {
        switch (goType) {
            case INT: case INT32: return "i32";
            case BOOL: return "i1";
            case FLOAT64: return "double";
            case STRING: return "[" + STRING_BUFFER_SIZE + " x i8]"; // Aloca um buffer de char
            case ARRAY_INT: return "[10 x i32]";
            case ARRAY_BOOL: return "[10 x i1]";
            case ARRAY_FLOAT64: return "[10 x double]";
            case ARRAY_STRING: return "[10 x i8*]";
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
        String formatStrName = createGlobalString(formatSpecifier, ".fmt.printf");
        printfFormatStrings.put(type, formatStrName);
        return formatStrName;
    }

    private String getOrCreateScanfFormatString(GoType type) {
        if (scanfFormatStrings.containsKey(type)) {
            return scanfFormatStrings.get(type);
        }
        String formatSpecifier;
        switch (type) {
            case INT: case INT32: formatSpecifier = "%d"; break;
            case BOOL: formatSpecifier = "%d"; break;
            case FLOAT64: formatSpecifier = "%lf"; break;
            case STRING: formatSpecifier = "%s"; break;
            default: formatSpecifier = "";
        }
        String formatStrName = createGlobalString(formatSpecifier, ".fmt.scanf");
        scanfFormatStrings.put(type, formatStrName);
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
            String pointerName = "%" + paramName + ".addr." + regCounter++;

            if (paramType.isArray()) {
                symbolTable.peek().put(paramName, new SymbolTableEntry(false, "%" + i, paramType));
            } else if (paramType == GoType.STRING) {
                // For string parameters, we directly use the pointer without additional allocation
                String llvmType = getLLVMType(paramType); // This is i8*
                emit(pointerName + " = alloca " + llvmType);
                emit("store " + llvmType + " %" + i + ", " + llvmType + "* " + pointerName);
                symbolTable.peek().put(paramName, new SymbolTableEntry(false, pointerName, paramType));
            } else {
                String llvmAllocType = getLLVMTypeForAlloc(paramType);
                emit(pointerName + " = alloca " + llvmAllocType);
                emit("store " + getLLVMType(paramType) + " %" + i + ", " + getLLVMType(paramType) + "* " + pointerName);
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

        // Process THEN branch
        emitLabel(thenLabel);
        visit(node.getChild(1));
        boolean thenHasTerminator = lastInstructionWasTerminator;
        if (!thenHasTerminator) {
            emit("br label %" + endLabel);
        }

        // Process ELSE branch (if exists)
        boolean elseHasTerminator = false;
        if (hasElse) {
            emitLabel(elseLabel);
            visit(node.getChild(2));
            elseHasTerminator = lastInstructionWasTerminator;
            if (!elseHasTerminator) {
                emit("br label %" + endLabel);
            }
        }

        // Only emit the end label if at least one branch needs it
        if (!thenHasTerminator || (hasElse && !elseHasTerminator) || !hasElse) {
            emitLabel(endLabel);
        }
        
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
        if (condNode != null) {
            String condValue = visit(condNode);
            emit("br i1 " + condValue + ", label %" + bodyLabel + ", label %" + endLabel);
        } else {
            // For infinite loop (empty condition), always branch to body
            emit("br label %" + bodyLabel);
        }

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
    
    /**
     * CORREÇÃO: Usa getLLVMTypeForAlloc para alocar espaço para variáveis,
     * tratando strings como buffers.
     */
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
            String llvmAllocType = getLLVMTypeForAlloc(varType);
            
            // Check if we're in a function scope or global scope
            if (symbolTable.isEmpty()) {
                // Global variable - create as global
                String globalName = "@" + varName + ".global";
                headerBuilder.append(globalName).append(" = global ")
                             .append(llvmAllocType).append(" ");
                
                // Initialize with default value
                if (varType.isInteger()) {
                    headerBuilder.append("0");
                } else if (varType.isFloat()) {
                    headerBuilder.append("0.0");
                } else if (varType == GoType.BOOL) {
                    headerBuilder.append("false");
                } else {
                    headerBuilder.append("zeroinitializer");
                }
                headerBuilder.append("\n");
                
                // Add to global symbol table
                globalSymbols.put(varName, new SymbolTableEntry(false, globalName, varType));
            } else {
                // Local variable
                String pointerName = "%" + varName + ".addr." + regCounter++;
                // For strings, allocate space for a pointer, not a buffer
                String allocType = (varType == GoType.STRING) ? getLLVMType(varType) : getLLVMTypeForAlloc(varType);
                emit(pointerName + " = alloca " + allocType);
                symbolTable.peek().put(varName, new SymbolTableEntry(false, pointerName, varType));

                if (exprListNode != null && exprIndex < exprListNode.getChildCount()) {
                    String value = visit(exprListNode.getChild(exprIndex));
                    if (!varType.isArray()) {
                        emit("store " + getLLVMType(varType) + " " + value + ", " + getLLVMType(varType) + "* " + pointerName);
                    }
                    exprIndex++;
                }
            }
        }
        return "";
    }
    
    /**
     * CORREÇÃO: Usa getLLVMTypeForAlloc para alocar espaço para variáveis,
     * tratando strings como buffers.
     */
    private String visitShortVarDeclNode(AST node) {
        AST idListNode = node.getChild(0);
        AST exprListNode = node.getChild(1);

        for (int i = 0; i < idListNode.getChildCount(); i++) {
            AST idNode = idListNode.getChild(i);
            AST exprNode = exprListNode.getChild(i);
            String varName = idNode.text;
            GoType varType = idNode.getAnnotatedType();
            // For strings, allocate space for a pointer, not a buffer
            String allocType = (varType == GoType.STRING) ? getLLVMType(varType) : getLLVMTypeForAlloc(varType);
            String pointerName = "%" + varName + ".addr." + regCounter++;

            emit(pointerName + " = alloca " + allocType);
            symbolTable.peek().put(varName, new SymbolTableEntry(false, pointerName, varType));

            String value = visit(exprNode);
            // Store the value
            if (!value.isEmpty()) {
                emit("store " + getLLVMType(varType) + " " + value + ", " + getLLVMType(varType) + "* " + pointerName);
            }
        }
        return "";
    }

    private String visitAssignNode(AST node) {
        AST lvalueNode = node.getChild(0);
        AST rvalueNode = node.getChild(1);
        
        if (lvalueNode == null || rvalueNode == null) {
            // Handle null children gracefully - this may be due to unsupported AST structures
            return "";
        }
        
        if (lvalueNode.kind == NodeKind.INDEX_NODE) {
            AST arrayNode = lvalueNode.getChild(0);
            AST indexNode = lvalueNode.getChild(1);
            
            String arrayPtr = visit(arrayNode);
            String indexValue = visit(indexNode);
            String value = visit(rvalueNode);
            
            GoType arrayType = arrayNode.getAnnotatedType();
            GoType elementType = arrayType.getElementType();
            String elementLLVMType = getLLVMType(elementType);
            String arrayBaseType = getLLVMType(arrayType).replace("*", "");
            
            String elementPtr = newReg();
            emit(elementPtr + " = getelementptr inbounds " + arrayBaseType + ", " + 
                 getLLVMType(arrayType) + " " + arrayPtr + ", i64 0, i32 " + indexValue);
            
            emit("store " + elementLLVMType + " " + value + ", " + elementLLVMType + "* " + elementPtr);
        } else {
            String varName = lvalueNode.text;
            SymbolTableEntry entry = null;
            
            // First try to find in local scope
            if (!symbolTable.isEmpty()) {
                entry = symbolTable.peek().get(varName);
            }
            
            // If not found in local scope, try global scope
            if (entry == null) {
                entry = globalSymbols.get(varName);
            }
            
            if (entry == null) {
                System.err.println("ERROR: Variable not found for assignment: " + varName);
                return "";
            }
            
            // Atribuição a strings não é suportada (requer strcpy)
            if (entry.type == GoType.STRING) {
                // Ignora por enquanto
            } else {
                String pointerName = entry.value;
                String llvmType = getLLVMType(entry.type);
                String value = visit(rvalueNode);
                emit("store " + llvmType + " " + value + ", " + llvmType + "* " + pointerName);
            }
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
                int formatStrLen = stringLengths.get(formatStrName);

                String formatStrPtr = newReg();
                emit(formatStrPtr + " = getelementptr inbounds [" + formatStrLen + " x i8], [" + formatStrLen + " x i8]* " + formatStrName + ", i64 0, i64 0");
                
                String llvmType = getLLVMType(argType);
                if (argType == GoType.BOOL) {
                    String extendedBool = newReg();
                    emit(extendedBool + " = zext i1 " + argValue + " to i32");
                    argValue = extendedBool;
                    llvmType = "i32";
                }

                emit("call i32 (i8*, ...) @printf(i8* " + formatStrPtr + ", " + llvmType + " " + argValue + ")");
            }
            return "";
        }

        if (funcName.equals("scanln")) {
            String totalScannedReg = newReg();
            emit(totalScannedReg + " = alloca i32");
            emit("store i32 0, i32* " + totalScannedReg);

            for (int i = 1; i < node.getChildCount(); i++) {
                AST argNode = node.getChild(i);
                if (argNode.kind != NodeKind.ID_NODE) {
                    continue;
                }
                
                String varName = argNode.text;
                SymbolTableEntry entry = symbolTable.peek().get(varName);
                String varPtr = entry.value;
                GoType varType = entry.type;

                String formatStrName = getOrCreateScanfFormatString(varType);
                if (formatStrName.isEmpty()) continue;

                int formatStrLen = stringLengths.get(formatStrName);

                String formatStrPtr = newReg();
                emit(formatStrPtr + " = getelementptr inbounds [" + formatStrLen + " x i8], [" + formatStrLen + " x i8]* " + formatStrName + ", i64 0, i64 0");

                String scanResultReg = newReg();
                
                // CORREÇÃO: Trata a chamada a scanf para cada tipo
                if (varType == GoType.STRING) {
                    String bufferPtr = newReg();
                    String bufferType = "[" + STRING_BUFFER_SIZE + " x i8]";
                    // Obtém um i8* para o início do buffer
                    emit(bufferPtr + " = getelementptr inbounds " + bufferType + ", " + bufferType + "* " + varPtr + ", i64 0, i64 0");
                    emit(scanResultReg + " = call i32 (i8*, ...) @scanf(i8* " + formatStrPtr + ", i8* " + bufferPtr + ")");
                } else if (varType == GoType.BOOL) {
                    String tempInt = newReg();
                    emit(tempInt + " = alloca i32");
                    emit(scanResultReg + " = call i32 (i8*, ...) @scanf(i8* " + formatStrPtr + ", i32* " + tempInt + ")");
                    
                    String loadedInt = newReg();
                    emit(loadedInt + " = load i32, i32* " + tempInt);
                    
                    String boolResult = newReg();
                    emit(boolResult + " = icmp ne i32 " + loadedInt + ", 0");
                    emit("store i1 " + boolResult + ", i1* " + varPtr);
                } else {
                    // Para tipos numéricos, passa o ponteiro diretamente
                    emit(scanResultReg + " = call i32 (i8*, ...) @scanf(i8* " + formatStrPtr + ", " + getLLVMType(varType) + "* " + varPtr + ")");
                }

                String currentTotal = newReg();
                emit(currentTotal + " = load i32, i32* " + totalScannedReg);
                String newTotal = newReg();
                emit(newTotal + " = add nsw i32 " + currentTotal + ", " + scanResultReg);
                emit("store i32 " + newTotal + ", i32* " + totalScannedReg);
            }

            String finalCount = newReg();
            emit(finalCount + " = load i32, i32* " + totalScannedReg);
            return finalCount;
        }

        List<String> argValues = new ArrayList<>();
        for (int i = 1; i < node.getChildCount(); i++) {
            argValues.add(visit(node.getChild(i)));
        }

        AST funcDecl = functionDeclarations.get(funcName);
        if (funcDecl == null) {
            // Function not found in declarations (built-in or undefined)
            // For built-in functions, we handle them above
            // For user functions, this shouldn't happen, but handle gracefully
            System.err.println("WARNING: Function declaration not found: " + funcName);
            return "";
        }
        
        AST paramListNode = funcDecl.getChild(1);
        List<String> typedArgs = new ArrayList<>();
        for (int i = 0; i < argValues.size(); i++) {
            if (i < paramListNode.getChildCount()) {
                AST paramNode = paramListNode.getChild(i).getChild(0);
                String paramType = getLLVMType(paramNode.getAnnotatedType());
                typedArgs.add(paramType + " " + argValues.get(i));
            }
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
        } else {
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
                // Use signed predicates for integers
                Map<String, String> intConditions = Map.of("eq", "eq", "ne", "ne", "lt", "slt", "le", "sle", "gt", "sgt", "ge", "sge");
                instruction = destReg + " = " + cmpPrefix + " " + intConditions.get(condition) + " " + operandType + " " + left + ", " + right;
            } else {
                // Don't use nsw with srem and sdiv
                if (arithOp.equals("srem") || arithOp.equals("sdiv")) {
                    instruction = destReg + " = " + arithOp + " " + operandType + " " + left + ", " + right;
                } else {
                    instruction = destReg + " = " + arithOp + " nsw " + operandType + " " + left + ", " + right;
                }
            }
        }
        emit(instruction);
        return destReg;
    }

    /**
     * CORREÇÃO: Ao acessar um ID de string, retorna um ponteiro para o início
     * do buffer alocado, em vez de tentar carregar um valor.
     */
    private String visitIdNode(AST node) {
        String varName = node.text;
        SymbolTableEntry entry = null;
        
        // First try to find in local scope
        if (!symbolTable.isEmpty()) {
            entry = symbolTable.peek().get(varName);
        }
        
        // If not found in local scope, try global scope
        if (entry == null) {
            entry = globalSymbols.get(varName);
        }
        
        if (entry == null) {
            System.err.println("ERROR: Variable not found: " + varName);
            return ""; // Or throw an exception
        }

        if (entry.isConstant) {
            return entry.value;
        } else {
            String pointerName = entry.value;
            if (entry.type == GoType.STRING) {
                // Para strings, não carregamos. Obtemos um ponteiro para o primeiro elemento.
                String destReg = newReg();
                String bufferType = getLLVMTypeForAlloc(GoType.STRING);
                emit(destReg + " = getelementptr inbounds " + bufferType + ", " + bufferType + "* " + pointerName + ", i64 0, i64 0");
                return destReg; // Retorna um i8*
            } else if (entry.type.isArray()) {
                return entry.value;
            } else {
                // Para outros tipos simples, carrega o valor do ponteiro
                String llvmType = getLLVMType(entry.type);
                String destReg = newReg();
                emit(destReg + " = load " + llvmType + ", " + llvmType + "* " + pointerName);
                return destReg;
            }
        }
    }

    private String visitIndexNode(AST node) {
        AST arrayNode = node.getChild(0);
        AST indexNode = node.getChild(1);
        
        String arrayPtr = visit(arrayNode);
        String indexValue = visit(indexNode);
        
        GoType arrayType = arrayNode.getAnnotatedType();
        GoType elementType = arrayType.getElementType();
        String elementLLVMType = getLLVMType(elementType);
        String arrayBaseType = getLLVMType(arrayType).replace("*", "");
        
        String elementPtr = newReg();
        emit(elementPtr + " = getelementptr inbounds " + arrayBaseType + ", " + 
             getLLVMType(arrayType) + " " + arrayPtr + ", i64 0, i32 " + indexValue);
        
        String elementValue = newReg();
        emit(elementValue + " = load " + elementLLVMType + ", " + elementLLVMType + "* " + elementPtr);
        
        return elementValue;
    }

    private String visitCompositeLiteralNode(AST node) {
        // Handle composite literals - this is a simplified implementation
        GoType arrayType = node.getAnnotatedType();
        
        if (arrayType == null) {
            // If type is not annotated, skip for now
            return "";
        }
        
        String arrayBaseType = getLLVMType(arrayType).replace("*", "");
        
        // Create a temporary array and initialize it
        String arrayPtr = newReg();
        emit(arrayPtr + " = alloca " + arrayBaseType);
        
        // Initialize elements if present
        for (int i = 0; i < node.getChildCount(); i++) {
            AST elementNode = node.getChild(i);
            String elementValue = visit(elementNode);
            GoType elementType = arrayType.getElementType();
            String elementLLVMType = getLLVMType(elementType);
            
            String elementPtr = newReg();
            emit(elementPtr + " = getelementptr inbounds " + arrayBaseType + ", " + 
                 getLLVMType(arrayType) + " " + arrayPtr + ", i64 0, i32 " + i);
            emit("store " + elementLLVMType + " " + elementValue + ", " + elementLLVMType + "* " + elementPtr);
        }
        
        return arrayPtr;
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
