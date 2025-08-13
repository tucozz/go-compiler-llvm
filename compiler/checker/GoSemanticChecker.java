package compiler.checker;

import Go_Parser.Go_ParserBaseVisitor;
import Go_Parser.Go_Parser;
import compiler.tables.StrTable;
import compiler.tables.VarTable;
import compiler.tables.VarTable.VarEntry;
import compiler.tables.FunctionTable;
import compiler.tables.FunctionInfo;
import compiler.tables.ArrayTable;
import compiler.ast.AST;
import compiler.ast.NodeKind;
import compiler.tables.ArrayInfo;
import compiler.typing.GoType;
import compiler.typing.TypeTable;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.antlr.v4.runtime.tree.TerminalNode;

public class GoSemanticChecker extends Go_ParserBaseVisitor<AST> {

    AST root;
    GoType lastDeclType;

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

    // Visita a regra program: (statement)* EOF
    @Override
    public AST visitProgramRule(Go_Parser.ProgramRuleContext ctx) {
        AST programNode = new AST(NodeKind.PROGRAM_NODE, GoType.NO_TYPE);
        for (Go_Parser.StatementContext stmtCtx : ctx.statement()) {
            AST childNode = super.visit(stmtCtx);
            System.out.println("DEBUG: Adding child node: " + childNode.toString());
            if (childNode != null) {
                programNode.addChild(childNode);
            }
        }

        // Salvar a AST raiz para impressão posterior
        this.root = programNode;
        return this.root;
    }

    // --- DECLARAÇÕES ---

    @Override
    public AST visitConstDecl(Go_Parser.ConstDeclContext ctx) {
        System.out.println("DEBUG: ConstDecl Principal");

        AST constDeclNode = new AST(NodeKind.CONST_DECL_NODE, GoType.NO_TYPE);
        constDeclNode.addChild(super.visitConstDecl(ctx));
        return constDeclNode;
    }

    @Override
    public AST visitVarDecl(Go_Parser.VarDeclContext ctx) {
        System.out.println("DEBUG: VarDecl Principal");
        AST varDeclNode = new AST(NodeKind.VAR_DECL_NODE, GoType.NO_TYPE);
        varDeclNode.addChild(super.visitVarDecl(ctx));
        return varDeclNode;
    }

    // // --- ESPECIFICAÇÕES ---

    // // Regra identifierList (typeSpec)? ASSIGN expressionList
    // @Override
    // public Void visitConstSpecification(Go_Parser.ConstSpecificationContext ctx)
    // {
    // System.out.println("DEBUG: ConstSpec");

    // // Extrair typeSpec
    // String typeSpec = "unknown";
    // if (ctx.typeSpec() != null) {
    // typeSpec = ctx.typeSpec().getText();
    // }
    // System.out.println("DEBUG: typeSpec = " + typeSpec);

    // // Extrair identifierLIst
    // String[] identifiers = ctx.identifierList().getText().split(",");
    // for (String id : identifiers) {
    // System.out.println("DEBUG: identifier = " + id.trim());
    // }

    // // Extrair número da linha
    // int lineNumber = ctx.start.getLine();
    // System.out.println("DEBUG: lineNumber = " + lineNumber);

    // // Processar cada constante
    // for (String id : identifiers) {
    // if (id != null && !id.isEmpty()) {
    // GoType constType = GoType.fromString(typeSpec);

    // // Tentar adicionar a constante à tabela
    // if (!varTable.addConstant(id, constType, lineNumber)) {
    // VarEntry existing = varTable.lookup(id);
    // if (existing != null && varTable.existsInCurrentScope(id)) {
    // reportSemanticError(ctx,
    // "constant '" + id + "' already declared at line " +
    // existing.getDeclarationLine());
    // }
    // } else {
    // // Adicionar à lista de variáveis processadas para o relatório
    // VarEntry constEntry = varTable.lookup(id);
    // if (constEntry != null) {
    // allProcessedVariables.add(constEntry);
    // }

    // // Adicionar também à tabela de tipos para referência
    // typeTable.addVariable(id, constType);

    // // Se for um array, adicionar à ArrayTable
    // processArrayDeclaration(id, typeSpec, lineNumber);
    // }
    // }
    // }

    // return null;
    // }

