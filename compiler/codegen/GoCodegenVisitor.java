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
 * A saída é uma string de texto que pode ser salva em um arquivo .ll.
 */
public class GoCodegenVisitor {

    // Define um tamanho fixo para buffers de string alocados na pilha.
    private static final int STRING_BUFFER_SIZE = 1024;

    // Classe interna para representar uma entrada na tabela de símbolos específica para a geração de código LLVM
    private static class SymbolTableEntry {
        final boolean isConstant; // Se é constante ou variável
        final String value; // Valor literal (constantes) ou ponteiro LLVM (variáveis)
        final GoType type; // Tipo Go da variável/constante

        SymbolTableEntry(boolean isConstant, String value, GoType type) {
            this.isConstant = isConstant;
            this.value = value;
            this.type = type;
        }
    }

    private StringBuilder irBuilder; // Constrói o código LLVM das funções (instruções, labels, etc.)
    private StringBuilder headerBuilder; // Constrói declarações globais, constantes e protótipos de função
    //  LLVM:
    //  Módulo: Arquivo .ll completo
    //  Funções: Blocos de código executável
    //  Globais: Variáveis e constantes globais

    // Contadores para Nomes Únicos
    private int regCounter; // Gera nomes únicos para registradores virtuais (%0, %1, etc.)
    private int labelCounter; // Gera nomes únicos para labels de bloco (%if.then.0, etc.)
    private int strCounter; // Gera nomes únicos para strings globais
    //  LLVM:
    //  SSA (Static Single Assignment): Cada registrador virtual é atribuído apenas uma vez
    //  Registradores Virtuais: %nome são temporários criados pelo compilador
    //  Labels: Identificam blocos básicos para controle de fluxo

    
    private String currentBlock; // Rastreia o bloco básico atual
    private boolean lastInstructionWasTerminator; // Controla se a última instrução termina o bloco
    //  LLVM:
    //  Blocos Básicos: Sequências lineares de instruções terminadas por branch/return
    //  Terminators: Instruções que terminam blocos (br, ret, etc.) 

    private Stack<Map<String, SymbolTableEntry>> symbolTable; // Pilha de escopos para variáveis locais
    private Map<String, SymbolTableEntry> globalSymbols; // Mapa para variáveis globais
    //  LLVM:
    //  Escopos: LLVM não tem escopos implícitos - o compilador deve gerenciá-los
    //  Variáveis Locais: Alocadas na pilha com alloca
    //  Variáveis Globais: Declaradas no topo do módulo

    private Map<String, AST> functionDeclarations; // Cache das declarações de função para resolução
    private Map<GoType, String> printfFormatStrings; // Cache de strings de formato para I/O
    private Map<GoType, String> scanfFormatStrings; // Cache de strings de formato para I/O
    private Map<String, Integer> stringLengths; // Cache dos comprimentos das strings
    //  LLVM:
    //  Strings como Arrays: Strings são arrays de bytes ([N x i8])
    //  Constantes Globais: Strings são armazenadas como globais constantes
    //  Garbage Collection: Reuse de strings idênticas

    private Stack<String> loopPostLabels; // Pilha de labels para instruções continue
    private Stack<String> loopEndLabels; // Pilha de labels para instruções break
    //  LLVM:
    //  Estrutura de Loop: LLVM requer labels explícitas para controle de fluxo
    //  Break/Continue: Mapeiam para branches condicionais para labels específicas

    public GoCodegenVisitor() {
        this.irBuilder = new StringBuilder();
        this.headerBuilder = new StringBuilder();
        this.regCounter = 0;
        this.labelCounter = 0;
        this.strCounter = 0;
        this.currentBlock = "entry"; // Initialize with entry block
        this.symbolTable = new Stack<>();
        this.globalSymbols = new HashMap<>();
        this.functionDeclarations = new HashMap<>();
        this.printfFormatStrings = new HashMap<>();
        this.scanfFormatStrings = new HashMap<>();
        this.stringLengths = new HashMap<>();
        this.loopPostLabels = new Stack<>();
        this.loopEndLabels = new Stack<>();
        this.lastInstructionWasTerminator = false;
    }

