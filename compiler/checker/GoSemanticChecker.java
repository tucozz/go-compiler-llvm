package compiler.checker;

import Go_Parser.Go_ParserBaseVisitor;
import Go_Parser.Go_Parser;
import compiler.tables.StrTable;
import compiler.tables.VarTable;
import compiler.tables.VarTable.VarEntry;
import compiler.tables.FunctionTable;
import compiler.tables.FunctionInfo;
import compiler.tables.ArrayTable;
import compiler.tables.ArrayInfo;
import compiler.typing.GoType;
import compiler.typing.TypeTable;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

public class GoSemanticChecker extends Go_ParserBaseVisitor<Void> {

    private VarTable varTable;
    private StrTable stringTable;
    private FunctionTable functionTable;
    private ArrayTable arrayTable;
    private TypeTable typeTable;
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
        this.allProcessedVariables = new ArrayList<>();
        this.currentFunctionReturnType = null;
        this.currentFunctionName = null;
        this.loopDepth = 0;
    }

    public boolean hasSemanticErrors() {
        return foundSemanticErrors;
    }

    public VarTable getVarTable() {
        return varTable;
    }

    public StrTable getStringTable() {
        return stringTable;
    }

    public FunctionTable getFunctionTable() {
        return functionTable;
    }

    public ArrayTable getArrayTable() {
        return arrayTable;
    }

    public TypeTable getTypeTable() {
        return typeTable;
    }

    /**
     * Reporta erro semântico (versão simplificada)
     */
    private void reportSemanticError(String message) {
        System.err.println("SEMANTIC ERROR (1): " + message);
        foundSemanticErrors = true;
    }

    /**
     * Reporta erro semântico usando contexto ANTLR (simplificado)
     */
    private void reportSemanticError(Object ctx, String message) {
        reportSemanticError(message);
    }

    // --- PROGRAMA PRINCIPAL ---

    @Override
    public Void visitProgramRule(Go_Parser.ProgramRuleContext ctx) {
        return super.visitProgramRule(ctx);
    }

    // --- DECLARAÇÕES ---

    @Override
    public Void visitConstDeclarationStmt(Go_Parser.ConstDeclarationStmtContext ctx) {
        return super.visitConstDeclarationStmt(ctx);
    }

    @Override
    public Void visitVarDeclarationStmt(Go_Parser.VarDeclarationStmtContext ctx) {
        return super.visitVarDeclarationStmt(ctx);
    }

    // --- DECLARAÇÕES ESPECÍFICAS ---

    @Override
    public Void visitConstDecl(Go_Parser.ConstDeclContext ctx) {
        return super.visitConstDecl(ctx);
    }

    @Override
    public Void visitVarDecl(Go_Parser.VarDeclContext ctx) {
        return super.visitVarDecl(ctx);
    }

    // --- ESPECIFICAÇÕES ---

    @Override
    public Void visitConstSpecification(Go_Parser.ConstSpecificationContext ctx) {
        // Extrair informações da constante diretamente do contexto
        List<String> constNames = extractIdentifierNames(ctx);
        String typeInfo = extractTypeInfo(ctx);

        int lineNumber = 1; // default
        try {
            if (ctx.identifierList() != null) {
                // Usar reflexão simplificada para pegar start token
                Object startToken = ctx.identifierList().getClass().getMethod("getStart").invoke(ctx.identifierList());
                if (startToken != null) {
                    lineNumber = (Integer) startToken.getClass().getMethod("getLine").invoke(startToken);
                }
            }
        } catch (Exception e) {
            lineNumber = 1;
        }

        // Processar cada constante
        for (String constName : constNames) {
            if (constName != null && !constName.isEmpty()) {
                GoType constType = GoType.fromString(typeInfo);
                
                // Tentar adicionar a constante à tabela
                if (!varTable.addConstant(constName, constType, lineNumber)) {
                    VarEntry existing = varTable.lookup(constName);
                    if (existing != null && varTable.existsInCurrentScope(constName)) {
                        reportSemanticError(ctx, 
                            "constant '" + constName + "' already declared at line " + existing.getDeclarationLine());
                    }
                } else {
                    // Adicionar à lista de variáveis processadas para o relatório
                    VarEntry constEntry = varTable.lookup(constName);
                    if (constEntry != null) {
                        allProcessedVariables.add(constEntry);
                    }
                    
                    // Adicionar também à tabela de tipos para referência
                    typeTable.addVariable(constName, constType);
                    
                    // Se for um array, adicionar à ArrayTable
                    processArrayDeclaration(constName, typeInfo, lineNumber);
                }
            }
        }
        
        return null;
    }

    @Override
    public Void visitVarSpecification(Go_Parser.VarSpecificationContext ctx) {
        // Extrair informações da variável diretamente do contexto
        List<String> varNames = extractIdentifierNames(ctx);
        String typeInfo = extractTypeInfo(ctx);
        
        int lineNumber = 1; // default
        try {
            if (ctx.identifierList() != null) {
                // Usar reflexão simplificada para pegar start token
                Object startToken = ctx.identifierList().getClass().getMethod("getStart").invoke(ctx.identifierList());
                if (startToken != null) {
                    lineNumber = (Integer) startToken.getClass().getMethod("getLine").invoke(startToken);
                }
            }
        } catch (Exception e) {
            lineNumber = 1; // fallback se falhar
        }
        
        // Processar cada variável
        for (String varName : varNames) {
            if (varName != null && !varName.isEmpty()) {
                GoType varType = GoType.fromString(typeInfo);
                
                // Tentar adicionar a variável à tabela
                boolean added = varTable.addVariable(varName, varType, lineNumber);
                
                if (!added) {
                    VarEntry existing = varTable.lookup(varName);
                    if (existing != null && varTable.existsInCurrentScope(varName)) {
                        reportSemanticError(
                            "variable '" + varName + "' already declared at line " + existing.getDeclarationLine());
                    }
                } else {
                    // Adicionar à lista de variáveis processadas para o relatório
                    VarEntry varEntry = varTable.lookup(varName);
                    if (varEntry != null) {
                        allProcessedVariables.add(varEntry);
                    }
                    
                    // Adicionar também à tabela de tipos para referência
                    typeTable.addVariable(varName, varType);
                    
                    // Se for um array, adicionar à ArrayTable
                    processArrayDeclaration(varName, typeInfo, lineNumber);
                }
            }
        }
        
        return null;
    }

    // --- DECLARAÇÕES CURTAS ---

    @Override
    public Void visitShortVariableDecl(Go_Parser.ShortVariableDeclContext ctx) {
        // Extrair nomes das variáveis usando contexto direto
        List<String> varNames = new ArrayList<>();
        if (ctx.identifierList() != null) {
            // Usar getTerminalText diretamente no contexto ANTLR
            String fullIdList = getTerminalText(ctx.identifierList());
            if (fullIdList != null && !fullIdList.isEmpty()) {
                // Parsing simples por vírgulas para múltiplas variáveis
                String[] ids = fullIdList.split(",");
                for (String id : ids) {
                    varNames.add(id.trim());
                }
            }
        }
        int lineNumber = 1; // default
        try {
            if (ctx.identifierList() != null) {
                // Usar reflexão simplificada para pegar start token
                Object startToken = ctx.identifierList().getClass().getMethod("getStart").invoke(ctx.identifierList());
                if (startToken != null) {
                    lineNumber = (Integer) startToken.getClass().getMethod("getLine").invoke(startToken);
                }
            }
        } catch (Exception e) {
            lineNumber = 1; // fallback se falhar
        }
        
        // Inferir tipo da expressão do lado direito
        List<String> exprTypes = new ArrayList<>();
        String ctxText = getTerminalText(ctx);
        
        if (ctxText.contains("[]int")) {
            exprTypes.add("[]int");
        } else if (ctxText.contains("[]string")) {
            exprTypes.add("[]string");
        } else if (ctxText.contains("[]bool")) {
            exprTypes.add("[]bool");
        } else {
            // Por enquanto, assumir tipo desconhecido para outras expressões
            exprTypes.add("unknown");
        }
        
        // Processar cada variável (assumindo correspondência 1:1)
        for (int i = 0; i < varNames.size(); i++) {
            String varName = varNames.get(i);
            String typeInfo = i < exprTypes.size() ? exprTypes.get(i) : "unknown";
            
            if (varName != null && !varName.isEmpty()) {
                GoType varType = GoType.fromString(typeInfo);
                
                // Tentar adicionar a variável à tabela
                boolean added = varTable.addVariable(varName, varType, lineNumber);
                
                if (!added) {
                    VarEntry existing = varTable.lookup(varName);
                    if (existing != null && varTable.existsInCurrentScope(varName)) {
                        reportSemanticError(
                            "variable '" + varName + "' already declared at line " + existing.getDeclarationLine());
                    }
                } else {
                    // Adicionar à lista de variáveis processadas para o relatório
                    VarEntry varEntry = varTable.lookup(varName);
                    if (varEntry != null) {
                        allProcessedVariables.add(varEntry);
                    }
                    
                    // Adicionar também à tabela de tipos para referência
                    typeTable.addVariable(varName, varType);
                    
                    // Se for um array, adicionar à ArrayTable
                    processArrayDeclaration(varName, typeInfo, lineNumber);
                }
            }
        }
        
        return null;
    }

    /**
     * Extrai nomes de identificadores usando contexto direto
     */
    private List<String> extractIdentifierNames(Object ctx) {
        List<String> names = new ArrayList<>();
        
        if (ctx instanceof Go_Parser.ConstSpecificationContext) {
            Go_Parser.ConstSpecificationContext constCtx = (Go_Parser.ConstSpecificationContext) ctx;
            if (constCtx.identifierList() != null) {
                String idListText = getTerminalText(constCtx.identifierList());
                if (idListText != null && !idListText.isEmpty()) {
                    String[] ids = idListText.split(",");
                    for (String id : ids) {
                        names.add(id.trim());
                    }
                }
            }
        } else if (ctx instanceof Go_Parser.VarSpecificationContext) {
            Go_Parser.VarSpecificationContext varCtx = (Go_Parser.VarSpecificationContext) ctx;
            if (varCtx.identifierList() != null) {
                String idListText = getTerminalText(varCtx.identifierList());
                if (idListText != null && !idListText.isEmpty()) {
                    String[] ids = idListText.split(",");
                    for (String id : ids) {
                        names.add(id.trim());
                    }
                }
            }
        }
        
        return names;
    }

    /**
     * Extrai informação de tipo usando contexto direto
     */
    private String extractTypeInfo(Object ctx) {
        if (ctx instanceof Go_Parser.ConstSpecificationContext) {
            Go_Parser.ConstSpecificationContext constCtx = (Go_Parser.ConstSpecificationContext) ctx;
            if (constCtx.typeSpec() != null) {
                return getTypeText(constCtx.typeSpec());
            }
        } else if (ctx instanceof Go_Parser.VarSpecificationContext) {
            Go_Parser.VarSpecificationContext varCtx = (Go_Parser.VarSpecificationContext) ctx;
            if (varCtx.typeSpec() != null) {
                return getTypeText(varCtx.typeSpec());
            }
        }
        
        return "unknown";
    }

    /**
     * Função auxiliar para extrair texto de TerminalNode usando reflexão mínima
     */
    private String getTerminalText(Object terminalNode) {
        if (terminalNode == null) return "unknown";
        
        try {
            Method getTextMethod = terminalNode.getClass().getMethod("getText");
            return (String) getTextMethod.invoke(terminalNode);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Processa declaração de array e adiciona à ArrayTable
     */
    private void processArrayDeclaration(String varName, String typeInfo, int lineNumber) {
        if (ArrayTable.isArrayType(typeInfo)) {
            ArrayInfo arrayInfo = ArrayTable.parseArrayType(typeInfo);
            if (arrayInfo != null) {
                arrayTable.addArray(varName, arrayInfo.getElementType(), arrayInfo.getSize(), lineNumber);
            }
        }
    }

    /**
     * Extrai parâmetros de função usando contexto ANTLR direto
     * Baseado no código de referência GoSemanticAnalyzer
     */
    private java.util.List<String> extractParameterNames(Go_Parser.FunctionDeclContext ctx) {
        java.util.List<String> parameters = new java.util.ArrayList<>();
        
        // Acessar signature -> parameters -> parameterList -> parameter
        if (ctx.signature() != null) {
            Go_Parser.FunctionSignatureContext funcSig = (Go_Parser.FunctionSignatureContext) ctx.signature();
            
            if (funcSig.parameters() != null) {
                Go_Parser.ParametersDeclarationContext paramsCtx = (Go_Parser.ParametersDeclarationContext) funcSig.parameters();
                
                if (paramsCtx.parameterList() != null) {
                    Go_Parser.ParamListContext paramListCtx = (Go_Parser.ParamListContext) paramsCtx.parameterList();
                    
                    for (Go_Parser.ParameterContext paramCtx : paramListCtx.parameter()) {
                        Go_Parser.ParameterDeclarationContext paramDeclCtx = (Go_Parser.ParameterDeclarationContext) paramCtx;
                        String paramName = getTerminalText(paramDeclCtx.ID());
                        parameters.add(paramName);
                    }
                }
            }
        }
        
        return parameters;
    }
    
    /**
     * Extrai nome do parâmetro de uma string como "nint" -> "n" ou "arr[]int" -> "arr"
     */
    private String extractNameFromParam(String param) {
        if (param == null || param.isEmpty()) return "";
        
        // Casos comuns de tipos Go (verificar []types primeiro)
        String[] goTypes = {"[]int", "[]string", "[]bool", "int", "string", "bool", "float64"};
        
        for (String type : goTypes) {
            if (param.endsWith(type)) {
                String name = param.substring(0, param.length() - type.length());
                // Remover [] extras se presente no nome
                if (name.endsWith("[]")) {
                    name = name.substring(0, name.length() - 2);
                }
                if (!name.isEmpty()) {
                    return name;
                }
            }
        }
        
        // Se não conseguiu extrair pelo sufixo, tentar por análise de caracteres
        // Procurar primeira sequência de letras minúsculas como nome
        for (int i = 1; i < param.length(); i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c) || c == '[' || Character.isDigit(c)) {
                return param.substring(0, i);
            }
        }
        
        // Fallback: usar a string inteira
        return param;
    }

    /**
     * Extrai tipos de parâmetros usando contexto ANTLR direto
     */
    private java.util.List<String> extractParameterTypes(Go_Parser.FunctionDeclContext ctx) {
        java.util.List<String> types = new java.util.ArrayList<>();
        
        // Mesma estrutura que extractParameterNames, mas pega os tipos
        if (ctx.signature() != null) {
            Go_Parser.FunctionSignatureContext funcSig = (Go_Parser.FunctionSignatureContext) ctx.signature();
            
            if (funcSig.parameters() != null) {
                Go_Parser.ParametersDeclarationContext paramsCtx = (Go_Parser.ParametersDeclarationContext) funcSig.parameters();
                
                if (paramsCtx.parameterList() != null) {
                    Go_Parser.ParamListContext paramListCtx = (Go_Parser.ParamListContext) paramsCtx.parameterList();
                    
                    for (Go_Parser.ParameterContext paramCtx : paramListCtx.parameter()) {
                        Go_Parser.ParameterDeclarationContext paramDeclCtx = (Go_Parser.ParameterDeclarationContext) paramCtx;
                        String paramType = getTerminalText(paramDeclCtx.typeSpec());
                        types.add(paramType);
                    }
                }
            }
        }
        
        return types;
    }
    
    /**
     * Extrai tipo do parâmetro de uma string como "nint" -> "int" ou "arr[]int" -> "[]int"
     */
    private String extractTypeFromParam(String param) {
        if (param == null || param.isEmpty()) return "unknown";
        
        // Casos comuns de tipos Go
        String[] goTypes = {"[]int", "[]string", "[]bool", "int", "string", "bool", "float64"};
        
        for (String type : goTypes) {
            if (param.endsWith(type)) {
                return type;
            }
        }
        
        // Se não conseguiu extrair pelo sufixo, retornar unknown
        return "unknown";
    }

    /**
     * Converte string de tipo para GoType
     */
    private GoType convertStringToGoType(String typeName) {
        if (typeName == null || typeName.equals("unknown")) {
            return GoType.UNKNOWN;
        }
        
        // Usar o método fromString do GoType
        GoType type = GoType.fromString(typeName);
        return type != null ? type : GoType.UNKNOWN;
    }

    /**
     * Extrai tipo de retorno de função diretamente do contexto ANTLR
     */
    private String extractReturnType(Go_Parser.FunctionDeclContext ctx) {
        if (ctx == null) return "void";
        
        if (ctx.signature() != null && ctx.signature() instanceof Go_Parser.FunctionSignatureContext) {
            Go_Parser.FunctionSignatureContext funcSig = (Go_Parser.FunctionSignatureContext) ctx.signature();
            
            if (funcSig.result() != null) {
                Go_Parser.ResultContext result = funcSig.result();
                
                if (result instanceof Go_Parser.ResultSingleTypeContext) {
                    Go_Parser.ResultSingleTypeContext singleType = (Go_Parser.ResultSingleTypeContext) result;
                    if (singleType.typeSpec() != null) {
                        String returnType = getTypeText(singleType.typeSpec());
                        return returnType;
                    }
                }
            }
        }
        
        // Se não tem result explícito, é void
        return "void";
    }

    /**
     * Extrai o texto de um contexto de tipo usando reflexão como fallback
     * para manter compatibilidade
     */
    private String getTypeText(Go_Parser.TypeSpecContext typeSpec) {
        if (typeSpec == null) return "unknown";
        
        // Para manter funcionando enquanto migra, usar reflexão como fallback
        try {
            Method getTextMethod = typeSpec.getClass().getMethod("getText");
            return (String) getTextMethod.invoke(typeSpec);
        } catch (Exception e) {
            return "unknown";
        }
    }

    // --- FUNÇÕES ---

    @Override
    public Void visitFunctionDecl(Go_Parser.FunctionDeclContext ctx) {
        String functionName = getTerminalText(ctx.ID());

        // Extrair parâmetros da função
        java.util.List<String> paramNames = extractParameterNames(ctx);
        java.util.List<String> paramTypeNames = extractParameterTypes(ctx);
        
        // Extrair tipo de retorno da função
        String returnTypeName = extractReturnType(ctx);
        
        // Verificar se a função já foi declarada
        if (functionTable.hasFunction(functionName)) {
            reportSemanticError(
                "function '" + functionName + "' already declared");
            return null;
        }
        
        // Criar novo escopo para a função
        varTable.enterScope();
        
        // Converter nomes de tipos para GoType
        List<String> paramNamesJava = new ArrayList<>();
        List<GoType> paramTypes = new ArrayList<>();
        GoType returnType = convertStringToGoType(returnTypeName);
        
        // Rastrear função atual para validação de returns
        currentFunctionName = functionName;
        currentFunctionReturnType = returnType;
        
        for (String paramName : paramNames) {
            paramNamesJava.add(paramName);
        }
        
        for (String typeName : paramTypeNames) {
            GoType goType = convertStringToGoType(typeName);
            paramTypes.add(goType);
        }
        
        // Adicionar parâmetros como variáveis locais no escopo da função
        int functionLineNumber = 1; // Default fallback for function declarations
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String typeName = i < paramTypeNames.size() ? paramTypeNames.get(i) : "unknown";
            GoType paramType = convertStringToGoType(typeName);
            
            if (!varTable.addVariable(paramName, paramType, functionLineNumber)) {
                reportSemanticError("parameter '" + paramName + "' already declared");
            } else {
                // Adicionar parâmetros à lista de variáveis processadas para o relatório
                VarEntry paramEntry = varTable.lookup(paramName);
                if (paramEntry != null) {
                    allProcessedVariables.add(paramEntry);
                }
                
                // Se for um array, adicionar também à ArrayTable
                if (ArrayTable.isArrayType(typeName)) {
                    ArrayInfo arrayInfo = ArrayTable.parseArrayType(typeName);
                    if (arrayInfo != null) {
                        arrayTable.addArray(paramName, arrayInfo.getElementType(), arrayInfo.getSize(), functionLineNumber);
                    }
                }
            }
        }
        
        // Adicionar função à tabela com tipo de retorno correto
        if (!functionTable.addFunction(functionName, paramNamesJava, paramTypes, returnType, 0)) {
            reportSemanticError("Failed to add function '" + functionName + "'");
        } else {
            // Marcar como definida (já que tem corpo)
            functionTable.markAsDefined(functionName);
        }
        
        // Processar corpo da função (delegar aos visitadores padrão)
        super.visitFunctionDecl(ctx);
        
        // Sair do escopo da função
        varTable.exitScope();
        
        // Limpar rastreamento da função atual
        currentFunctionName = null;
        currentFunctionReturnType = null;
        
        return null;
    }

    @Override 
    public Void visitBlockCode(Go_Parser.BlockCodeContext ctx) {
        // Entrar em novo escopo para blocos
        varTable.enterScope();
        
        // Processar conteúdo do bloco
        super.visitBlockCode(ctx);
        
        // Sair do escopo
        varTable.exitScope();
        
        return null;
    }

    // --- LITERAIS ---

    @Override
    public Void visitStringLiteral(Go_Parser.StringLiteralContext ctx) {
        // Processar literais de string
        return super.visitStringLiteral(ctx);
    }

    @Override
    public Void visitIdExpr(Go_Parser.IdExprContext ctx) {
        return super.visitIdExpr(ctx);
    }

    /**
     * Processa chamadas de função
     */
    @Override
    public Void visitCallExpression(Go_Parser.CallExpressionContext ctx) {
        String functionName = getTerminalText(ctx.ID());
        
        // Se a função não existe, verificar se é built-in e adicioná-la
        if (!functionTable.hasFunction(functionName)) {
            if (isBuiltInFunction(functionName)) {
                functionTable.addBuiltInFunctionIfNeeded(functionName);
            } else {
                reportSemanticError(ctx, "undefined function '" + functionName + "'");
            }
        }
        
        // Se agora a função existe, validar argumentos
        if (functionTable.hasFunction(functionName)) {
            // Extrair argumentos da chamada
            java.util.List<String> arguments = extractCallArguments(ctx);
            
            // Validar número de argumentos
            FunctionInfo funcInfo = functionTable.getFunction(functionName);
            if (funcInfo != null) {
                List<GoType> expectedParamTypes = funcInfo.getParameterTypes();
                
                if (arguments.size() != expectedParamTypes.size()) {
                    reportSemanticError(ctx, 
                        "function '" + functionName + "' expects " + expectedParamTypes.size() + 
                        " arguments, got " + arguments.size());
                } else {
                    
                    // Validar tipos dos argumentos (implementação básica)
                    for (int i = 0; i < arguments.size(); i++) {
                        String arg = arguments.get(i);
                        GoType expectedType = expectedParamTypes.get(i);
                        GoType argType = inferArgumentType(arg);
                        
                        if (!areTypesCompatible(argType, expectedType)) {
                            reportSemanticError(ctx, 
                                "argument " + (i + 1) + " to '" + functionName + 
                                "': cannot convert " + argType.getTypeName() + 
                                " to " + expectedType.getTypeName());
                        }
                    }
                }
            }
        }
        
        // Continuar processamento dos argumentos
        super.visitCallExpression(ctx);
        return null;
    }

    /**
     * Verifica se uma função é built-in do Go
     */
    private boolean isBuiltInFunction(String functionName) {
        return "println".equals(functionName) || "len".equals(functionName);
    }

    /**
     * Extrai argumentos de uma chamada de função usando parsing simples
     */
    private java.util.List<String> extractCallArguments(Go_Parser.CallExpressionContext ctx) {
        java.util.List<String> arguments = new java.util.ArrayList<>();
        
        if (ctx != null && ctx.expressionList() != null) {
            String argsText = getTerminalText(ctx.expressionList());
            if (argsText != null && !argsText.isEmpty()) {
                // Parsing simples por vírgulas
                String[] args = argsText.split(",");
                for (String arg : args) {
                    arguments.add(arg.trim());
                }
            }
        }
        
        return arguments;
    }

    /**
     * Infere o tipo de um argumento baseado em sua representação textual
     */
    private GoType inferArgumentType(String arg) {
        if (arg == null || arg.trim().isEmpty()) {
            return GoType.UNKNOWN;
        }
        
        // Remover espaços
        arg = arg.trim();
        
        // Usar TypeTable para inferir tipo de literais
        GoType literalType = typeTable.inferLiteralType(arg);
        if (literalType != GoType.UNKNOWN) {
            return literalType;
        }
        
        // Se não é literal, verificar se é uma variável conhecida
        if (varTable.exists(arg)) {
            VarEntry varEntry = varTable.lookup(arg);
            if (varEntry != null) {
                return varEntry.getType();
            }
        }
        
        // Tentar analisar como expressão aritmética simples
        GoType exprType = analyzeSimpleExpression(arg);
        if (exprType != GoType.UNKNOWN) {
            return exprType;
        }
        
        // Se não encontrou a variável, reportar erro
        return GoType.UNKNOWN;
    }

    /**
     * Analisa expressões aritméticas simples (e.g., x+y, a-b)
     * Focado apenas nos elementos mínimos do projeto
     */
    private GoType analyzeSimpleExpression(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return GoType.UNKNOWN;
        }
        
        expr = expr.trim();
        
        // Operações básicas requeridas pelo projeto (conforme README)
        String[] arithmeticOps = {"+", "-", "*", "/"};
        String[] comparisonOps = {"==", "!=", "<", ">", "<=", ">="};
        
        // Primeiro, tentar operações aritméticas
        for (String op : arithmeticOps) {
            int opIndex = expr.indexOf(op);
            if (opIndex > 0 && opIndex < expr.length() - op.length()) {
                String left = expr.substring(0, opIndex).trim();
                String right = expr.substring(opIndex + op.length()).trim();
                
                
                GoType leftType = inferSimpleOperand(left);
                GoType rightType = inferSimpleOperand(right);
                
                // Para operações aritméticas, usar TypeTable
                GoType resultType = typeTable.getBinaryOperationResultType(leftType, rightType, op);
                
                return resultType;
            }
        }
        
        // Depois, tentar operações de comparação
        for (String op : comparisonOps) {
            int opIndex = expr.indexOf(op);
            if (opIndex > 0 && opIndex < expr.length() - op.length()) {
                String left = expr.substring(0, opIndex).trim();
                String right = expr.substring(opIndex + op.length()).trim();
                
                
                // Comparações sempre retornam bool (conforme especificação básica)
                return GoType.BOOL;
            }
        }
        
        return GoType.UNKNOWN;
    }

    /**
     * Infere tipo de operando simples (literal ou variável)
     * Evita recursão infinita ao analisar expressões
     */
    private GoType inferSimpleOperand(String operand) {
        if (operand == null || operand.trim().isEmpty()) {
            return GoType.UNKNOWN;
        }
        
        operand = operand.trim();
        
        // Primeiro, verificar se é literal
        GoType literalType = typeTable.inferLiteralType(operand);
        if (literalType != GoType.UNKNOWN) {
            return literalType;
        }
        
        // Depois, verificar se é variável
        if (varTable.exists(operand)) {
            VarEntry varEntry = varTable.lookup(operand);
            if (varEntry != null) {
                return varEntry.getType();
            }
        }
        
        return GoType.UNKNOWN;
    }

    /**
     * Verifica se dois tipos são compatíveis
     * Utiliza a lógica de compatibilidade implementada na classe GoType
     */
    private boolean areTypesCompatible(GoType actual, GoType expected) {
        if (actual == null || expected == null) {
            return false;
        }
        // Delegar para a lógica de compatibilidade da classe GoType
        return actual.isCompatibleWith(expected);
    }

    /**
     * Processa statements de return
     */
    @Override
    public Void visitReturnStatementWithExpr(Go_Parser.ReturnStatementWithExprContext ctx) {
        if (currentFunctionName == null || currentFunctionReturnType == null) {
            reportSemanticError("return statement outside of function");
            return null;
        }
        
        // Extrair expressão de retorno usando reflexão
        String returnExpr = extractReturnExpression(ctx);
        
        if (returnExpr == null || returnExpr.trim().isEmpty()) {
            // Return sem expressão
            if (currentFunctionReturnType != GoType.VOID) {
                reportSemanticError(
                    "function '" + currentFunctionName + "' expects return type " + 
                    currentFunctionReturnType.getTypeName() + ", but got void");
            }
        } else {
            // Return com expressão - verificar tipo
            GoType returnType = inferArgumentType(returnExpr.trim());
            
            if (!areTypesCompatible(returnType, currentFunctionReturnType)) {
                reportSemanticError(
                    "function '" + currentFunctionName + "' expects return type " + 
                    currentFunctionReturnType.getTypeName() + ", but got " + 
                    returnType.getTypeName());
            }
        }
        
        // Continuar processamento
        super.visitReturnStatementWithExpr(ctx);
        return null;
    }

    /**
     * Extrai expressão de return usando contexto direto
     */
    private String extractReturnExpression(Go_Parser.ReturnStatementWithExprContext ctx) {
        if (ctx != null && ctx.expr() != null) {
            return getTerminalText(ctx.expr());
        }
        return null; // Return sem expressão
    }

    // --- STATEMENTS DE CONTROLE ---

    /**
     * Processa assignments simples (x = y)
     */
    @Override
    public Void visitSimpleAssignStatement(Go_Parser.SimpleAssignStatementContext ctx) {
        if (ctx.lvalue() == null || ctx.expr() == null) {
            return super.visitSimpleAssignStatement(ctx);
        }
        
        // Extrair informações usando getText() e parsing simples
        String fullLvalue = getTerminalText(ctx.lvalue());
        String rvalue = getTerminalText(ctx.expr());
        String varName = null;
        boolean isArrayAccess = false;
        String indexExpr = null;
        
        // Verificar se é acesso a array (formato: var[index])
        if (fullLvalue.contains("[") && fullLvalue.contains("]")) {
            int bracketIndex = fullLvalue.indexOf("[");
            int closeBracketIndex = fullLvalue.indexOf("]");
            varName = fullLvalue.substring(0, bracketIndex);
            indexExpr = fullLvalue.substring(bracketIndex + 1, closeBracketIndex);
            isArrayAccess = true;
        } else {
            varName = fullLvalue;
        }
        
        if (varName == null || varName.trim().isEmpty()) {
            return super.visitSimpleAssignStatement(ctx);
        }
        
        // Verificar se a variável existe na VarTable
        if (varTable.exists(varName)) {
            VarEntry varEntry = varTable.lookup(varName);
            if (varEntry != null) {
                // Verificar se é uma constante
                if (varEntry.isConstant()) {
                    reportSemanticError(ctx, "cannot assign to constant '" + varName + "'");
                    return super.visitSimpleAssignStatement(ctx);
                }
                
                GoType lvalueType = varEntry.getType();
                
                // Se é acesso a array, validar e obter tipo do elemento
                if (isArrayAccess) {
                    if (arrayTable.hasArray(varName)) {
                        ArrayInfo arrayInfo = arrayTable.getArray(varName);
                        lvalueType = arrayInfo.getElementType();
                    } else if (lvalueType.isArray()) {
                        lvalueType = lvalueType.getElementType();
                    } else {
                        reportSemanticError(ctx, "'" + varName + "' is not an array");
                        return super.visitSimpleAssignStatement(ctx);
                    }
                    
                    // Validar tipo do índice
                    if (indexExpr != null) {
                        GoType indexType = inferArgumentType(indexExpr);
                        if (indexType != GoType.INT && indexType != GoType.UNKNOWN) {
                            reportSemanticError(ctx, "array index must be integer, got " + indexType.getTypeName());
                        }
                    }
                }
                
                // Verificar compatibilidade de tipos
                GoType rvalueType = inferArgumentType(rvalue);
                if (!areTypesCompatible(rvalueType, lvalueType)) {
                    reportSemanticError(ctx, 
                        "cannot assign " + rvalueType.getTypeName() + 
                        " to variable '" + varName + "' of type " + lvalueType.getTypeName());
                }
            }
        }
        // Verificar se é array puro na ArrayTable
        else if (arrayTable.hasArray(varName)) {
            if (!isArrayAccess) {
                reportSemanticError(ctx, "cannot assign to array '" + varName + "' without index");
                return super.visitSimpleAssignStatement(ctx);
            }
            
            ArrayInfo arrayInfo = arrayTable.getArray(varName);
            GoType elementType = arrayInfo.getElementType();
            
            // Validar tipo do índice
            if (indexExpr != null) {
                GoType indexType = inferArgumentType(indexExpr);
                if (indexType != GoType.INT && indexType != GoType.UNKNOWN) {
                    reportSemanticError(ctx, "array index must be integer, got " + indexType.getTypeName());
                }
            }
            
            // Verificar compatibilidade de tipos
            GoType rvalueType = inferArgumentType(rvalue);
            if (!areTypesCompatible(rvalueType, elementType)) {
                reportSemanticError(ctx, 
                    "cannot assign " + rvalueType.getTypeName() + 
                    " to array element of type " + elementType.getTypeName());
            }
        }
        // Variável não encontrada
        else {
            reportSemanticError(ctx, "undefined variable '" + varName + "'");
        }
        
        return super.visitSimpleAssignStatement(ctx);
    }

    /**
     * Processa statements if-else
     */
    @Override
    public Void visitIfElseStatement(Go_Parser.IfElseStatementContext ctx) {
        
        // Extrair e validar condição do if
        String condition = null;
        if (ctx.expr() != null) {
            condition = getTerminalText(ctx.expr());
        }
        
        if (condition != null && !condition.trim().isEmpty()) {
            GoType conditionType = inferArgumentType(condition);
            
            // Em Go, condições devem ser do tipo bool
            if (conditionType != GoType.BOOL && conditionType != GoType.UNKNOWN) {
                reportSemanticError(ctx, 
                    "if condition must be boolean, got " + conditionType.getTypeName());
            }
        }
        
        // Continuar processamento automático dos blocos
        super.visitIfElseStatement(ctx);
        
        return null;
    }

    /**
     * Processa statements for (loops)
     */
    @Override
    public Void visitForLoopStatement(Go_Parser.ForLoopStatementContext ctx) {
        
        // Entrar no loop (incrementar profundidade)
        loopDepth++;
        
        // Verificar se tem condição de loop
        String condition = null;
        if (ctx.expr() != null) {
            condition = getTerminalText(ctx.expr());
        }
        
        if (condition != null && !condition.trim().isEmpty()) {
            GoType conditionType = inferArgumentType(condition);
            
            // Em Go, condições de loop devem ser do tipo bool
            if (conditionType != GoType.BOOL && conditionType != GoType.UNKNOWN) {
                reportSemanticError(ctx, 
                    "for condition must be boolean, got " + conditionType.getTypeName());
            }
        }
        
        // Continuar processamento automático
        super.visitForLoopStatement(ctx);
        
        loopDepth--;
        return null;
    }

    /**
     * Processa acesso a arrays (arr[index])
     */
    @Override
    public Void visitArrayAccessExpr(Go_Parser.ArrayAccessExprContext ctx) {
        // Extrair nome do array e índice usando reflexão
        String arrayName = extractArrayName(ctx);
        String indexExpr = extractArrayIndex(ctx);
        
        // Verificar se o array existe
        if (arrayName != null && !arrayName.trim().isEmpty()) {
            // Primeiro verificar na ArrayTable
            if (arrayTable.hasArray(arrayName)) {
                ArrayInfo arrayInfo = arrayTable.getArray(arrayName);
                
                // Verificar se o índice é do tipo int
                if (indexExpr != null && !indexExpr.trim().isEmpty()) {
                    GoType indexType = inferArgumentType(indexExpr);
                    if (indexType != GoType.INT && indexType != GoType.UNKNOWN) {
                        reportSemanticError(
                            "array index must be integer, got " + indexType.getTypeName());
                    }
                }
            } 
            // Fallback: verificar na VarTable para arrays declarados como variáveis
            else if (varTable.exists(arrayName)) {
                VarEntry arrayEntry = varTable.lookup(arrayName);
                if (arrayEntry != null) {
                    GoType arrayType = arrayEntry.getType();
                    
                    // Verificar se é realmente um array
                    if (!arrayType.isArray()) {
                        reportSemanticError(
                            "'" + arrayName + "' is not an array (type: " + arrayType.getTypeName() + ")");
                    } else {
                        
                        // Verificar se o índice é do tipo int
                        if (indexExpr != null && !indexExpr.trim().isEmpty()) {
                            GoType indexType = inferArgumentType(indexExpr);
                            if (indexType != GoType.INT && indexType != GoType.UNKNOWN) {
                                reportSemanticError(
                                    "array index must be integer, got " + indexType.getTypeName());
                            }
                        }
                    }
                }
            }
            // Array não encontrado em nenhuma tabela
            else {
                reportSemanticError("undefined array '" + arrayName + "'");
            }
        }
        
        // Continuar processamento
        super.visitArrayAccessExpr(ctx);
        return null;
    }

    /**
     * Extrai nome do array usando reflexão
     */
    private String extractArrayName(Go_Parser.ArrayAccessExprContext ctx) {
        if (ctx != null && ctx.expr().size() >= 1) {
            // O primeiro expr() é o nome do array
            return getTerminalText(ctx.expr(0));
        }
        return "unknown";
    }

    /**
     * Extrai índice do array usando reflexão
     */
    private String extractArrayIndex(Go_Parser.ArrayAccessExprContext ctx) {
        if (ctx != null && ctx.expr().size() >= 2) {
            // O segundo expr() é o índice (primeiro é o array)
            return getTerminalText(ctx.expr(1));
        }
        return "unknown";
    }

    /**
     * Processa statements break
     */
    @Override
    public Void visitBreakStatementRule(Go_Parser.BreakStatementRuleContext ctx) {
        // Verificar se estamos dentro de um loop
        if (loopDepth == 0) {
            reportSemanticError("break statement not in loop");
        }
        
        return null;
    }

    /**
     * Processa statements continue  
     */
    @Override
    public Void visitContinueStatementRule(Go_Parser.ContinueStatementRuleContext ctx) {
        // Verificar se estamos dentro de um loop
        if (loopDepth == 0) {
            reportSemanticError("continue statement not in loop");
        }
        
        return null;
    }

    // --- EXPRESSÕES PARENTIZADAS ---

    @Override
    public Void visitParenthesizedExpr(Go_Parser.ParenthesizedExprContext ctx) {
        super.visitParenthesizedExpr(ctx);
        return null;
    }

    // --- STATEMENTS SIMPLES ---

    @Override
    public Void visitExpressionSimpleStmt(Go_Parser.ExpressionSimpleStmtContext ctx) {
        super.visitExpressionSimpleStmt(ctx);
        return null;
    }

    /**
     * Imprime um relatório da análise semântica
     */
    public void printReport() {
        System.out.println("\n=== RELATÓRIO DA ANÁLISE SEMÂNTICA ===");
        
        // Relatório de erros
        if (foundSemanticErrors) {
            System.out.println("❌ Erros semânticos encontrados!");
        } else {
            System.out.println("✅ Nenhum erro semântico encontrado.");
        }
        
        // Tabela de Variáveis
        System.out.println("\n=== VARIABLE TABLE ===");
        if (allProcessedVariables.isEmpty()) {
            System.out.println("No variables declared.");
        } else {
            System.out.println("Variables declared:");
            for (VarEntry entry : allProcessedVariables) {
                System.out.println("  " + entry.toString());
            }
            System.out.println("Total variables: " + allProcessedVariables.size());
        }
        
        // Tabela de Funções
        functionTable.printTable();
        
        // Tabela de Arrays
        arrayTable.printTable();
        
        // Tabela de Strings
        System.out.println("\n=== STRING TABLE ===");
        System.out.println("String literals processed by compiler.");
        stringTable.printTable();
        
        // Estatísticas das tabelas
        System.out.println("\n--- ESTATÍSTICAS ---");
        System.out.println("Variáveis/Constantes: " + varTable.getScopeDepth() + " escopo(s)");
        System.out.println("Funções declaradas: " + functionTable.size());
        System.out.println("Arrays declarados: " + arrayTable.size());
        System.out.println("Strings literais: " + stringTable.size());
    }
}