    @Override
    public AST visitVarSpecification(Go_Parser.VarSpecificationContext ctx) {
        System.out.println("DEBUG: VarSpec");

        AST varSpecNode = new AST(NodeKind.VAR_SPEC_NODE, GoType.NO_TYPE);

        // Extrair typeSpec
        String typeSpec = ctx.typeSpec().getText();
        System.out.println("DEBUG: typeSpec = " + typeSpec);

        // Extrair identifierLIst
        String[] identifiers = ctx.identifierList().getText().split(",");
        for (String id : identifiers) {
            System.out.println("DEBUG: identifier = " + id.trim());
        }

        // Extrair número da linha
        int lineNumber = ctx.start.getLine();
        System.out.println("DEBUG: lineNumber = " + lineNumber);

        // Processar cada variável
        for (String id : identifiers) {
            if (id != null && !id.isEmpty()) {
                GoType varType = GoType.fromString(typeSpec);

                // Criar nó AST para o identificador
                AST idNode = AST.id(id, lineNumber, 0);
                idNode.setAnnotatedType(varType);
                varSpecNode.addChild(idNode);

                // // Criar nó AST para o tipo
                // AST typeNode = AST.id(typeSpec, lineNumber, 0);
                // typeNode.setAnnotatedType(varType);
                // varSpecNode.addChild(typeNode);

                // varSpecNode.addChild(idNode);

                // Tentar adicionar a variável à tabela
                if (!varTable.addVariable(id, varType, lineNumber)) {
                    VarEntry existing = varTable.lookup(id);
                    if (existing != null && varTable.existsInCurrentScope(id)) {
                        reportSemanticError(
                                "variable '" + id + "' already declared at line " + existing.getDeclarationLine());
                    }
                } else {
                    // Adicionar à lista de variáveis processadas para o relatório
                    VarEntry varEntry = varTable.lookup(id);
                    if (varEntry != null) {
                        allProcessedVariables.add(varEntry);
                    }

                    // Adicionar também à tabela de tipos para referência
                    typeTable.addVariable(id, varType);

                    // Se for um array, adicionar à ArrayTable
                    processArrayDeclaration(id, typeSpec, lineNumber);
                }
            }
        }

        // Se há expressionList, processar as expressões
        if (ctx.expressionList() != null) {
            AST exprListNode = new AST(NodeKind.EXPR_LIST_NODE, GoType.NO_TYPE);
            varSpecNode.addChild(exprListNode);

            // Extrair expressões individuais usando o parser ANTLR
            Go_Parser.ExprListContext exprListContext = (Go_Parser.ExprListContext) ctx.expressionList();
            List<Go_Parser.ExprContext> expressions = exprListContext.expr();

            for (int i = 0; i < expressions.size() && i < identifiers.length; i++) {
                String expr = expressions.get(i).getText().trim();
                String varName = identifiers[i].trim();
                GoType declaredType = GoType.fromString(typeSpec);

                System.out.println(
                        "DEBUG: Validating assignment: " + varName + " (" + declaredType.getTypeName() + ") = " + expr);

                // Inferir tipo da expressão de inicialização
                GoType exprType = inferArgumentType(expr);

                // Verificar compatibilidade de tipos
                if (!areTypesCompatible(exprType, declaredType)) {
                    reportSemanticError("cannot assign " + exprType.getTypeName() +
                            " to variable '" + varName + "' of type " + declaredType.getTypeName());
                }
            }

            // Processar as expressões usando visitadores ANTLR (para validações mais
            // profundas)
            for (Go_Parser.ExprContext exprCtx : expressions) {
                AST exprNode = visit(exprCtx);
                if (exprNode != null) {
                    exprListNode.addChild(exprNode);
                }
            }
            // AST exprNode = visit(ctx.expressionList());
            // if (exprNode != null) {
            //     exprListNode.addChild(exprNode);
            // }
        }

        return varSpecNode;
    }

    // // --- DECLARAÇÕES CURTAS ---

    // // Não está inferindo o tipo da expressão do lado direito, apenas processando
    // // como "unknown"
    // @Override
    // public Void visitShortVariableDecl(Go_Parser.ShortVariableDeclContext ctx) {
    // System.out.println("DEBUG: ShortVariableDecl");

    // // Extrair identifierList
    // String[] identifiers = ctx.identifierList().getText().split(",");
    // for (String id : identifiers) {
    // System.out.println("DEBUG: identifier = " + id.trim());
    // }

    // // Extrair número da linha
    // int lineNumber = ctx.start.getLine();
    // System.out.println("DEBUG: lineNumber = " + lineNumber);