    /**
     * Método principal do gerador de código LLVM IR.
     * Recebe a raiz da AST e gera o código LLVM completo como string.
     *
     * @param root Nó raiz da Abstract Syntax Tree (AST) do programa Go
     * @return String contendo o código LLVM IR completo (cabeçalho + funções)
     * @throws IllegalArgumentException se root for null
     */
    public String run(AST root) {
        // Validação de entrada - AST não pode ser null
        if (root == null) {
            throw new IllegalArgumentException("AST root cannot be null");
        }

        // FASE 1: Configuração do cabeçalho LLVM
        // Declara as funções da biblioteca C que serão usadas pelo programa
        headerBuilder.append("declare i32 @printf(i8*, ...)\n");
        headerBuilder.append("declare i32 @scanf(i8*, ...)\n\n");

        // FASE 2: Coleta de declarações de função
        // Percorre a AST para encontrar todas as declarações de função
        // e armazena em um cache para resolução posterior de chamadas
        for (AST child : root.getChildren()) {
            if (child.kind == NodeKind.FUNC_DECL_NODE) {
                String funcName = child.getChild(0).text;
                functionDeclarations.put(funcName, child);
            }
        }

        // FASE 3: Geração do código LLVM
        // Inicia a travessia da AST para gerar código LLVM IR
        visit(root);

        // FASE 4: Montagem do código final
        // Combina o cabeçalho (declarações globais + protótipos)
        // com o corpo (implementações das funções)
        return headerBuilder.toString() + irBuilder.toString();
    }

