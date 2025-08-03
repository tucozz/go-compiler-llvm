package compiler.checker;

import Go_Parser.Go_ParserBaseVisitor;
import Go_Parser.Go_Parser;
import compiler.tables.StrTable;
import compiler.tables.VarTable;
import compiler.tables.VarTable.VarEntry;
import compiler.tables.FunctionTable;
import compiler.tables.FunctionInfo;
import compiler.typing.GoType;
import compiler.typing.TypeTable;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

public class GoSemanticChecker extends Go_ParserBaseVisitor<Void> {

    private VarTable varTable;
    private StrTable stringTable;
    private FunctionTable functionTable;
    private TypeTable typeTable;
    private boolean foundSemanticErrors;
    
    // Estado para rastreamento da função atual
    private GoType currentFunctionReturnType;
    private String currentFunctionName;
    
    // Estado para rastreamento de loops (para break/continue)
    private int loopDepth;

    public GoSemanticChecker() {
        this.varTable = new VarTable();
        this.stringTable = new StrTable();
        this.functionTable = new FunctionTable();
        this.typeTable = new TypeTable();
        this.foundSemanticErrors = false;
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

    public TypeTable getTypeTable() {
        return typeTable;
    }

    private void reportSemanticError(int lineNumber, String message) {
        System.err.println("SEMANTIC ERROR (" + lineNumber + "): " + message);
        foundSemanticErrors = true;
    }

    /**
     * Reporta erro semântico extraindo linha do contexto ANTLR
     */
    private void reportSemanticError(Object ctx, String message) {
        int lineNumber = extractLineNumber(ctx);
        reportSemanticError(lineNumber, message);
    }

    /**
     * Extrai número da linha de um contexto ANTLR usando reflexão
     */
    private int extractLineNumber(Object ctx) {
        try {
            // Tentar acessar getStart() do contexto
            Method getStartMethod = ctx.getClass().getMethod("getStart");
            Object token = getStartMethod.invoke(ctx);
            
            if (token != null) {
                Method getLineMethod = token.getClass().getMethod("getLine");
                Object lineObj = getLineMethod.invoke(token);
                if (lineObj instanceof Integer) {
                    return (Integer) lineObj;
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Não foi possível extrair linha: " + e.getMessage());
        }
        return 0; // Linha desconhecida
    }

    // --- PROGRAMA PRINCIPAL ---

    @Override
    public Void visitProgramRule(Go_Parser.ProgramRuleContext ctx) {
        System.out.println("DEBUG: Visitando programa principal");
        
        // Usar super para processar automaticamente todos os filhos
        return super.visitProgramRule(ctx);
    }

    // --- DECLARAÇÕES ---

    @Override
    public Void visitConstDeclarationStmt(Go_Parser.ConstDeclarationStmtContext ctx) {
        System.out.println("DEBUG: Visitando declaração de constante");
        // Usar super para processar automaticamente
        return super.visitConstDeclarationStmt(ctx);
    }

    @Override
    public Void visitTypeDeclarationStmt(Go_Parser.TypeDeclarationStmtContext ctx) {
        System.out.println("DEBUG: Visitando declaração de tipo");
        // Usar super para processar automaticamente
        return super.visitTypeDeclarationStmt(ctx);
    }

    @Override
    public Void visitVarDeclarationStmt(Go_Parser.VarDeclarationStmtContext ctx) {
        System.out.println("DEBUG: Visitando declaração de variável");
        // Usar super para processar automaticamente
        return super.visitVarDeclarationStmt(ctx);
    }

    // --- DECLARAÇÕES ESPECÍFICAS ---

    @Override
    public Void visitConstDecl(Go_Parser.ConstDeclContext ctx) {
        System.out.println("DEBUG: Processando constDeclaration");
        
        // Usar super para processar automaticamente
        return super.visitConstDecl(ctx);
    }

    @Override
    public Void visitTypeDecl(Go_Parser.TypeDeclContext ctx) {
        System.out.println("DEBUG: Processando typeDeclaration");
        
        // Por enquanto, apenas registrar que encontramos uma declaração de tipo
        // TODO: Implementar análise completa de tipos personalizados
        
        return null;
    }

    @Override
    public Void visitVarDecl(Go_Parser.VarDeclContext ctx) {
        System.out.println("DEBUG: Processando varDeclaration");
        
        // Usar super para processar automaticamente
        return super.visitVarDecl(ctx);
    }

    // --- ESPECIFICAÇÕES ---

    @Override
    public Void visitConstSpecification(Go_Parser.ConstSpecificationContext ctx) {
        System.out.println("DEBUG: Processando constSpecification");
        
        // Extrair informações da constante usando reflexão
        List<String> constNames = extractIdentifierNames(ctx, "identifierList");
        String typeInfo = extractTypeInfo(ctx, "typeSpec");
        
        System.out.println("DEBUG: Constantes encontradas: " + constNames);
        System.out.println("DEBUG: Tipo: " + typeInfo);
        
        // Processar cada constante
        for (String constName : constNames) {
            if (constName != null && !constName.isEmpty()) {
                GoType constType = GoType.fromString(typeInfo);
                
                System.out.println("DEBUG: Declarando constante '" + constName + "' do tipo " + constType);
                
                // Tentar adicionar a constante à tabela
                if (!varTable.addConstant(constName, constType, 0)) {
                    VarEntry existing = varTable.lookup(constName);
                    if (existing != null && varTable.existsInCurrentScope(constName)) {
                        reportSemanticError(0, 
                            "constant '" + constName + "' already declared at line " + existing.getDeclarationLine());
                    }
                } else {
                    // Adicionar também à tabela de tipos para referência
                    typeTable.addVariable(constName, constType);
                    System.out.println("DEBUG: Constante '" + constName + "' adicionada com sucesso");
                }
            }
        }
        
        return null;
    }

    @Override
    public Void visitVarSpecification(Go_Parser.VarSpecificationContext ctx) {
        System.out.println("DEBUG: Processando varSpecification");
        
        // Extrair informações da variável usando reflexão
        List<String> varNames = extractIdentifierNames(ctx, "identifierList");
        String typeInfo = extractTypeInfo(ctx, "typeSpec");
        
        System.out.println("DEBUG: Variáveis encontradas: " + varNames);
        System.out.println("DEBUG: Tipo: " + typeInfo);
        
        // Processar cada variável
        for (String varName : varNames) {
            if (varName != null && !varName.isEmpty()) {
                GoType varType = GoType.fromString(typeInfo);
                
                System.out.println("DEBUG: Declarando variável '" + varName + "' do tipo " + varType);
                
                // Tentar adicionar a variável à tabela
                if (!varTable.addVariable(varName, varType, 0)) {
                    VarEntry existing = varTable.lookup(varName);
                    if (existing != null && varTable.existsInCurrentScope(varName)) {
                        reportSemanticError(0, 
                            "variable '" + varName + "' already declared at line " + existing.getDeclarationLine());
                    }
                } else {
                    // Adicionar também à tabela de tipos para referência
                    typeTable.addVariable(varName, varType);
                    System.out.println("DEBUG: Variável '" + varName + "' adicionada com sucesso");
                }
            }
        }
        
        return null;
    }

    /**
     * Extrai nomes de identificadores usando reflexão
     */
    private List<String> extractIdentifierNames(Object ctx, String methodName) {
        List<String> names = new ArrayList<>();
        try {
            Method method = ctx.getClass().getMethod(methodName);
            Object identifierList = method.invoke(ctx);
            if (identifierList != null) {
                // Tentar extrair IDs do identifierList
                Method idMethod = identifierList.getClass().getMethod("ID");
                Object ids = idMethod.invoke(identifierList);
                if (ids != null && ids instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> idList = (java.util.List<Object>) ids;
                    for (Object id : idList) {
                        Method getTextMethod = id.getClass().getMethod("getText");
                        String name = (String) getTextMethod.invoke(id);
                        names.add(name);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Não foi possível extrair nomes via reflexão: " + e.getMessage());
            names.add("unknown_identifier");
        }
        return names;
    }

    /**
     * Extrai informação de tipo usando reflexão
     */
    private String extractTypeInfo(Object ctx, String methodName) {
        try {
            Method method = ctx.getClass().getMethod(methodName);
            Object typeSpec = method.invoke(ctx);
            if (typeSpec != null) {
                Method getTextMethod = typeSpec.getClass().getMethod("getText");
                return (String) getTextMethod.invoke(typeSpec);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Não foi possível extrair tipo via reflexão: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * Verifica se um tipo é um array e extrai informações
     */
    private boolean isArrayType(String typeStr) {
        return typeStr != null && typeStr.startsWith("[]");
    }

    /**
     * Extrai o tipo do elemento de um array
     */
    private String extractArrayElementType(String arrayType) {
        if (arrayType != null && arrayType.startsWith("[]")) {
            return arrayType.substring(2); // Remove "[]"
        }
        return "unknown";
    }

    /**
     * Extrai parâmetros de função usando reflexão
     */
    private java.util.List<String> extractParameterNames(Object ctx) {
        java.util.List<String> parameters = new java.util.ArrayList<>();
        if (ctx == null) return parameters;
        
        try {
            // Tenta acessar signature() da função
            Method signatureMethod = ctx.getClass().getMethod("signature");
            Object signature = signatureMethod.invoke(ctx);
            
            if (signature != null) {
                // Tenta acessar parameters()
                Method parametersMethod = signature.getClass().getMethod("parameters");
                Object params = parametersMethod.invoke(signature);
                
                if (params != null) {
                    // Tenta acessar parameterList()
                    Method parameterListMethod = params.getClass().getMethod("parameterList");
                    Object paramList = parameterListMethod.invoke(params);
                    
                    if (paramList != null) {
                        // Tenta acessar parameter() - agora o método correto
                        Method parameterMethod = paramList.getClass().getMethod("parameter");
                        Object paramDecls = parameterMethod.invoke(paramList);
                        
                        if (paramDecls instanceof java.util.List) {
                            java.util.List<?> declList = (java.util.List<?>) paramDecls;
                            for (Object decl : declList) {
                                // Extrai ID do parâmetro
                                try {
                                    Method idMethod = decl.getClass().getMethod("ID");
                                    Object idResult = idMethod.invoke(decl);
                                    if (idResult != null) {
                                        Method getTextMethod = idResult.getClass().getMethod("getText");
                                        String paramName = (String) getTextMethod.invoke(idResult);
                                        parameters.add(paramName);
                                    }
                                } catch (Exception e) {
                                    System.out.println("DEBUG: Erro ao extrair nome do parâmetro: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair parâmetros: " + e.getMessage());
        }
        
        return parameters;
    }

    /**
     * Extrai tipos de parâmetros usando reflexão
     */
    private java.util.List<String> extractParameterTypes(Object ctx) {
        java.util.List<String> types = new java.util.ArrayList<>();
        if (ctx == null) return types;
        
        try {
            Method signatureMethod = ctx.getClass().getMethod("signature");
            Object signature = signatureMethod.invoke(ctx);
            
            if (signature != null) {
                Method parametersMethod = signature.getClass().getMethod("parameters");
                Object params = parametersMethod.invoke(signature);
                
                if (params != null) {
                    Method parameterListMethod = params.getClass().getMethod("parameterList");
                    Object paramList = parameterListMethod.invoke(params);
                    
                    if (paramList != null) {
                        Method parameterMethod = paramList.getClass().getMethod("parameter");
                        Object paramDecls = parameterMethod.invoke(paramList);
                        
                        if (paramDecls instanceof java.util.List) {
                            java.util.List<?> declList = (java.util.List<?>) paramDecls;
                            for (Object decl : declList) {
                                String type = extractTypeInfo(decl, "typeSpec");
                                types.add(type);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair tipos de parâmetros: " + e.getMessage());
        }
        
        return types;
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
     * Extrai tipo de retorno de função usando reflexão
     */
    private String extractReturnType(Object ctx) {
        if (ctx == null) return "void";
        
        try {
            // Tenta acessar signature() da função
            Method signatureMethod = ctx.getClass().getMethod("signature");
            Object signature = signatureMethod.invoke(ctx);
            
            if (signature != null) {
                // Tenta acessar result()
                Method resultMethod = signature.getClass().getMethod("result");
                Object result = resultMethod.invoke(signature);
                
                if (result != null) {
                    // Se tem result, extrair o tipo
                    String returnType = extractTypeInfo(result, "typeSpec");
                    System.out.println("DEBUG: Tipo de retorno encontrado: " + returnType);
                    return returnType;
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair tipo de retorno: " + e.getMessage());
        }
        
        // Se não tem result explícito, é void
        return "void";
    }

    // --- FUNÇÕES ---

    @Override
    public Void visitFunctionDecl(Go_Parser.FunctionDeclContext ctx) {
        // Tentar extrair nome da função usando reflexão
        String functionName = extractFunctionName(ctx);
        System.out.println("DEBUG: Processando declaração de função '" + functionName + "'");
        
        // Extrair parâmetros da função
        java.util.List<String> paramNames = extractParameterNames(ctx);
        java.util.List<String> paramTypeNames = extractParameterTypes(ctx);
        
        // Extrair tipo de retorno da função
        String returnTypeName = extractReturnType(ctx);
        
        System.out.println("DEBUG: Parâmetros: " + paramNames);
        System.out.println("DEBUG: Tipos dos parâmetros: " + paramTypeNames);
        System.out.println("DEBUG: Tipo de retorno: " + returnTypeName);
        
        // Verificar se a função já foi declarada
        if (functionTable.hasFunction(functionName)) {
            reportSemanticError(0, 
                "function '" + functionName + "' already declared");
            return null;
        }
        
        // Criar novo escopo para a função
        varTable.enterScope();
        System.out.println("DEBUG: Entrando no escopo da função '" + functionName + "'");
        
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
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String typeName = i < paramTypeNames.size() ? paramTypeNames.get(i) : "unknown";
            GoType paramType = convertStringToGoType(typeName);
            
            if (!varTable.addVariable(paramName, paramType, 0)) {
                reportSemanticError(0, "parameter '" + paramName + "' already declared");
            } else {
                System.out.println("DEBUG: Parâmetro '" + paramName + "' do tipo " + typeName + " adicionado");
            }
        }
        
        // Adicionar função à tabela com tipo de retorno correto
        if (!functionTable.addFunction(functionName, paramNamesJava, paramTypes, returnType, 0)) {
            reportSemanticError(0, "Failed to add function '" + functionName + "'");
        } else {
            System.out.println("DEBUG: Função '" + functionName + "' adicionada com tipo de retorno " + returnTypeName);
            // Marcar como definida (já que tem corpo)
            functionTable.markAsDefined(functionName);
        }
        
        // Processar corpo da função (delegar aos visitadores padrão)
        super.visitFunctionDecl(ctx);
        
        // Sair do escopo da função
        varTable.exitScope();
        System.out.println("DEBUG: Saindo do escopo da função '" + functionName + "'");
        
        // Limpar rastreamento da função atual
        currentFunctionName = null;
        currentFunctionReturnType = null;
        
        return null;
    }

    /**
     * Extrai o nome da função usando reflexão
     */
    private String extractFunctionName(Go_Parser.FunctionDeclContext ctx) {
        try {
            // Tentar usar o método ID() via reflexão
            Method idMethod = ctx.getClass().getMethod("ID");
            Object idNode = idMethod.invoke(ctx);
            if (idNode != null) {
                Method getTextMethod = idNode.getClass().getMethod("getText");
                String functionName = (String) getTextMethod.invoke(idNode);
                return functionName;
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Não foi possível extrair nome da função via reflexão: " + e.getMessage());
        }
        
        // Fallback: usar nome genérico
        return "function_" + System.currentTimeMillis();
    }

    @Override 
    public Void visitBlockCode(Go_Parser.BlockCodeContext ctx) {
        System.out.println("DEBUG: Processando bloco de código");
        
        // Usar super para processar automaticamente todos os filhos
        return super.visitBlockCode(ctx);
    }

    /**
     * Converte um contexto de tipo em string
     */
    private String getTypeString(Go_Parser.TypeSpecContext typeCtx) {
        if (typeCtx instanceof Go_Parser.TypeIntContext) {
            return "int";
        } else if (typeCtx instanceof Go_Parser.TypeStringContext) {
            return "string";
        } else if (typeCtx instanceof Go_Parser.TypeBoolContext) {
            return "bool";
        } else if (typeCtx instanceof Go_Parser.TypeFloat64Context) {
            return "float64";
        } else if (typeCtx instanceof Go_Parser.TypeFloat32Context) {
            return "float32";
        }
        // Adicionar outros tipos conforme necessário
        return "unknown";
    }

    /**
     * Extrai o tipo de retorno de uma função
     */
    private GoType extractReturnType(Go_Parser.ResultContext resultCtx) {
        if (resultCtx instanceof Go_Parser.ResultSingleTypeContext) {
            // TODO: Implementar extração de tipo correta
            // Go_Parser.ResultSingleTypeContext singleTypeCtx = (Go_Parser.ResultSingleTypeContext) resultCtx;
            // String typeString = singleTypeCtx.typeSpec().getText();
            // return GoType.fromString(typeString);
            return GoType.INT; // Tipo padrão temporário
        } else if (resultCtx instanceof Go_Parser.ResultParametersContext) {
            // Para múltiplos valores de retorno, por enquanto retornamos UNKNOWN
            // TODO: Implementar suporte para múltiplos valores de retorno
            return GoType.UNKNOWN;
        }
        
        return GoType.VOID;
    }

    // --- LITERAIS ---

    @Override
    public Void visitStringLiteral(Go_Parser.StringLiteralContext ctx) {
        // TODO: Implementar corretamente quando TerminalNode estiver disponível
        // String stringValueWithQuotes = ctx.STRING_LIT().getText();
        // String stringContent = stringValueWithQuotes.substring(1, stringValueWithQuotes.length() - 1);
        // stringTable.addString(stringContent);
        System.out.println("DEBUG: Encontrado literal string");
        return null;
    }

    @Override
    public Void visitIdExpr(Go_Parser.IdExprContext ctx) {
        // TODO: Implementar corretamente quando TerminalNode estiver disponível
        // String varName = ctx.ID().getText();
        // int lineNumber = ctx.ID().getSymbol().getLine();

        System.out.println("DEBUG: Encontrado identificador em expressão");
        
        // Por enquanto, não fazer verificação de variáveis
        return null;
    }

    /**
     * Processa chamadas de função
     */
    @Override
    public Void visitCallExpression(Go_Parser.CallExpressionContext ctx) {
        // Extrair nome da função usando reflexão
        String functionName = extractFunctionNameFromCall(ctx);
        System.out.println("DEBUG: Processando chamada de função '" + functionName + "'");
        
        // Verificar se a função existe
        if (!functionTable.hasFunction(functionName)) {
            reportSemanticError(0, "undefined function '" + functionName + "'");
        } else {
            System.out.println("DEBUG: Função '" + functionName + "' encontrada na tabela");
            
            // Extrair argumentos da chamada
            java.util.List<String> arguments = extractCallArguments(ctx);
            System.out.println("DEBUG: Argumentos da chamada: " + arguments);
            
            // Validar número de argumentos
            FunctionInfo funcInfo = functionTable.getFunction(functionName);
            if (funcInfo != null) {
                List<GoType> expectedParamTypes = funcInfo.getParameterTypes();
                
                if (arguments.size() != expectedParamTypes.size()) {
                    reportSemanticError(0, 
                        "function '" + functionName + "' expects " + expectedParamTypes.size() + 
                        " arguments, got " + arguments.size());
                } else {
                    System.out.println("DEBUG: Número de argumentos correto para '" + functionName + "'");
                    
                    // Validar tipos dos argumentos (implementação básica)
                    for (int i = 0; i < arguments.size(); i++) {
                        String arg = arguments.get(i);
                        GoType expectedType = expectedParamTypes.get(i);
                        GoType argType = inferArgumentType(arg);
                        
                        System.out.println("DEBUG: Argumento " + (i + 1) + ": '" + arg + 
                                         "' (esperado: " + expectedType.getTypeName() + 
                                         ", inferido: " + argType.getTypeName() + ")");
                        
                        if (!areTypesCompatible(argType, expectedType)) {
                            reportSemanticError(0, 
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
     * Extrai nome da função de uma chamada usando reflexão
     */
    private String extractFunctionNameFromCall(Object ctx) {
        try {
            Method idMethod = ctx.getClass().getMethod("ID");
            Object idResult = idMethod.invoke(ctx);
            if (idResult != null) {
                Method getTextMethod = idResult.getClass().getMethod("getText");
                return (String) getTextMethod.invoke(idResult);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair nome da função da chamada: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * Extrai argumentos de uma chamada de função usando reflexão
     */
    private java.util.List<String> extractCallArguments(Object ctx) {
        java.util.List<String> arguments = new java.util.ArrayList<>();
        
        try {
            // Tenta acessar expressionList() da chamada
            Method expressionListMethod = ctx.getClass().getMethod("expressionList");
            Object exprList = expressionListMethod.invoke(ctx);
            
            if (exprList != null) {
                // Tenta acessar expr() para obter as expressões
                Method exprMethod = exprList.getClass().getMethod("expr");
                Object exprs = exprMethod.invoke(exprList);
                
                if (exprs instanceof java.util.List) {
                    java.util.List<?> exprListResult = (java.util.List<?>) exprs;
                    for (Object expr : exprListResult) {
                        try {
                            Method getTextMethod = expr.getClass().getMethod("getText");
                            String argText = (String) getTextMethod.invoke(expr);
                            arguments.add(argText);
                        } catch (Exception e) {
                            arguments.add("unknown_arg");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair argumentos: " + e.getMessage());
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
                System.out.println("DEBUG: Variável '" + arg + "' encontrada com tipo " + varEntry.getType().getTypeName());
                return varEntry.getType();
            }
        }
        
        // Tentar analisar como expressão aritmética simples
        GoType exprType = analyzeSimpleExpression(arg);
        if (exprType != GoType.UNKNOWN) {
            return exprType;
        }
        
        // Se não encontrou a variável, reportar erro
        System.out.println("DEBUG: Variável '" + arg + "' não encontrada ou tipo desconhecido");
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
                
                System.out.println("DEBUG: Analisando expressão aritmética: '" + left + "' " + op + " '" + right + "'");
                
                GoType leftType = inferSimpleOperand(left);
                GoType rightType = inferSimpleOperand(right);
                
                // Para operações aritméticas, usar TypeTable
                GoType resultType = typeTable.getBinaryOperationResultType(leftType, rightType, op);
                System.out.println("DEBUG: Resultado da expressão: " + resultType.getTypeName());
                
                return resultType;
            }
        }
        
        // Depois, tentar operações de comparação
        for (String op : comparisonOps) {
            int opIndex = expr.indexOf(op);
            if (opIndex > 0 && opIndex < expr.length() - op.length()) {
                String left = expr.substring(0, opIndex).trim();
                String right = expr.substring(opIndex + op.length()).trim();
                
                System.out.println("DEBUG: Analisando expressão de comparação: '" + left + "' " + op + " '" + right + "'");
                
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
     */
    private boolean areTypesCompatible(GoType actual, GoType expected) {
        if (actual == null || expected == null) {
            return false;
        }
        
        // Tipos idênticos são compatíveis
        if (actual == expected) {
            return true;
        }
        
        // UNKNOWN é compatível com qualquer tipo (para casos onde não conseguimos inferir)
        if (actual == GoType.UNKNOWN || expected == GoType.UNKNOWN) {
            return true;
        }
        
        // Outras regras de compatibilidade podem ser adicionadas aqui
        // Por exemplo: int compatível com float64, etc.
        
        return false;
    }

    /**
     * Processa statements de return
     */
    @Override
    public Void visitReturnStatementWithExpr(Go_Parser.ReturnStatementWithExprContext ctx) {
        System.out.println("DEBUG: Processando statement return");
        
        if (currentFunctionName == null || currentFunctionReturnType == null) {
            reportSemanticError(0, "return statement outside of function");
            return null;
        }
        
        // Extrair expressão de retorno usando reflexão
        String returnExpr = extractReturnExpression(ctx);
        System.out.println("DEBUG: Expressão de retorno: '" + returnExpr + "'");
        
        if (returnExpr == null || returnExpr.trim().isEmpty()) {
            // Return sem expressão
            if (currentFunctionReturnType != GoType.VOID) {
                reportSemanticError(0, 
                    "function '" + currentFunctionName + "' expects return type " + 
                    currentFunctionReturnType.getTypeName() + ", but got void");
            } else {
                System.out.println("DEBUG: Return void correto para função " + currentFunctionName);
            }
        } else {
            // Return com expressão - verificar tipo
            GoType returnType = inferArgumentType(returnExpr.trim());
            System.out.println("DEBUG: Tipo inferido do return: " + returnType.getTypeName() + 
                             ", esperado: " + currentFunctionReturnType.getTypeName());
            
            if (!areTypesCompatible(returnType, currentFunctionReturnType)) {
                reportSemanticError(0, 
                    "function '" + currentFunctionName + "' expects return type " + 
                    currentFunctionReturnType.getTypeName() + ", but got " + 
                    returnType.getTypeName());
            } else {
                System.out.println("DEBUG: Tipo de return correto para função " + currentFunctionName);
            }
        }
        
        // Continuar processamento
        super.visitReturnStatementWithExpr(ctx);
        return null;
    }

    /**
     * Extrai expressão de return usando reflexão
     */
    private String extractReturnExpression(Object ctx) {
        try {
            // Tenta acessar expr() do return statement
            Method exprMethod = ctx.getClass().getMethod("expr");
            Object expr = exprMethod.invoke(ctx);
            
            if (expr != null) {
                Method getTextMethod = expr.getClass().getMethod("getText");
                return (String) getTextMethod.invoke(expr);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair expressão de return: " + e.getMessage());
        }
        
        return null; // Return sem expressão
    }

    // --- STATEMENTS DE CONTROLE ---

    /**
     * Processa assignments simples (x = y)
     */
    @Override
    public Void visitSimpleAssignStatement(Go_Parser.SimpleAssignStatementContext ctx) {
        System.out.println("DEBUG: Processando assignment simples");
        
        // Extrair lvalue completo (lado esquerdo) usando reflexão
        String fullLvalue = extractFullLValue(ctx);
        String lvalue = extractLValue(ctx);
        System.out.println("DEBUG: LValue completo: '" + fullLvalue + "', base: '" + lvalue + "'");
        
        // Extrair expressão (lado direito) usando reflexão
        String rvalue = extractAssignmentExpression(ctx);
        System.out.println("DEBUG: RValue: '" + rvalue + "'");
        
        // Verificar se a variável no lvalue existe
        if (lvalue != null && !lvalue.trim().isEmpty()) {
            if (!varTable.exists(lvalue)) {
                reportSemanticError(0, "undefined variable '" + lvalue + "'");
            } else {
                VarEntry varEntry = varTable.lookup(lvalue);
                if (varEntry != null) {
                    // Verificar se é uma constante
                    if (varEntry.isConstant()) {
                        reportSemanticError(0, "cannot assign to constant '" + lvalue + "'");
                    } else {
                        // Determinar o tipo do lvalue (considerando acesso a array)
                        GoType lvalueType = varEntry.getType();
                        
                        // Se é acesso a array (var[index]), o tipo é o tipo do elemento
                        if (fullLvalue.contains("[") && fullLvalue.contains("]")) {
                            if (lvalueType.isArray()) {
                                lvalueType = lvalueType.getElementType();
                                System.out.println("DEBUG: Assignment para array, tipo do elemento: " + lvalueType.getTypeName());
                                
                                // Validar índice do array
                                String indexPart = fullLvalue.substring(fullLvalue.indexOf("[") + 1, fullLvalue.indexOf("]"));
                                GoType indexType = inferArgumentType(indexPart);
                                if (indexType != GoType.INT && indexType != GoType.UNKNOWN) {
                                    reportSemanticError(0, "array index must be integer, got " + indexType.getTypeName());
                                }
                            } else {
                                reportSemanticError(0, "'" + lvalue + "' is not an array");
                                return super.visitSimpleAssignStatement(ctx);
                            }
                        }
                        
                        // Verificar compatibilidade de tipos
                        GoType rvalueType = inferArgumentType(rvalue);
                        
                        System.out.println("DEBUG: Assignment - LValue tipo: " + lvalueType.getTypeName() + 
                                         ", RValue tipo: " + rvalueType.getTypeName());
                        
                        if (!areTypesCompatible(rvalueType, lvalueType)) {
                            reportSemanticError(0, 
                                "cannot assign " + rvalueType.getTypeName() + 
                                " to variable '" + lvalue + "' of type " + lvalueType.getTypeName());
                        } else {
                            System.out.println("DEBUG: Assignment válido para '" + lvalue + "'");
                        }
                    }
                }
            }
        }
        
        // Continuar processamento
        super.visitSimpleAssignStatement(ctx);
        return null;
    }

    /**
     * Extrai lvalue completo (incluindo acesso a array) usando reflexão
     */
    private String extractFullLValue(Object ctx) {
        try {
            Method lvalueMethod = ctx.getClass().getMethod("lvalue");
            Object lvalue = lvalueMethod.invoke(ctx);
            
            if (lvalue != null) {
                Method getTextMethod = lvalue.getClass().getMethod("getText");
                return (String) getTextMethod.invoke(lvalue);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair lvalue completo: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * Extrai lvalue (lado esquerdo) de um assignment usando reflexão
     */
    private String extractLValue(Object ctx) {
        try {
            Method lvalueMethod = ctx.getClass().getMethod("lvalue");
            Object lvalue = lvalueMethod.invoke(ctx);
            
            if (lvalue != null) {
                Method getTextMethod = lvalue.getClass().getMethod("getText");
                String lvalueText = (String) getTextMethod.invoke(lvalue);
                
                // Verificar se é acesso a array (formato: var[index])
                if (lvalueText.contains("[") && lvalueText.contains("]")) {
                    // Extrair nome da variável antes do [
                    int bracketIndex = lvalueText.indexOf("[");
                    return lvalueText.substring(0, bracketIndex);
                }
                
                return lvalueText;
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair lvalue: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * Extrai expressão de assignment usando reflexão
     */
    private String extractAssignmentExpression(Object ctx) {
        try {
            Method exprMethod = ctx.getClass().getMethod("expr");
            Object expr = exprMethod.invoke(ctx);
            
            if (expr != null) {
                Method getTextMethod = expr.getClass().getMethod("getText");
                return (String) getTextMethod.invoke(expr);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair expressão de assignment: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * Processa statements if-else
     */
    @Override
    public Void visitIfElseStatement(Go_Parser.IfElseStatementContext ctx) {
        System.out.println("DEBUG: Processando statement if-else");
        
        // Extrair e validar condição do if
        String condition = extractIfCondition(ctx);
        System.out.println("DEBUG: Condição do if: '" + condition + "'");
        
        if (condition != null && !condition.trim().isEmpty()) {
            GoType conditionType = inferArgumentType(condition);
            System.out.println("DEBUG: Tipo da condição: " + conditionType.getTypeName());
            
            // Em Go, condições devem ser do tipo bool
            if (conditionType != GoType.BOOL && conditionType != GoType.UNKNOWN) {
                reportSemanticError(0, 
                    "if condition must be boolean, got " + conditionType.getTypeName());
            } else {
                System.out.println("DEBUG: Condição do if é válida");
            }
        }
        
        // Processar blocos if e else (criar novos escopos se necessário)
        System.out.println("DEBUG: Processando bloco if");
        
        // Continuar processamento automático dos blocos
        super.visitIfElseStatement(ctx);
        
        System.out.println("DEBUG: Finalizou processamento if-else");
        return null;
    }

    /**
     * Extrai condição do if usando reflexão
     */
    private String extractIfCondition(Object ctx) {
        try {
            Method exprMethod = ctx.getClass().getMethod("expr");
            Object expr = exprMethod.invoke(ctx);
            
            if (expr != null) {
                Method getTextMethod = expr.getClass().getMethod("getText");
                return (String) getTextMethod.invoke(expr);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair condição do if: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * Processa statements for (loops)
     */
    @Override
    public Void visitForLoopStatement(Go_Parser.ForLoopStatementContext ctx) {
        System.out.println("DEBUG: Processando statement for");
        
        // Entrar no loop (incrementar profundidade)
        loopDepth++;
        System.out.println("DEBUG: Entrando no loop, profundidade: " + loopDepth);
        
        // Verificar se tem condição de loop
        String condition = extractForCondition(ctx);
        if (condition != null && !condition.trim().isEmpty()) {
            System.out.println("DEBUG: Condição do for: '" + condition + "'");
            
            GoType conditionType = inferArgumentType(condition);
            System.out.println("DEBUG: Tipo da condição do for: " + conditionType.getTypeName());
            
            // Em Go, condições de loop devem ser do tipo bool
            if (conditionType != GoType.BOOL && conditionType != GoType.UNKNOWN) {
                reportSemanticError(0, 
                    "for condition must be boolean, got " + conditionType.getTypeName());
            } else {
                System.out.println("DEBUG: Condição do for é válida");
            }
        } else {
            System.out.println("DEBUG: For sem condição (loop infinito ou com range)");
        }
        
        // Processar corpo do loop
        System.out.println("DEBUG: Processando corpo do for");
        
        // Continuar processamento automático
        super.visitForLoopStatement(ctx);
        
        // Sair do loop (decrementar profundidade)
        loopDepth--;
        System.out.println("DEBUG: Saindo do loop, profundidade: " + loopDepth);
        
        return null;
    }

    /**
     * Extrai condição do for usando reflexão
     */
    private String extractForCondition(Object ctx) {
        try {
            // Tentar acessar expr() diretamente (for simples com condição)
            Method exprMethod = ctx.getClass().getMethod("expr");
            Object expr = exprMethod.invoke(ctx);
            
            if (expr != null) {
                Method getTextMethod = expr.getClass().getMethod("getText");
                return (String) getTextMethod.invoke(expr);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: For não tem condição direta ou erro: " + e.getMessage());
        }
        
        return null; // For sem condição simples
    }

    /**
     * Processa acesso a arrays (arr[index])
     */
    @Override
    public Void visitArrayAccessExpr(Go_Parser.ArrayAccessExprContext ctx) {
        System.out.println("DEBUG: Processando acesso a array");
        
        // Extrair nome do array e índice usando reflexão
        String arrayName = extractArrayName(ctx);
        String indexExpr = extractArrayIndex(ctx);
        
        System.out.println("DEBUG: Array: '" + arrayName + "', Índice: '" + indexExpr + "'");
        
        // Verificar se o array existe
        if (arrayName != null && !arrayName.trim().isEmpty()) {
            if (!varTable.exists(arrayName)) {
                reportSemanticError(0, "undefined array '" + arrayName + "'");
            } else {
                VarEntry arrayEntry = varTable.lookup(arrayName);
                if (arrayEntry != null) {
                    GoType arrayType = arrayEntry.getType();
                    
                    // Verificar se é realmente um array
                    if (!arrayType.isArray()) {
                        reportSemanticError(0, 
                            "'" + arrayName + "' is not an array (type: " + arrayType.getTypeName() + ")");
                    } else {
                        System.out.println("DEBUG: Array '" + arrayName + "' válido do tipo " + arrayType.getTypeName());
                        
                        // Verificar se o índice é do tipo int
                        if (indexExpr != null && !indexExpr.trim().isEmpty()) {
                            GoType indexType = inferArgumentType(indexExpr);
                            if (indexType != GoType.INT && indexType != GoType.UNKNOWN) {
                                reportSemanticError(0, 
                                    "array index must be integer, got " + indexType.getTypeName());
                            } else {
                                System.out.println("DEBUG: Índice do array é válido");
                            }
                        }
                    }
                }
            }
        }
        
        // Continuar processamento
        super.visitArrayAccessExpr(ctx);
        return null;
    }

    /**
     * Extrai nome do array usando reflexão
     */
    private String extractArrayName(Object ctx) {
        try {
            // Tentar acessar expr() que representa o array
            Method exprMethod = ctx.getClass().getMethod("expr");
            Object expr = exprMethod.invoke(ctx);
            
            if (expr != null) {
                Method getTextMethod = expr.getClass().getMethod("getText");
                return (String) getTextMethod.invoke(expr);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair nome do array: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * Extrai índice do array usando reflexão
     */
    private String extractArrayIndex(Object ctx) {
        try {
            // Tentar acessar arrayIndex()
            Method arrayIndexMethod = ctx.getClass().getMethod("arrayIndex");
            Object arrayIndex = arrayIndexMethod.invoke(ctx);
            
            if (arrayIndex != null) {
                Method getTextMethod = arrayIndex.getClass().getMethod("getText");
                return (String) getTextMethod.invoke(arrayIndex);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao extrair índice do array: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * Processa statements break
     */
    @Override
    public Void visitBreakStatementRule(Go_Parser.BreakStatementRuleContext ctx) {
        System.out.println("DEBUG: Processando statement break");
        
        // Verificar se estamos dentro de um loop
        if (loopDepth == 0) {
            reportSemanticError(0, "break statement not in loop");
        } else {
            System.out.println("DEBUG: Break statement válido (dentro de loop, profundidade: " + loopDepth + ")");
        }
        
        return null;
    }

    /**
     * Processa statements continue  
     */
    @Override
    public Void visitContinueStatementRule(Go_Parser.ContinueStatementRuleContext ctx) {
        System.out.println("DEBUG: Processando statement continue");
        
        // Verificar se estamos dentro de um loop
        if (loopDepth == 0) {
            reportSemanticError(0, "continue statement not in loop");
        } else {
            System.out.println("DEBUG: Continue statement válido (dentro de loop, profundidade: " + loopDepth + ")");
        }
        
        return null;
    }

    // --- EXPRESSÕES ARITMÉTICAS ---

    @Override
    public Void visitAddSubExpr(Go_Parser.AddSubExprContext ctx) {
        System.out.println("DEBUG: Processando expressão aritmética (+/-)");
        
        // Processar operandos
        super.visitAddSubExpr(ctx);
        
        // TODO: Implementar validação de tipos dos operandos
        // Por enquanto, apenas registrar que encontramos
        
        return null;
    }

    @Override
    public Void visitMultiplyDivideModExpr(Go_Parser.MultiplyDivideModExprContext ctx) {
        System.out.println("DEBUG: Processando expressão aritmética (*, /, %)");
        
        // Processar operandos
        super.visitMultiplyDivideModExpr(ctx);
        
        // TODO: Implementar validação de tipos dos operandos
        
        return null;
    }

    @Override
    public Void visitUnaryPrefixExpr(Go_Parser.UnaryPrefixExprContext ctx) {
        System.out.println("DEBUG: Processando expressão unária (+, -, !)");
        
        // Processar operando
        super.visitUnaryPrefixExpr(ctx);
        
        // TODO: Implementar validação de tipos do operando
        
        return null;
    }

    // --- EXPRESSÕES LÓGICAS ---

    @Override
    public Void visitLogicalANDExpr(Go_Parser.LogicalANDExprContext ctx) {
        System.out.println("DEBUG: Processando expressão lógica AND (&&)");
        
        // Processar operandos
        super.visitLogicalANDExpr(ctx);
        
        // TODO: Validar que ambos operandos são bool
        
        return null;
    }

    @Override
    public Void visitLogicalORExpr(Go_Parser.LogicalORExprContext ctx) {
        System.out.println("DEBUG: Processando expressão lógica OR (||)");
        
        // Processar operandos
        super.visitLogicalORExpr(ctx);
        
        // TODO: Validar que ambos operandos são bool
        
        return null;
    }

    @Override
    public Void visitComparisonExpr(Go_Parser.ComparisonExprContext ctx) {
        System.out.println("DEBUG: Processando expressão de comparação");
        
        // Processar operandos
        super.visitComparisonExpr(ctx);
        
        // TODO: Validar que operandos são comparáveis
        
        return null;
    }

    // --- LITERAIS ---

    @Override
    public Void visitIntLiteral(Go_Parser.IntLiteralContext ctx) {
        System.out.println("DEBUG: Encontrado literal inteiro");
        // Literais são válidos por definição
        return null;
    }

    @Override
    public Void visitFloatLiteral(Go_Parser.FloatLiteralContext ctx) {
        System.out.println("DEBUG: Encontrado literal float");
        return null;
    }

    @Override
    public Void visitTrueLiteral(Go_Parser.TrueLiteralContext ctx) {
        System.out.println("DEBUG: Encontrado literal true");
        return null;
    }

    @Override
    public Void visitFalseLiteral(Go_Parser.FalseLiteralContext ctx) {
        System.out.println("DEBUG: Encontrado literal false");
        return null;
    }

    // --- EXPRESSÕES PARENTIZADAS ---

    @Override
    public Void visitParenthesizedExpr(Go_Parser.ParenthesizedExprContext ctx) {
        System.out.println("DEBUG: Processando expressão parentizada");
        
        // Processar expressão interna
        super.visitParenthesizedExpr(ctx);
        
        return null;
    }

    // --- ATRIBUIÇÕES COMPOSTAS ---

    @Override
    public Void visitCompoundAssignStatement(Go_Parser.CompoundAssignStatementContext ctx) {
        System.out.println("DEBUG: Processando atribuição composta (+=, -=, etc.)");
        
        // Processar lvalue e expressão
        super.visitCompoundAssignStatement(ctx);
        
        // TODO: Validar tipos compatíveis para a operação
        
        return null;
    }

    // --- DECLARAÇÃO CURTA ---

    @Override
    public Void visitShortVariableDecl(Go_Parser.ShortVariableDeclContext ctx) {
        System.out.println("DEBUG: Processando declaração curta (:=)");
        
        // Extrair nomes das variáveis
        List<String> varNames = extractIdentifierNames(ctx, "identifierList");
        System.out.println("DEBUG: Variáveis para declaração curta: " + varNames);
        
        // TODO: Inferir tipos das expressões e adicionar variáveis
        // Por enquanto, apenas processar filhos
        super.visitShortVariableDecl(ctx);
        
        return null;
    }

    // --- INCREMENT/DECREMENT ---

    @Override
    public Void visitIncDecOperationStatement(Go_Parser.IncDecOperationStatementContext ctx) {
        System.out.println("DEBUG: Processando operação de incremento/decremento");
        
        // Processar lvalue
        super.visitIncDecOperationStatement(ctx);
        
        // TODO: Validar que é um tipo numérico
        
        return null;
    }

    // --- EXPRESSÕES PRIMÁRIAS ---

    @Override
    public Void visitPrimaryOrPostfixExpr(Go_Parser.PrimaryOrPostfixExprContext ctx) {
        System.out.println("DEBUG: Processando expressão primária ou pós-fixa");
        
        // Processar expressão primária
        super.visitPrimaryOrPostfixExpr(ctx);
        
        return null;
    }

    // --- CONVERSÃO DE TIPOS ---

    @Override
    public Void visitTypeConversion(Go_Parser.TypeConversionContext ctx) {
        System.out.println("DEBUG: Processando conversão de tipo");
        
        // Extrair tipo de destino
        String targetType = extractTypeInfo(ctx, "typeSpec");
        System.out.println("DEBUG: Conversão para tipo: " + targetType);
        
        // Processar expressão sendo convertida
        super.visitTypeConversion(ctx);
        
        // TODO: Validar se a conversão é permitida
        
        return null;
    }

    // --- ARRAYS COMPOSTOS ---

    @Override
    public Void visitArraySliceLiteral(Go_Parser.ArraySliceLiteralContext ctx) {
        System.out.println("DEBUG: Processando literal de array/slice");
        
        // Processar elementos
        super.visitArraySliceLiteral(ctx);
        
        // TODO: Validar que todos elementos são do mesmo tipo
        
        return null;
    }

    @Override
    public Void visitCompositeLiteralExpr(Go_Parser.CompositeLiteralExprContext ctx) {
        System.out.println("DEBUG: Processando expressão de literal composto");
        
        // Processar literal composto
        super.visitCompositeLiteralExpr(ctx);
        
        return null;
    }

    // --- LVALUES ---

    @Override
    public Void visitIdLvalue(Go_Parser.IdLvalueContext ctx) {
        System.out.println("DEBUG: Processando lvalue de identificador");
        
        // TODO: Validar que identificador existe e pode ser atribuído
        
        return null;
    }

    @Override
    public Void visitArrayAccessLvalue(Go_Parser.ArrayAccessLvalueContext ctx) {
        System.out.println("DEBUG: Processando lvalue de acesso a array");
        
        // Processar acesso ao array
        super.visitArrayAccessLvalue(ctx);
        
        return null;
    }

    // --- LOOPS FOR ---

    @Override
    public Void visitForClassicClause(Go_Parser.ForClassicClauseContext ctx) {
        System.out.println("DEBUG: Processando cláusula for clássica (init; cond; post)");
        
        // Processar componentes do for
        super.visitForClassicClause(ctx);
        
        return null;
    }

    @Override
    public Void visitForRangeClauseExpr(Go_Parser.ForRangeClauseExprContext ctx) {
        System.out.println("DEBUG: Processando cláusula for range");
        
        // TODO: Validar que expressão é iterável (array, slice, etc.)
        super.visitForRangeClauseExpr(ctx);
        
        return null;
    }

    // --- STATEMENTS SIMPLES ---

    @Override
    public Void visitExpressionSimpleStmt(Go_Parser.ExpressionSimpleStmtContext ctx) {
        System.out.println("DEBUG: Processando statement de expressão simples");
        
        // Processar expressão
        super.visitExpressionSimpleStmt(ctx);
        
        return null;
    }

    // --- CAST DE TIPOS ---

    @Override
    public Void visitTypeCastExpr(Go_Parser.TypeCastExprContext ctx) {
        System.out.println("DEBUG: Processando expressão de cast de tipo");
        
        // Processar conversão
        super.visitTypeCastExpr(ctx);
        
        return null;
    }

    /**
     * Verifica se um identificador é uma palavra-chave Go
     */
    private boolean isGoKeyword(String identifier) {
        String[] goKeywords = {
                "package", "import", "func", "var", "const", "type",
                "if", "else", "for", "switch", "case", "default", "return",
                "break", "continue", "go", "defer", "select", "chan", "range",
                "interface", "map", "make", "new", "append", "len", "cap",
                "copy", "delete", "panic", "recover", "print", "println",
                "true", "false", "nil", "iota",
                "main", "fmt"
        };

        for (String keyword : goKeywords) {
            if (keyword.equals(identifier)) {
                return true;
            }
        }
        return false;
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
        
        // Estatísticas das tabelas
        System.out.println("\n--- Estatísticas ---");
        System.out.println("Variáveis/Constantes: " + varTable.getScopeDepth() + " escopo(s)");
        System.out.println("Funções declaradas: " + functionTable.size());
        System.out.println("Strings literais: " + stringTable.size());
    }
}