    // // Extrair expressões individuais
    // Go_Parser.ExprListContext exprListContext = (Go_Parser.ExprListContext)
    // ctx.expressionList();
    // List<Go_Parser.ExprContext> expressions = exprListContext.expr();

    // List<String> exprTypes = new ArrayList<>();

    // // Inferir tipo de cada expressão separadamente
    // for (Go_Parser.ExprContext exprCtx : expressions) {
    // String exprText = exprCtx.getText();
    // System.out.println("DEBUG: Individual expression = " + exprText);

    // String inferredType = inferExpressionType(exprText);
    // exprTypes.add(inferredType);
    // }

    // System.out.println("DEBUG: ExprTypes: " + exprTypes);

    // // Processar cada variável
    // for (int i = 0; i < identifiers.length; i++) {
    // String id = identifiers[i].trim();
    // String inferredType = (i < exprTypes.size()) ? exprTypes.get(i) : "unknown";

    // if (id != null && !id.isEmpty()) {
    // GoType varType = GoType.fromString(inferredType);
    // System.out.println("DEBUG: Adding short variable: " + id + " of type " +
    // varType.getTypeName());

    // // Tentar adicionar a variável à tabela
    // if (!varTable.addVariable(id, varType, lineNumber)) {
    // VarEntry existing = varTable.lookup(id);
    // if (existing != null && varTable.existsInCurrentScope(id)) {
    // reportSemanticError(
    // "variable '" + id + "' already declared at line " +
    // existing.getDeclarationLine());
    // }
    // } else {
    // // Adicionar à lista de variáveis processadas para o relatório
    // VarEntry varEntry = varTable.lookup(id);
    // if (varEntry != null) {
    // allProcessedVariables.add(varEntry);
    // }

    // // Adicionar também à tabela de tipos para referência
    // typeTable.addVariable(id, varType);

    // // Se for um array, adicionar à ArrayTable
    // processArrayDeclaration(id, inferredType, lineNumber);
    // }
    // }
    // }

    // return null;
    // }

    // /**
    // * Infere tipo de uma expressão individual
    // */
    // private String inferExpressionType(String exprText) {
    // if (exprText == null || exprText.trim().isEmpty()) {
    // return "unknown";
    // }

    // exprText = exprText.trim();

    // // Array/slice literals
    // if (exprText.matches("\\[\\]\\w+\\{.*\\}")) {
    // if (exprText.startsWith("[]int{"))
    // return "[]int";
    // if (exprText.startsWith("[]string{"))
    // return "[]string";
    // if (exprText.startsWith("[]bool{"))
    // return "[]bool";
    // if (exprText.startsWith("[]float"))
    // return "[]float64";
    // // Adicionar outros tipos conforme necessário
    // }

    // // Usar inferArgumentType existente para outros casos
    // GoType type = inferArgumentType(exprText);
    // return type.getTypeName();
    // }

    // /**
    // * Processa composite literals (arrays/slices)
    // */
    // @Override
    // public Void visitCompositeLiteralExpr(Go_Parser.CompositeLiteralExprContext
    // ctx) {
    // System.out.println("DEBUG: CompositeLiteralExpr");
    // // Processar elementos do array/slice
    // return super.visitCompositeLiteralExpr(ctx);
    // }

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

    // /**
    // * Converte string de tipo para GoType
    // */
    // private GoType convertStringToGoType(String typeName) {
    // if (typeName == null || typeName.equals("unknown")) {
    // return GoType.UNKNOWN;
    // }

    // // Usar o método fromString do GoType
    // GoType type = GoType.fromString(typeName);
    // return type != null ? type : GoType.UNKNOWN;
    // }

    // // --- FUNÇÕES ---

    // @Override
    // public Void visitFunctionDecl(Go_Parser.FunctionDeclContext ctx) {
    // // Verificar se há erro sintático (ID pode ser null)
    // if (ctx.ID() == null) {
    // reportSemanticError("invalid function declaration - missing function name");
    // return null;
    // }

    // String functionName = ctx.ID().getText();
    // currentFunctionName = functionName;

    // // Verificar se a função já foi declarada
    // if (functionTable.hasFunction(functionName)) {
    // reportSemanticError(
    // "function '" + functionName + "' already declared");
    // return null;
    // }

    // // Criar novo escopo para a função
    // varTable.enterScope();