    // Esta é a função despachante principal do padrão Visitor no gerador de código LLVM. 
    // Ela funciona como um roteador que direciona cada nó da AST para o método específico 
    // responsável por gerar código LLVM para aquele tipo de nó.
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
            case PLUS_NODE:         return visitPlusNode(node);
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
            case STR_VAL_NODE:           return visitStrValNode(node);
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
                System.err.println("ERROR: Unsupported AST node type: " + node.kind + " (text: '" + node.text + "')");
                for (AST child : node.getChildren()) {
                    visit(child);
                }
                break;
        }
        return "";
    }

    // --- Métodos Auxiliares ---

    // Gera nomes únicos para registradores virtuais no LLVM IR.
    private String newReg() { return "%" + regCounter++; }

    // Gera nomes únicos para labels de bloco no LLVM IR.
    private String newLabel(String prefix) { return prefix + "." + labelCounter++; }
    
    // Cria constantes de string globais no LLVM IR e retorna um ponteiro para elas.
    private String createGlobalString(String value, String namePrefix) {
        // Cria nomes únicos como @.const.str.0, @.fmt.printf.1, etc.
        String strName = "@" + namePrefix + "." + strCounter++;
        
        // Usar byte count em UTF-8
        byte[] utf8Bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int len = utf8Bytes.length + 1; // +1 para null terminator
        
        // Converte caracteres especiais para formato LLVM
        String escapedValue = value.replace("\\", "\\5C").replace("\n", "\\0A").replace("\"", "\\22");
        
        headerBuilder.append(strName).append(" = private unnamed_addr constant [")
                     .append(len).append(" x i8] c\"").append(escapedValue).append("\\00\"\n");
        
        // Armazena o tamanho para uso posterior em getelementptr
        stringLengths.put(strName, len);
        
        return strName;
    }

    // Adiciona instruções LLVM IR ao corpo da função sendo gerada.
    private void emit(String instruction) { 
        irBuilder.append("\t").append(instruction).append("\n");
        // Check if this is a terminator instruction
        String trimmed = instruction.trim().toLowerCase();
        lastInstructionWasTerminator = trimmed.startsWith("ret ") || trimmed.startsWith("br ");
    }

    // Adiciona labels de bloco ao código LLVM IR.
    private void emitLabel(String label) { 
        irBuilder.append(label).append(":\n");
        lastInstructionWasTerminator = false; // Labels iniciam novos blocos, então lastInstructionWasTerminator = false
        currentBlock = label; // Atualiza o bloco atual
    }
    
    private String getCurrentBlock() {
        return currentBlock;
    }

    // Converte tipos Go para seus equivalentes em LLVM IR para uso geral (parâmetros, retornos, operações).
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

    // Converte tipos Go para tipos LLVM específicos para instruções alloca (alocação de memória).
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

    // Gera strings de formato para a função printf (saída formatada).
    private String getOrCreateFormatString(GoType type) {
        if (printfFormatStrings.containsKey(type)) { // Verifica se já existe uma string de formato em cache
            return printfFormatStrings.get(type);
        }
        String formatSpecifier;
        if (type == null) {
            // Caso especial para println() sem argumentos - apenas quebra de linha
            formatSpecifier = "\n";
        } else {
            switch (type) {
                case INT: case INT32: formatSpecifier = "%d\n"; break;
                case BOOL: formatSpecifier = "%d\n"; break; 
                case FLOAT64: formatSpecifier = "%f\n"; break;
                case STRING: formatSpecifier = "%s\n"; break;
                default: formatSpecifier = "Unsupported type\n";
            }
        }
        String formatStrName = createGlobalString(formatSpecifier, ".fmt.printf"); // Se não existir, cria uma nova usando createGlobalString
        printfFormatStrings.put(type, formatStrName);
        return formatStrName;
    }

    // Gera strings de formato para a função scanf (entrada formatada).
    private String getOrCreateScanfFormatString(GoType type) {
        if (scanfFormatStrings.containsKey(type)) { // Verifica se já existe uma string de formato em cache
            return scanfFormatStrings.get(type);
        }
        String formatSpecifier;
        switch (type) {
            case INT: case INT32:  case BOOL: formatSpecifier = "%d"; break;
            case FLOAT64: formatSpecifier = "%lf"; break;
            case STRING: formatSpecifier = "%s"; break;
            default: formatSpecifier = "";
        }
        String formatStrName = createGlobalString(formatSpecifier, ".fmt.scanf"); // Se não existir, cria uma nova usando createGlobalString
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

    // Método auxiliar para identificar tipos simples
    private static class GoTypeHelper {
        public static boolean isSimpleType(GoType type) {
            return type == GoType.INT || type == GoType.INT32 || 
                type == GoType.BOOL || type == GoType.FLOAT64;
        }
    }

    private String visitFuncDeclNode(AST node) {
        String funcName = node.getChild(0).text;
        AST paramListNode = node.getChild(1);
        AST resultNode = node.getChild(2);
        AST bodyNode = node.getChild(3);
        String returnType = getLLVMType(resultNode.type);
        
        regCounter = paramListNode.getChildCount();

        Map<GoType, String> typeCache = new HashMap<>();
        Map<GoType, String> allocTypeCache = new HashMap<>();
    
        // Pre-computar tipos para todos os parâmetros
        for (AST paramNode : paramListNode.getChildren()) {
            AST paramIdNode = paramNode.getChild(0);
            GoType paramType = paramIdNode.getAnnotatedType();
            typeCache.put(paramType, getLLVMType(paramType));
            allocTypeCache.put(paramType, getLLVMTypeForAlloc(paramType));
        }

        StringBuilder paramsBuilder = new StringBuilder();
        boolean first = true;
        for (AST paramNode : paramListNode.getChildren()) {
            if (!first) {
                paramsBuilder.append(", ");
            }
            AST paramIdNode = paramNode.getChild(0);
            paramsBuilder.append(getLLVMType(paramIdNode.getAnnotatedType()));
            first = false;
        }
        String paramsString = paramsBuilder.toString();

        irBuilder.append("\ndefine ").append(returnType).append(" @").append(funcName)
                 .append("(").append(paramsString).append(") {\n");
        
        symbolTable.push(new HashMap<>());
        emitLabel("entry");

        for (int i = 0; i < paramListNode.getChildCount(); i++) {
            AST paramNode = paramListNode.getChild(i);
            AST paramIdNode = paramNode.getChild(0);
            String paramName = paramIdNode.text;
            GoType paramType = paramIdNode.getAnnotatedType();

            String pointerName = newReg(); // Use simple register naming

            String llvmType = typeCache.get(paramType);
            String llvmAllocType = allocTypeCache.get(paramType);

            if (GoTypeHelper.isSimpleType(paramType)) {  // int, bool, float64
                // CORREÇÃO: Parâmetros precisam ser alocados para permitir modificação
                emit(pointerName + " = alloca " + llvmType);
                emit("store " + llvmType + " %" + i + ", " + llvmType + "* " + pointerName);
                symbolTable.peek().put(paramName, new SymbolTableEntry(false, pointerName, paramType));
            } else if (paramType.isArray()) {
                symbolTable.peek().put(paramName, new SymbolTableEntry(false, "%" + i, paramType));
            } else if (paramType == GoType.STRING) {
                emit(pointerName + " = alloca " + llvmType);  // i8* para strings
                emit("store " + llvmType + " %" + i + ", " + llvmType + "* " + pointerName);
                symbolTable.peek().put(paramName, new SymbolTableEntry(false, pointerName, paramType));
            } else {
                // Para outros tipos complexos, manter alocação
                emit(pointerName + " = alloca " + llvmAllocType);
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
        symbolTable.push(new HashMap<>(symbolTable.peek())); // Cria um novo escopo na tabela de símbolos fazendo push() de uma cópia do escopo atual
        for (AST child : node.getChildren()) {
            visit(child);
        }
        symbolTable.pop(); // Remove o escopo da pilha com pop()
        return ""; // Retorna string vazia pois blocos não produzem valores em LLVM
    }
    
    // --- Statements ---

    private String visitIfNode(AST node) {
        String condValue = visit(node.getChild(0));
        String thenLabel = newLabel("if.then");
        String elseLabel = newLabel("if.else");
        String endLabel = newLabel("if.end");
        boolean hasElse = node.getChildCount() > 2;
        
        boolean endLabelReferenced = false;

        if (hasElse) {
            emit("br i1 " + condValue + ", label %" + thenLabel + ", label %" + elseLabel);
        } else {
            emit("br i1 " + condValue + ", label %" + thenLabel + ", label %" + endLabel);
            endLabelReferenced = true; // O branch condicional referencia endLabel
        }

        // Process THEN branch
        emitLabel(thenLabel);
        visit(node.getChild(1));
        boolean thenHasTerminator = lastInstructionWasTerminator;
        if (!thenHasTerminator) {
            emit("br label %" + endLabel);
            endLabelReferenced = true;
        }

        // Process ELSE branch (if exists)
        boolean elseHasTerminator = false;
        if (hasElse) {
            emitLabel(elseLabel);
            visit(node.getChild(2));
            elseHasTerminator = lastInstructionWasTerminator;
            if (!elseHasTerminator) {
                emit("br label %" + endLabel);
                endLabelReferenced = true;
            }
        }

        // Only emit the end label if it was referenced
        if (endLabelReferenced) {
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
        if (!loopEndLabels.isEmpty()) { // Checa se a pilha não está vazia antes de acessar
            emit("br label %" + loopEndLabels.peek()); // Usa peek() para obter o label sem remover da pilha
        }
        return "";
    }

    private String visitContinueNode(AST node) {
        if (!loopPostLabels.isEmpty()) { // Checa se a pilha não está vazia antes de acessar
            emit("br label %" + loopPostLabels.peek()); // Usa peek() para obter o label sem remover da pilha
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
                    headerBuilder.append("0");
                } else {
                    headerBuilder.append("zeroinitializer");
                }
                headerBuilder.append("\n");
                
                // Add to global symbol table
                globalSymbols.put(varName, new SymbolTableEntry(false, globalName, varType));
            } else {
                // Local variable
                String pointerName = newReg(); // Use simple register naming
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
            String pointerName = newReg();

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
            
            // Verificar se é constante antes de permitir atribuição
            if (entry.isConstant) {
                System.err.println("ERROR: Cannot assign to constant: " + varName);
                return "";
            }
            
            // Validação de tipos entre lvalue e rvalue
            GoType rvalueType = rvalueNode.getAnnotatedType();
            if (rvalueType != null && !areTypesCompatible(entry.type, rvalueType)) {
                System.err.println("ERROR: Type mismatch in assignment to " + varName + 
                                ": expected " + entry.type + ", got " + rvalueType);
                return "";
            }

            // Tratamento especial para strings com implementação básica
            if (entry.type == GoType.STRING) {
                String value = visit(rvalueNode);
                String pointerName = entry.value;
                
                // Para strings, copiamos o ponteiro (implementação simplificada)
                // Em uma implementação completa, seria necessário usar memcpy ou strcpy
                emit("store i8* " + value + ", i8** " + pointerName);
            } else {
                String pointerName = entry.value;
                String llvmType = getLLVMType(entry.type);
                String value = visit(rvalueNode);
                emit("store " + llvmType + " " + value + ", " + llvmType + "* " + pointerName);
            }
        }
        return "";
    }

    // Método auxiliar para verificar compatibilidade de tipos
    private boolean areTypesCompatible(GoType targetType, GoType sourceType) {
        // Tipos idênticos são sempre compatíveis
        if (targetType == sourceType) {
            return true;
        }
        
        // Conversões numéricas permitidas
        if (targetType.isInteger() && sourceType.isInteger()) {
            return true; // int32 para int é ok
        }
        
        if (targetType.isFloat() && sourceType.isFloat()) {
            return true; // float64 para float32 seria ok, mas aqui só temos float64
        }
        
        // Conversões entre int e float
        if ((targetType.isInteger() && sourceType.isFloat()) ||
            (targetType.isFloat() && sourceType.isInteger())) {
            return true;
        }
        
        // Bool é estritamente tipado
        if (targetType == GoType.BOOL || sourceType == GoType.BOOL) {
            return targetType == sourceType;
        }
        
        // Strings são estritamente tipadas
        if (targetType == GoType.STRING || sourceType == GoType.STRING) {
            return targetType == sourceType;
        }
        
        // Arrays devem ter tipos compatíveis
        if (targetType.isArray() && sourceType.isArray()) {
            return areTypesCompatible(targetType.getElementType(), sourceType.getElementType());
        }
        
        return false;
    }

    private String visitReturnNode(AST node) {
        if (node.hasChildren()) {
            // Return com expressão: return x
            String returnType = getLLVMType(node.getChild(0).getAnnotatedType());
            String exprResult = visit(node.getChild(0));
            emit("ret " + returnType + " " + exprResult);
        } else {
            // Return vazio: return
            emit("ret void");
        }
        return "";
    }

    // --- Expressões ---

    private String visitCallNode(AST node) {
        String funcName = node.getChild(0).text;
        
        switch (funcName) {
            case "println": return handlePrintln(node);
            case "len": return handleLen(node);
            case "scanln": return handleScanln(node);
            default: return handleUserFunctionCall(node, funcName);
        }
    }

    // Método para tratar chamadas println
    private String handlePrintln(AST node) {
        if (node.getChildCount() == 1) {
            // println() sem argumentos - apenas quebra de linha
            String newlineFormat = getOrCreateFormatString(null); // Especial para quebra de linha
            int formatStrLen = stringLengths.get(newlineFormat);
            String formatStrPtr = newReg();
            emit(formatStrPtr + " = getelementptr inbounds [" + formatStrLen + " x i8], [" + formatStrLen + " x i8]* " + newlineFormat + ", i64 0, i64 0");
            String printfResult = newReg();
            emit(printfResult + " = call i32 (i8*, ...) @printf(i8* " + formatStrPtr + ")");
            return "";
        }

        // Construir formato dinâmico e lista de argumentos
        StringBuilder formatBuilder = new StringBuilder();
        List<String> argValues = new ArrayList<>();
        List<String> argTypes = new ArrayList<>();

        for (int i = 1; i < node.getChildCount(); i++) {
            AST argNode = node.getChild(i);
            GoType argType = argNode.getAnnotatedType();
            String argValue = visit(argNode);

            // Adicionar espaço entre argumentos (exceto o primeiro)
            if (i > 1) {
                formatBuilder.append(" ");
            }

            // Adicionar especificador de formato baseado no tipo
            switch (argType) {
                case INT: case INT32:
                    formatBuilder.append("%d");
                    argTypes.add("i32");
                    break;
                case BOOL:
                    formatBuilder.append("%d");
                    String extendedBool = newReg();
                    emit(extendedBool + " = zext i1 " + argValue + " to i32");
                    argValue = extendedBool;
                    argTypes.add("i32");
                    break;
                case FLOAT64:
                    formatBuilder.append("%f");
                    argTypes.add("double");
                    break;
                case STRING:
                    formatBuilder.append("%s");
                    argTypes.add("i8*");
                    break;
                default:
                    formatBuilder.append("%s"); // fallback
                    argTypes.add("i8*");
            }
            argValues.add(argValue);
        }
        
        // Adicionar quebra de linha no final
        formatBuilder.append("\n"); // Use literal newline, not escaped

        // Criar a string de formato global
        String formatStrName = createGlobalString(formatBuilder.toString(), ".fmt.println");
        int formatStrLen = stringLengths.get(formatStrName);

        String formatStrPtr = newReg();
        emit(formatStrPtr + " = getelementptr inbounds [" + formatStrLen + " x i8], [" + formatStrLen + " x i8]* " + formatStrName + ", i64 0, i64 0");

        // Construir a chamada printf com todos os argumentos
        StringBuilder printfCall = new StringBuilder();
        String printfResult = newReg();
        printfCall.append(printfResult + " = call i32 (i8*, ...) @printf(i8* ").append(formatStrPtr);
        
        for (int i = 0; i < argValues.size(); i++) {
            printfCall.append(", ").append(argTypes.get(i)).append(" ").append(argValues.get(i));
        }
        printfCall.append(")");

        emit(printfCall.toString());
        return "";
    }

    // Método para tratar chamadas len
    private String handleLen(AST node) {
        // For string length - simplified implementation, always return 0
        String resultReg = newReg();
        emit(resultReg + " = add i32 0, 0  ; len() placeholder");
        return resultReg;
    }

    // Método para tratar chamadas scanln
    private String handleScanln(AST node) {
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
            
            // Trata a chamada a scanf para cada tipo
            handleScanfByType(varType, varPtr, formatStrPtr, scanResultReg);

            // Atualiza o contador total
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

    // Método auxiliar para tratar scanf por tipo
    private void handleScanfByType(GoType varType, String varPtr, String formatStrPtr, String scanResultReg) {
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
    }

    // Método para tratar chamadas de função definida pelo usuário
    private String handleUserFunctionCall(AST node, String funcName) {
        List<String> argValues = new ArrayList<>();
        for (int i = 1; i < node.getChildCount(); i++) {
            argValues.add(visit(node.getChild(i)));
        }

        AST funcDecl = functionDeclarations.get(funcName);
        if (funcDecl == null) {
            System.err.println("ERROR: Function not declared: " + funcName);
            return "";
        }
        
        // Verificar número de argumentos
        AST paramListNode = funcDecl.getChild(1);
        int expectedArgs = paramListNode.getChildCount();
        int providedArgs = node.getChildCount() - 1;
        if (providedArgs != expectedArgs) {
            System.err.println("ERROR: Wrong number of arguments for " + funcName + 
                              ": expected " + expectedArgs + ", got " + providedArgs);
            return "";
        }
        
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
        if (node == null || node.getChildCount() == 0) {
            System.err.println("ERROR: Invalid type conversion node");
            return "";
        }
        
        GoType targetType = node.getAnnotatedType();
        AST exprNode = node.getChild(0);

        if (exprNode == null) {
            System.err.println("ERROR: Missing expression in type conversion");
            return "";
        }

        GoType sourceType = exprNode.getAnnotatedType();
            
        if (targetType == null || sourceType == null) {
            System.err.println("ERROR: Type information missing for conversion");
            return "";
        }

        if (targetType == sourceType) {
            return visit(exprNode); // Sem conversão necessária
        }
        
        String sourceValue = visit(exprNode);
        String destReg = newReg();

        if (sourceType.isInteger() && targetType.isFloat()) {
            emit(destReg + " = sitofp " + getLLVMType(sourceType) + " " + sourceValue + " to " + getLLVMType(targetType));
        } else if (sourceType.isFloat() && targetType.isInteger()) {
            emit(destReg + " = fptosi " + getLLVMType(sourceType) + " " + sourceValue + " to " + getLLVMType(targetType));
        } else if (sourceType == GoType.BOOL && targetType.isInteger()) {
            emit(destReg + " = zext i1 " + sourceValue + " to " + getLLVMType(targetType));
        } else if (sourceType.isInteger() && targetType == GoType.BOOL) {
            emit(destReg + " = icmp ne " + getLLVMType(sourceType) + " " + sourceValue + ", 0");
        } else {
            System.err.println("ERROR: Unsupported type conversion: " + sourceType + " to " + targetType);
            return sourceValue;
        }
        
        return destReg;
    }

    private String visitUnaryMinusNode(AST node) {
        if (node == null || node.getChildCount() == 0) {
            System.err.println("ERROR: Invalid unary minus node");
            return "";
        }

        GoType type = node.getAnnotatedType();
        AST childNode = node.getChild(0);
        
        if (childNode == null) {
            System.err.println("ERROR: Missing operand in unary minus");
            return "";
        }

        if (type == null) {
            System.err.println("ERROR: Type information missing for unary minus");
            return "";
        }

        if (!type.isInteger() && !type.isFloat()) {
            System.err.println("ERROR: Unary minus not supported for type: " + type);
            return visit(childNode); // Retorna valor original
        }

        String llvmType = getLLVMType(type);
        String value = visit(childNode);
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
        String entryLabel = getCurrentBlock(); // Use current block instead of creating new one
        String evalRhsLabel = newLabel("and.eval_rhs");
        String endLabel = newLabel("and.end");
        
        // No initial branch - we're already in the entry block
        String lhsValue = visit(node.getChild(0));
        emit("br i1 " + lhsValue + ", label %" + evalRhsLabel + ", label %" + endLabel);

        emitLabel(evalRhsLabel);
        String rhsValue = visit(node.getChild(1));
        String rhsBlock = getCurrentBlock(); // Get the actual block where RHS ends
        emit("br label %" + endLabel);

        emitLabel(endLabel);
        String destReg = newReg();
        emit(destReg + " = phi i1 [ 0, %" + entryLabel + " ], [ " + rhsValue + ", %" + rhsBlock + " ]");
        return destReg;
    }

    private String visitOrNode(AST node) {
        String entryLabel = getCurrentBlock(); // Use current block instead of creating new one
        String evalRhsLabel = newLabel("or.eval_rhs");
        String endLabel = newLabel("or.end");

        // No initial branch - we're already in the entry block
        String lhsValue = visit(node.getChild(0));
        emit("br i1 " + lhsValue + ", label %" + endLabel + ", label %" + evalRhsLabel);

        emitLabel(evalRhsLabel);
        String rhsValue = visit(node.getChild(1));
        String rhsBlock = getCurrentBlock(); // Get the actual block where RHS ends
        emit("br label %" + endLabel);

        emitLabel(endLabel);
        String destReg = newReg();
        emit(destReg + " = phi i1 [ 1, %" + entryLabel + " ], [ " + rhsValue + ", %" + rhsBlock + " ]");
        return destReg;
    }

    private String visitPlusNode(AST node) {
        GoType resultType = node.getAnnotatedType();
        
        // Check if this is string concatenation
        if (resultType == GoType.STRING) {
            // For now, implement a simple version that returns a fixed pointer
            // In a real implementation, you would need to call strcat or similar
            String left = visit(node.getChild(0));
            String right = visit(node.getChild(1));
            
            // Create a simple placeholder - in reality this would need proper string concatenation
            String destReg = newReg();
            emit(destReg + " = select i1 true, i8* " + left + ", i8* " + right + "  ; placeholder for string concat");
            return destReg;
        } else {
            // Regular arithmetic addition
            return visitBinaryOpNode(node, "add", null);
        }
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
                // Para strings, carregamos o ponteiro. Strings são armazenadas como i8**
                String destReg = newReg();
                emit(destReg + " = load i8*, i8** " + pointerName);
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
        GoType arrayType = node.getAnnotatedType();
        
        if (arrayType == null) {
            return "";
        }
        
        String arrayBaseType = getLLVMType(arrayType).replace("*", "");
        
        String arrayPtr = newReg();
        emit(arrayPtr + " = alloca " + arrayBaseType);
        
        // CORREÇÃO: Inicializar todo o array com zero primeiro
        emit("store " + arrayBaseType + " zeroinitializer, " + arrayBaseType + "* " + arrayPtr);
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AST elementNode = node.getChild(i);
            String elementValue = visit(elementNode);
            GoType elementType = arrayType.getElementType();
            String elementLLVMType = getLLVMType(elementType);
            
            String elementPtr = newReg();
            // CORREÇÃO: Usar arrayBaseType em vez de getLLVMType(arrayType)
            emit(elementPtr + " = getelementptr inbounds " + arrayBaseType + ", " + 
                 arrayBaseType + "* " + arrayPtr + ", i64 0, i32 " + i);
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
        // Use the stored length from createGlobalString
        int strLen = stringLengths.get(strName);
        
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
                int strLen = stringLengths.get(strName);
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