    // // Extrair parâmetros da função
    // Go_Parser.FunctionSignatureContext signature =
    // (Go_Parser.FunctionSignatureContext) ctx.signature();
    // Go_Parser.ParametersDeclarationContext paramsDecl =
    // (Go_Parser.ParametersDeclarationContext) signature
    // .parameters();

    // // IDs dos parâmetros
    // List<TerminalNode> idNodes = paramsDecl.ID();
    // List<String> paramNames = new ArrayList<>();
    // for (TerminalNode idNode : idNodes) {
    // paramNames.add(idNode.getText());
    // }
    // System.out.println("DEBUG: ID Nodes = " + idNodes);

    // // Tipos dos parâmetros
    // List<GoType> paramTypeNames = new ArrayList<>();
    // for (Go_Parser.TypeSpecContext typeNode : paramsDecl.typeSpec()) {
    // paramTypeNames.add(convertStringToGoType(typeNode.getText()));
    // }
    // System.out.println("DEBUG: Parameter Types = " + paramTypeNames);

    // // Retorno da função
    // GoType returnType = GoType.VOID; // Default se não especificado
    // Go_Parser.ResultContext result = signature.result();
    // if (result != null) {
    // if (result instanceof Go_Parser.ResultSingleTypeContext) { // Tipo único de
    // retorno
    // returnType = convertStringToGoType(result.getText());
    // System.out.println("DEBUG: Return Type = " + returnType);

    // }
    // }
    // currentFunctionReturnType = returnType;
    // System.out.println("DEBUG: Return Type = " + returnType);

    // // Extrair número da linha da declaração da função
    // // Extrair número da linha
    // int lineNumber = ctx.start.getLine();
    // System.out.println("DEBUG: lineNumber = " + lineNumber);

    // // Adicionar parâmetros como variáveis locais no escopo da função
    // for (int i = 0; i < paramNames.size(); i++) {
    // String id = paramNames.get(i);
    // GoType type = paramTypeNames.get(i);

    // if (!varTable.addVariable(id, type, lineNumber)) {
    // reportSemanticError("parameter '" + id + "' already declared");
    // } else {
    // // Adicionar parâmetros à lista de variáveis processadas para o relatório
    // VarEntry paramEntry = varTable.lookup(id);
    // if (paramEntry != null) {
    // allProcessedVariables.add(paramEntry);
    // }

    // // Se for um array, adicionar também à ArrayTable
    // if (ArrayTable.isArrayType(type.getTypeName())) {
    // ArrayInfo arrayInfo = ArrayTable.parseArrayType(type.getTypeName());
    // if (arrayInfo != null) {
    // arrayTable.addArray(id, arrayInfo.getElementType(), arrayInfo.getSize(),
    // lineNumber);
    // }
    // }
    // }
    // }

    // // Adicionar função à tabela com tipo de retorno correto
    // if (!functionTable.addFunction(functionName, paramNames, paramTypeNames,
    // returnType, lineNumber)) {
    // reportSemanticError("Failed to add function '" + functionName + "'");
    // } else {
    // // Marcar como definida (já que tem corpo)
    // functionTable.markAsDefined(functionName);
    // }

    // // Processar corpo da função (delegar aos visitadores padrão)
    // super.visitFunctionDecl(ctx);

    // // Sair do escopo da função
    // varTable.exitScope();

    // // Limpar rastreamento da função atual
    // currentFunctionName = null;
    // currentFunctionReturnType = null;

    // return null;
    // }

    // @Override
    // public Void visitBlockCode(Go_Parser.BlockCodeContext ctx) {
    // varTable.enterScope(); // Entrar em novo escopo para blocos
    // super.visitBlockCode(ctx); // Processar conteúdo do bloco
    // varTable.exitScope(); // Sair do escopo
    // return null;
    // }

    // // --- LITERAIS ---
    // @Override
    // public Void visitIdExpr(Go_Parser.IdExprContext ctx) {
    // return super.visitIdExpr(ctx);
    // }

    // @Override
    // public Void visitIntLiteral(Go_Parser.IntLiteralContext ctx) {
    // return super.visitIntLiteral(ctx);
    // }

    // @Override
    // public Void visitFloatLiteral(Go_Parser.FloatLiteralContext ctx) {
    // return super.visitFloatLiteral(ctx);
    // }

    // @Override
    // public Void visitStringLiteral(Go_Parser.StringLiteralContext ctx) {
    // return super.visitStringLiteral(ctx);
    // }

    // @Override
    // public Void visitTrueLiteral(Go_Parser.TrueLiteralContext ctx) {
    // return super.visitTrueLiteral(ctx);
    // }

    // @Override
    // public Void visitFalseLiteral(Go_Parser.FalseLiteralContext ctx) {
    // return super.visitFalseLiteral(ctx);
    // }

    // @Override
    // public Void visitParenthesizedExpr(Go_Parser.ParenthesizedExprContext ctx) {
    // return super.visitParenthesizedExpr(ctx);
    // }

    // /**
    // * Processa chamadas de função
    // */
    // @Override
    // public Void visitCallExpression(Go_Parser.CallExpressionContext ctx) {
    // String functionName = ctx.ID().getText();

    // // Se a função não existe, verificar se é built-in e adicioná-la
    // if (!functionTable.hasFunction(functionName)) {
    // if (isBuiltInFunction(functionName)) {
    // functionTable.addBuiltInFunctionIfNeeded(functionName);
    // } else {
    // reportSemanticError(ctx, "undefined function '" + functionName + "'");
    // }
    // }

    // // Se agora a função existe, validar argumentos
    // if (functionTable.hasFunction(functionName)) {
    // // Extrair argumentos da chamada
    // List<String> arguments = extractCallArguments(ctx);

    // // Validar número de argumentos
    // FunctionInfo funcInfo = functionTable.getFunction(functionName);
    // if (funcInfo != null) {
    // List<GoType> expectedParamTypes = funcInfo.getParameterTypes();

    // if (arguments.size() != expectedParamTypes.size()) {
    // reportSemanticError(ctx,
    // "function '" + functionName + "' expects " + expectedParamTypes.size() +
    // " arguments, got " + arguments.size());
    // } else {

    // // Validar tipos dos argumentos (implementação básica)
    // for (int i = 0; i < arguments.size(); i++) {
    // String arg = arguments.get(i);
    // GoType expectedType = expectedParamTypes.get(i);
    // GoType argType = inferArgumentType(arg);

    // if (!areTypesCompatible(argType, expectedType)) {
    // reportSemanticError(ctx,
    // "argument " + (i + 1) + " to '" + functionName +
    // "': cannot convert " + argType.getTypeName() +
    // " to " + expectedType.getTypeName());
    // }
    // }
    // }
    // }
    // }

    // // Continuar processamento dos argumentos
    // super.visitCallExpression(ctx);
    // return null;
    // }

    // /**
    // * Verifica se uma função é built-in do Go
    // */
    // private boolean isBuiltInFunction(String functionName) {
    // return "println".equals(functionName) || "len".equals(functionName);
    // }

    // /**
    // * Extrai argumentos de uma chamada de função usando parsing simples
    // */
    // private List<String> extractCallArguments(Go_Parser.CallExpressionContext
    // ctx) {
    // List<String> arguments = new ArrayList<>();

    // if (ctx != null && ctx.expressionList() != null) {
    // String argsText = ctx.expressionList().getText();
    // if (argsText != null && !argsText.isEmpty()) {
    // // Parsing simples por vírgulas
    // String[] args = argsText.split(",");
    // for (String arg : args) {
    // arguments.add(arg.trim());
    // }
    // }
    // }

    // return arguments;
    // }

    // /**
    // * Infere o tipo de um argumento baseado em sua representação textual
    // */
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

    // /**
    // * Analisa expressões aritméticas simples (e.g., x+y, a-b)
    // * Focado apenas nos elementos mínimos do projeto
    // */
    private GoType analyzeSimpleExpression(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return GoType.UNKNOWN;
        }

        expr = expr.trim();

        // Operações básicas requeridas pelo projeto (conforme README)
        String[] arithmeticOps = { "+", "-", "*", "/" };
        String[] comparisonOps = { "==", "!=", "<", ">", "<=", ">=" };

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

    // /**
    // * Infere tipo de operando simples (literal ou variável)
    // * Evita recursão infinita ao analisar expressões
    // */
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

    // /**
    // * Verifica se dois tipos são compatíveis
    // * Utiliza a lógica de compatibilidade implementada na classe GoType
    // */
    private boolean areTypesCompatible(GoType actual, GoType expected) {
        if (actual == null || expected == null) {
            return false;
        }
        // Delegar para a lógica de compatibilidade da classe GoType
        return actual.isCompatibleWith(expected);
    }

    // /**
    // * Processa statements de return
    // */
    // @Override
    // public Void visitReturnStatement(Go_Parser.ReturnStatementContext ctx) {
    // System.out.println("DEBUG: Return Statement");
    // if (currentFunctionName == null || currentFunctionReturnType == null) {
    // reportSemanticError("return statement outside of function");
    // return null;
    // }
    // String returnExpr = null;
    // if (ctx.expr() != null) {
    // returnExpr = ctx.expr().getText();
    // }
    // System.out.println("DEBUG: Return Expression = " + returnExpr);

    // if (returnExpr == null || returnExpr.trim().isEmpty()) {
    // // Return sem expressão
    // if (currentFunctionReturnType != GoType.VOID) {
    // reportSemanticError(
    // "function '" + currentFunctionName + "' expects return type " +
    // currentFunctionReturnType.getTypeName() + ", but got void");
    // }
    // } else {
    // // Return com expressão - verificar tipo
    // GoType returnType = inferArgumentType(returnExpr.trim());

    // if (!areTypesCompatible(returnType, currentFunctionReturnType)) {
    // reportSemanticError(
    // "function '" + currentFunctionName + "' expects return type " +
    // currentFunctionReturnType.getTypeName() + ", but got " +
    // returnType.getTypeName());
    // }
    // }

    // // Continuar processamento
    // super.visitReturnStatement(ctx);
    // return null;
    // }

    // // --- STATEMENTS DE CONTROLE ---

    // /**
    // * Processa assignments simples (x = y)
    // */
    // @Override
    // public Void visitSimpleAssignStatement(Go_Parser.SimpleAssignStatementContext
    // ctx) {
    // if (ctx.lvalue() == null || ctx.expr() == null) {
    // return super.visitSimpleAssignStatement(ctx);
    // }

    // // Extrair informações usando getText() e parsing simples
    // String fullLvalue = ctx.lvalue().getText();
    // String rvalue = ctx.expr().getText();
    // String varName = null;
    // boolean isArrayAccess = false;
    // String indexExpr = null;

    // // Verificar se é acesso a array (formato: var[index])
    // if (fullLvalue.contains("[") && fullLvalue.contains("]")) {
    // int bracketIndex = fullLvalue.indexOf("[");
    // int closeBracketIndex = fullLvalue.indexOf("]");
    // varName = fullLvalue.substring(0, bracketIndex);
    // indexExpr = fullLvalue.substring(bracketIndex + 1, closeBracketIndex);
    // isArrayAccess = true;
    // } else {
    // varName = fullLvalue;
    // }

    // if (varName == null || varName.trim().isEmpty()) {
    // return super.visitSimpleAssignStatement(ctx);
    // }

    // // Verificar se a variável existe na VarTable
    // if (varTable.exists(varName)) {
    // VarEntry varEntry = varTable.lookup(varName);
    // if (varEntry != null) {
    // // Verificar se é uma constante
    // if (varEntry.isConstant()) {
    // reportSemanticError(ctx, "cannot assign to constant '" + varName + "'");
    // return super.visitSimpleAssignStatement(ctx);
    // }

    // GoType lvalueType = varEntry.getType();

    // // Se é acesso a array, validar e obter tipo do elemento
    // if (isArrayAccess) {
    // if (arrayTable.hasArray(varName)) {
    // ArrayInfo arrayInfo = arrayTable.getArray(varName);
    // lvalueType = arrayInfo.getElementType();
    // } else if (lvalueType.isArray()) {
    // lvalueType = lvalueType.getElementType();
    // } else {
    // reportSemanticError(ctx, "'" + varName + "' is not an array");
    // return super.visitSimpleAssignStatement(ctx);
    // }

    // // Validar tipo do índice
    // if (indexExpr != null) {
    // GoType indexType = inferArgumentType(indexExpr);
    // if (indexType != GoType.INT && indexType != GoType.UNKNOWN) {
    // reportSemanticError(ctx, "array index must be integer, got " +
    // indexType.getTypeName());
    // }
    // }
    // }

    // // Verificar compatibilidade de tipos
    // GoType rvalueType = inferArgumentType(rvalue);
    // if (!areTypesCompatible(rvalueType, lvalueType)) {
    // reportSemanticError(ctx,
    // "cannot assign " + rvalueType.getTypeName() +
    // " to variable '" + varName + "' of type " + lvalueType.getTypeName());
    // }
    // }
    // }
    // // Verificar se é array puro na ArrayTable
    // else if (arrayTable.hasArray(varName)) {
    // if (!isArrayAccess) {
    // reportSemanticError(ctx, "cannot assign to array '" + varName + "' without
    // index");
    // return super.visitSimpleAssignStatement(ctx);
    // }

    // ArrayInfo arrayInfo = arrayTable.getArray(varName);
    // GoType elementType = arrayInfo.getElementType();

    // // Validar tipo do índice
    // if (indexExpr != null) {
    // GoType indexType = inferArgumentType(indexExpr);
    // if (indexType != GoType.INT && indexType != GoType.UNKNOWN) {
    // reportSemanticError(ctx, "array index must be integer, got " +
    // indexType.getTypeName());
    // }
    // }

    // // Verificar compatibilidade de tipos
    // GoType rvalueType = inferArgumentType(rvalue);
    // if (!areTypesCompatible(rvalueType, elementType)) {
    // reportSemanticError(ctx,
    // "cannot assign " + rvalueType.getTypeName() +
    // " to array element of type " + elementType.getTypeName());
    // }
    // }
    // // Variável não encontrada
    // else {
    // reportSemanticError(ctx, "undefined variable '" + varName + "'");
    // }

    // return super.visitSimpleAssignStatement(ctx);
    // }

    // /**
    // * Processa statements if-else
    // */
    // @Override
    // public Void visitIfElseStatement(Go_Parser.IfElseStatementContext ctx) {
    // System.out.println("DEBUG: IfElseStatement");
    // // Extrair e validar condição do if
    // String condition = ctx.expr().getText();
    // System.out.println("DEBUG: If condition = " + condition);

    // if (condition != null && !condition.trim().isEmpty()) {
    // GoType conditionType = inferArgumentType(condition);
    // System.out.println("DEBUG: If condition (inferred) = " +
    // conditionType.getTypeName());
    // // Em Go, condições devem ser do tipo bool
    // if (conditionType != GoType.BOOL && conditionType != GoType.UNKNOWN) {
    // reportSemanticError(ctx,
    // "if condition must be boolean, got " + conditionType.getTypeName());
    // }
    // }
    // // Continuar processamento automático dos blocos
    // super.visitIfElseStatement(ctx);

    // return null;
    // }

    // @Override
    // public Void visitForClauseRule(Go_Parser.ForClauseRuleContext ctx) {
    // System.out.println("DEBUG: ForClause");
    // // Primeira cláusula (inicialização)
    // if (ctx.simpleStmt().size() >= 1 && ctx.simpleStmt(0) != null) {
    // // Processar a primeira cláusula (inicialização)
    // String initStmt = ctx.simpleStmt(0).getText();
    // System.out.println("DEBUG: For init statement = " + initStmt);
    // visit(ctx.simpleStmt(0));
    // }

    // // Segunda cláusula (condição)
    // if (ctx.expr() != null) {
    // // Processar expressão de condição
    // String conditionExpr = ctx.expr().getText();
    // System.out.println("DEBUG: For condition expression = " + conditionExpr);

    // GoType conditionType = inferArgumentType(conditionExpr);
    // if (conditionType != GoType.BOOL && conditionType != GoType.UNKNOWN) {
    // reportSemanticError("for condition must be boolean, got " +
    // conditionType.getTypeName());
    // }

    // visit(ctx.expr());
    // }

    // // Terceira cláusula (pós)
    // if (ctx.simpleStmt().size() >= 2 && ctx.simpleStmt(1) != null) {
    // // Processar a segunda cláusula (condição)
    // String conditionStmt = ctx.simpleStmt(1).getText();
    // System.out.println("DEBUG: For post statement = " + conditionStmt);

    // if (conditionStmt.contains("++") || conditionStmt.contains("--")) {
    // String varName = conditionStmt.substring(0, conditionStmt.length() -
    // 2).trim();
    // System.out.println("DEBUG: For post variable = " + varName);
    // if (varName != null && !varTable.exists(varName)) {
    // reportSemanticError("undefined variable '" + varName + "' in for post
    // statement");
    // }
    // }

    // visit(ctx.simpleStmt(1));
    // }
    // return null;
    // }

    // /**
    // * Processa statements for (loops)
    // */
    // @Override
    // public Void visitForLoopStatement(Go_Parser.ForLoopStatementContext ctx) {
    // System.out.println("DEBUG: ForLoopStatement");
    // // Entrar no loop (incrementar profundidade)
    // loopDepth++;
    // varTable.enterScope(); // Entrar em novo escopo para o loop

    // if (ctx.forClause() != null) {
    // super.visit(ctx.forClause()); // Visitar cláusula do for
    // } else if (ctx.expr() != null) {
    // System.out.println("DEBUG: For condition expression = " +
    // ctx.expr().getText());
    // GoType conditionType = inferArgumentType(ctx.expr().getText());
    // if (conditionType != GoType.BOOL && conditionType != GoType.UNKNOWN) {
    // reportSemanticError("for condition must be boolean, got " +
    // conditionType.getTypeName());
    // }
    // } else {
    // System.out.println("DEBUG: For loop without condition");
    // }

    // if (ctx.block() != null) {
    // super.visit(ctx.block()); // Visitar corpo do loop
    // }

    // varTable.exitScope(); // Sair do escopo do loop
    // loopDepth--;

    // return null;
    // }

    // /**
    // * Processa acesso a arrays (arr[index])
    // */
    // @Override
    // public Void visitArrayAccessExpr(Go_Parser.ArrayAccessExprContext ctx) {
    // // Extrair nome do array e índice
    // String arrayName = null;
    // String indexExpr = null;

    // if (ctx.expr().size() >= 2) {
    // arrayName = ctx.expr(0).getText(); // Primeiro expr é o array
    // indexExpr = ctx.expr(1).getText(); // Segundo expr é o índice
    // }

    // System.out.println("DEBUG: Array access - name: " + arrayName + ", index: " +
    // indexExpr);

    // // Verificar se o array existe
    // if (arrayName != null && !arrayName.trim().isEmpty()) {
    // // Verificar na VarTable primeiro (onde arrays são declarados)
    // if (varTable.exists(arrayName)) {
    // VarEntry arrayEntry = varTable.lookup(arrayName);
    // if (arrayEntry != null) {
    // GoType arrayType = arrayEntry.getType();
    // System.out.println("DEBUG: Found variable '" + arrayName + "' of type: " +
    // arrayType.getTypeName());

    // // Verificar se é realmente um array usando string pattern
    // if (arrayType.getTypeName().startsWith("[]")) { // ✅ Fix: usar string pattern
    // System.out.println("DEBUG: '" + arrayName + "' is confirmed as array");

    // // Verificar se o índice é do tipo int
    // if (indexExpr != null && !indexExpr.trim().isEmpty()) {
    // GoType indexType = inferArgumentType(indexExpr);
    // if (indexType != GoType.INT && indexType != GoType.UNKNOWN) {
    // reportSemanticError("array index must be integer, got " +
    // indexType.getTypeName());
    // }
    // }
    // } else {
    // reportSemanticError("'" + arrayName + "' is not an array (type: " +
    // arrayType.getTypeName() + ")");
    // }
    // }
    // } else {
    // reportSemanticError("undefined variable '" + arrayName + "'");
    // }
    // }

    // return super.visitArrayAccessExpr(ctx);
    // }

    // /**
    // * Processa statements break
    // */
    // @Override
    // public Void visitBreakStatementRule(Go_Parser.BreakStatementRuleContext ctx)
    // {
    // // Verificar se estamos dentro de um loop
    // if (loopDepth == 0) {
    // reportSemanticError("break statement not in loop");
    // }
    // return null;
    // }

    // /**
    // * Processa statements continue
    // */
    // @Override
    // public Void visitContinueStatementRule(Go_Parser.ContinueStatementRuleContext
    // ctx) {
    // // Verificar se estamos dentro de um loop
    // if (loopDepth == 0) {
    // reportSemanticError("continue statement not in loop");
    // }

    // return null;
    // }

    // // --- STATEMENTS SIMPLES ---

    // @Override
    // public Void visitExpressionSimpleStmt(Go_Parser.ExpressionSimpleStmtContext
    // ctx) {
    // super.visitExpressionSimpleStmt(ctx);
    // return null;
    // }

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

    // Exibe a AST no formato DOT em stderr.
    public void printAST() {
        if (root == null) {
            System.err.println("AST not built - no tree to print");
            return;
        }
        AST.printDot(root, this.varTable);
    }
}
