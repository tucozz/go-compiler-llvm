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
    public AST visitConstDecl(Go_Parser.ConstDeclContext ctx) {
        System.out.println("DEBUG: ConstDecl Principal");
        AST constDeclNode = new AST(NodeKind.CONST_DECL_NODE, GoType.NO_TYPE);

        // Visita explicitamente cada 'constSpec' e adiciona sua subárvore
        if (ctx.constSpec() != null) {
            for (Go_Parser.ConstSpecContext specCtx : ctx.constSpec()) {
                constDeclNode.addChild(visit(specCtx));
            }
        }
        
        return constDeclNode;
    }

    @Override
    public AST visitVarDecl(Go_Parser.VarDeclContext ctx) {
        System.out.println("DEBUG: VarDecl Principal");
        AST varDeclNode = new AST(NodeKind.VAR_DECL_NODE, GoType.NO_TYPE);
         if (ctx.varSpec() != null) {
            for (Go_Parser.VarSpecContext specCtx : ctx.varSpec()) {
                varDeclNode.addChild(visit(specCtx));
            }
        }
        return varDeclNode;
    }

    // // --- ESPECIFICAÇÕES ---

    // // Regra identifierList (typeSpec)? ASSIGN expressionList
    @Override
    public AST visitConstSpecification(Go_Parser.ConstSpecificationContext ctx) {
        System.out.println("DEBUG: ConstSpec");
        AST constSpecNode = new AST(NodeKind.CONST_SPEC_NODE, GoType.NO_TYPE);
        int lineNumber = ctx.start.getLine();

        // Extrair identificadores
        String[] identifiers = ctx.identifierList().getText().split(",");

        // Extrair tipo (opcional para constantes, mas vamos processá-lo se existir)
        GoType constType = GoType.UNKNOWN; // Tipo padrão se não especificado
        if (ctx.typeSpec() != null) {
            constType = GoType.fromString(ctx.typeSpec().getText());
        }

        // Adicionar constantes à tabela de símbolos e nós à AST
        for (String id : identifiers) {
            id = id.trim();
            if (!id.isEmpty()) {
                // Tenta adicionar a constante à tabela
                if (!varTable.addConstant(id, constType, lineNumber)) {
                    VarEntry existing = varTable.lookup(id);
                    if (existing != null && varTable.existsInCurrentScope(id)) {
                        reportSemanticError(ctx,
                                "constant '" + id + "' already declared at line " + existing.getDeclarationLine());
                    }
                }
                // Adiciona o nó do identificador à AST
                constSpecNode.addChild(AST.id(id, lineNumber, 0));
            }
        }
        
        // Processar a lista de expressões (valores da constante)
        if (ctx.expressionList() != null) {
            AST exprListNode = new AST(NodeKind.EXPR_LIST_NODE, GoType.NO_TYPE);
            constSpecNode.addChild(exprListNode);
            
            for (Go_Parser.ExprContext exprCtx : ((Go_Parser.ExprListContext)ctx.expressionList()).expr()) {
                exprListNode.addChild(visit(exprCtx));
            }
        }

        return constSpecNode;
    }

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

    // --- EXPRESSÕES ARITMÉTICAS ---

    @Override
    public AST visitAddSubExpr(Go_Parser.AddSubExprContext ctx) {
        AST left = visit(ctx.expr(0));
        AST right = visit(ctx.expr(1));

        String op = ctx.getChild(1).getText();
        NodeKind kind = op.equals("+") ? NodeKind.PLUS_NODE : NodeKind.MINUS_NODE;

        GoType leftType = left.getAnnotatedType();
        GoType rightType = right.getAnnotatedType();
        GoType resultType = typeTable.getBinaryOperationResultType(leftType, rightType, op);

        if (resultType == GoType.UNKNOWN) {
            reportSemanticError(ctx, "invalid operation: " + leftType.getTypeName() + " " + op + " " + rightType.getTypeName());
        }

        AST node = AST.binaryOp(kind, left, right, ctx.start.getLine(), 0);
        node.setAnnotatedType(resultType);
        
        return node;
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
        
        GoType leftType = left.getAnnotatedType();
        GoType rightType = right.getAnnotatedType();
        GoType resultType = typeTable.getBinaryOperationResultType(leftType, rightType, op);
        
        if (resultType == GoType.UNKNOWN) {
            reportSemanticError(ctx, "invalid operation: " + leftType.getTypeName() + " " + op + " " + rightType.getTypeName());
        }

        AST node = AST.binaryOp(kind, left, right, ctx.start.getLine(), 0);
        node.setAnnotatedType(resultType);
        
        return node;
    }

    // --- EXPRESSÕES LÓGICAS E DE COMPARAÇÃO ---

    @Override
    public AST visitLogicalANDExpr(Go_Parser.LogicalANDExprContext ctx) {
        AST left = visit(ctx.expr(0));
        AST right = visit(ctx.expr(1));

        // Verificação semântica: operandos de '&&' devem ser booleanos.
        GoType leftType = left.getAnnotatedType();
        GoType rightType = right.getAnnotatedType();
        if (leftType != GoType.BOOL || rightType != GoType.BOOL) {
            reportSemanticError(ctx, "invalid operation: non-boolean arguments to && (" 
                + leftType.getTypeName() + " and " + rightType.getTypeName() + ")");
        }

        // O resultado de uma operação lógica é sempre booleano.
        AST node = AST.binaryOp(NodeKind.AND_NODE, left, right, ctx.start.getLine(), 0);
        node.setAnnotatedType(GoType.BOOL);
        return node;
    }

    @Override
    public AST visitLogicalORExpr(Go_Parser.LogicalORExprContext ctx) {
        AST left = visit(ctx.expr(0));
        AST right = visit(ctx.expr(1));

        // Verificação semântica: operandos de '||' devem ser booleanos.
        GoType leftType = left.getAnnotatedType();
        GoType rightType = right.getAnnotatedType();
        if (leftType != GoType.BOOL || rightType != GoType.BOOL) {
            reportSemanticError(ctx, "invalid operation: non-boolean arguments to || (" 
                + leftType.getTypeName() + " and " + rightType.getTypeName() + ")");
        }
        
        // O resultado de uma operação lógica é sempre booleano.
        AST node = AST.binaryOp(NodeKind.OR_NODE, left, right, ctx.start.getLine(), 0);
        node.setAnnotatedType(GoType.BOOL);
        return node;
    }

    @Override
    public AST visitComparisonExpr(Go_Parser.ComparisonExprContext ctx) {
        AST left = visit(ctx.expr(0));
        AST right = visit(ctx.expr(1));
        
        // Verificar se algum dos operandos é null
        if (left == null) {
            reportSemanticError(ctx, "left operand of comparison expression is null");
            return null;
        }
        if (right == null) {
            reportSemanticError(ctx, "right operand of comparison expression is null");
            return null;
        }
        
        String op = ctx.relation_op().getText();

        // Verificação de tipo: os tipos devem ser comparáveis.
        GoType leftType = left.getAnnotatedType();
        GoType rightType = right.getAnnotatedType();
        
        // Usamos a typeTable para verificar se a operação é válida
        GoType resultType = typeTable.getBinaryOperationResultType(leftType, rightType, op);
        if (resultType == GoType.UNKNOWN) {
             reportSemanticError(ctx, "invalid operation: cannot compare " 
                + leftType.getTypeName() + " and " + rightType.getTypeName());
        }

        // Mapeia o operador para o NodeKind correspondente
        NodeKind kind;
        switch(op) {
            case "==": kind = NodeKind.EQUAL_NODE; break;
            case "!=": kind = NodeKind.NOT_EQUAL_NODE; break;
            case "<":  kind = NodeKind.LESS_NODE; break;
            case "<=": kind = NodeKind.LESS_EQ_NODE; break;
            case ">":  kind = NodeKind.GREATER_NODE; break;
            case ">=": kind = NodeKind.GREATER_EQ_NODE; break;
            default:
                // Não deve acontecer
                throw new RuntimeException("Operador de comparação desconhecido: " + op);
        }

        // O resultado de uma comparação é sempre booleano.
        AST node = AST.binaryOp(kind, left, right, ctx.start.getLine(), 0);
        node.setAnnotatedType(GoType.BOOL);
        return node;
    }
    
    @Override
    public AST visitUnaryPrefixExpr(Go_Parser.UnaryPrefixExprContext ctx) {
        AST operand = visit(ctx.expr());
        String op = ctx.getChild(0).getText();

        switch(op) {
            case "!":
                if (operand.getAnnotatedType() != GoType.BOOL) {
                    reportSemanticError(ctx, "invalid operation: argument to ! is not boolean");
                }
                AST notNode = AST.unaryOp(NodeKind.NOT_NODE, operand, ctx.start.getLine(), 0);
                notNode.setAnnotatedType(GoType.BOOL);
                return notNode;

            case "+":
                 // O '+' unário não muda o valor ou tipo.
                 // Apenas retornamos o nó do operando.
                return operand;

            case "-":
                // O '-' unário deve ser aplicado a tipos numéricos.
                GoType type = operand.getAnnotatedType();
                if (!type.isNumeric()) {
                     reportSemanticError(ctx, "invalid operation: unary - applied to non-numeric type " + type.getTypeName());
                }
                AST minusNode = AST.unaryOp(NodeKind.UNARY_MINUS_NODE, operand, ctx.start.getLine(), 0);
                minusNode.setAnnotatedType(type); // Tipo resultante é o mesmo do operando.
                return minusNode;
        }

        return operand; // Fallback, não deve ser alcançado.
    }

    // // --- DECLARAÇÕES CURTAS ---

    // // Não está inferindo o tipo da expressão do lado direito, apenas processando
    // // como "unknown"
        @Override
    public AST visitShortVariableDecl(Go_Parser.ShortVariableDeclContext ctx) {
        System.out.println("DEBUG: ShortVariableDecl");
        
        AST shortDeclNode = AST.newSubtree(NodeKind.SHORT_VAR_DECL_NODE, GoType.NO_TYPE);
        int lineNumber = ctx.start.getLine();

        // Pega as listas de identificadores e expressões
        Go_Parser.IdentifierListContext idListCtx = ctx.identifierList();
        Go_Parser.ExprListContext exprListCtx = (Go_Parser.ExprListContext) ctx.expressionList();

        // --- CORREÇÃO APLICADA AQUI ---
        // Acessa os identificadores e expressões da forma correta
        String[] identifiers = idListCtx.getText().split(",");
        java.util.List<Go_Parser.ExprContext> expressions = exprListCtx.expr();

        // Validação semântica: verifica se o número de variáveis e valores é o mesmo
        if (identifiers.length != expressions.size()) {
            reportSemanticError(ctx, "assignment mismatch: " + identifiers.length + " variables but " + expressions.size() + " values");
            return shortDeclNode; // Retorna o nó (mesmo que incompleto) para não quebrar a AST
        }

        AST idListNode = new AST(NodeKind.IDENTIFIER_LIST_NODE, GoType.NO_TYPE);
        shortDeclNode.addChild(idListNode);

        AST exprListNode = new AST(NodeKind.EXPR_LIST_NODE, GoType.NO_TYPE);
        shortDeclNode.addChild(exprListNode);

        // Processa cada par de identificador e expressão usando um índice
        for (int i = 0; i < identifiers.length; i++) {
            String id = identifiers[i].trim(); // Limpa espaços em branco
            Go_Parser.ExprContext exprCtx = expressions.get(i);

            AST exprNode = visit(exprCtx);
            if (exprNode == null) {
                // Se o visitador retornou null, criar um nó de erro
                exprNode = new AST(NodeKind.ID_NODE, GoType.UNKNOWN);
                System.err.println("WARNING: Expression visitor returned null for: " + exprCtx.getText());
            }
            exprListNode.addChild(exprNode);

            GoType varType = exprNode.getAnnotatedType();
            if (varType == null) { 
                varType = inferArgumentType(exprCtx.getText());
            }

            System.out.println("DEBUG: Adding short variable: " + id + " of inferred type " + varType.getTypeName());
            
            if (!varTable.addVariable(id, varType, lineNumber)) {
                VarEntry existing = varTable.lookup(id);
                if (existing != null && varTable.existsInCurrentScope(id)) {
                    reportSemanticError(
                            "variable '" + id + "' already declared at line " + existing.getDeclarationLine());
                }
            } else {
                 VarEntry varEntry = varTable.lookup(id);
                 if (varEntry != null) {
                    allProcessedVariables.add(varEntry);
                 }
            }
            
            AST idNode = AST.id(id, lineNumber, 0);
            idNode.setAnnotatedType(varType);
            idListNode.addChild(idNode);
        }

        return shortDeclNode;
    }

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

    // // --- FUNÇÕES ---

    @Override
    public AST visitFunctionDecl(Go_Parser.FunctionDeclContext ctx) {
        // Verificar se há erro sintático (ID pode ser null)
        if (ctx.ID() == null) {
            reportSemanticError("invalid function declaration - missing function name");
            return new AST(NodeKind.FUNC_DECL_NODE, GoType.NO_TYPE);
        }

        String functionName = ctx.ID().getText();
        int lineNumber = ctx.start.getLine();
        currentFunctionName = functionName;

        // Verificar se a função já foi declarada
        if (functionTable.hasFunction(functionName)) {
            reportSemanticError(
                "function '" + functionName + "' already declared");
            return new AST(NodeKind.FUNC_DECL_NODE, GoType.NO_TYPE);
        }

        // Criar nó principal da função
        AST funcDeclNode = new AST(NodeKind.FUNC_DECL_NODE, GoType.NO_TYPE);

        // Adicionar o nome da função como primeiro filho
        AST nameNode = AST.id(functionName, lineNumber, 0);
        funcDeclNode.addChild(nameNode);

        // Criar novo escopo para a função
        varTable.enterScope();

        // Extrair parâmetros da função
        Go_Parser.FunctionSignatureContext signature = 
            (Go_Parser.FunctionSignatureContext) ctx.signature();
        Go_Parser.ParametersDeclarationContext paramsDecl = 
            (Go_Parser.ParametersDeclarationContext) signature.parameters();

        // Processar parâmetros
        AST paramListNode = new AST(NodeKind.PARAM_LIST_NODE, GoType.NO_TYPE);
        funcDeclNode.addChild(paramListNode);

        List<String> paramNames = new ArrayList<>();
        List<GoType> paramTypeNames = new ArrayList<>();

        if (paramsDecl != null) {
            // IDs dos parâmetros
            List<TerminalNode> idNodes = paramsDecl.ID();
            for (TerminalNode idNode : idNodes) {
                paramNames.add(idNode.getText());
            }
            System.out.println("DEBUG: ID Nodes = " + idNodes);

            // Tipos dos parâmetros
            for (Go_Parser.TypeSpecContext typeNode : paramsDecl.typeSpec()) {
                paramTypeNames.add(convertStringToGoType(typeNode.getText()));
            }
            System.out.println("DEBUG: Parameter Types = " + paramTypeNames);

            // Criar nós para cada parâmetro
            for (int i = 0; i < paramNames.size() && i < paramTypeNames.size(); i++) {
                String paramName = paramNames.get(i);
                GoType paramType = paramTypeNames.get(i);

                // Criar nó do parâmetro
                AST paramNode = new AST(NodeKind.PARAM_NODE, GoType.NO_TYPE);
                AST paramIdNode = AST.id(paramName, lineNumber, 0);
                paramIdNode.setAnnotatedType(paramType);
                paramNode.addChild(paramIdNode);
                paramListNode.addChild(paramNode);

                // Adicionar parâmetro à tabela de símbolos
                if (!varTable.addVariable(paramName, paramType, lineNumber)) {
                    reportSemanticError("parameter '" + paramName + "' already declared");
                } else {
                    // Adicionar parâmetros à lista de variáveis processadas para o relatório
                    VarEntry paramEntry = varTable.lookup(paramName);
                    if (paramEntry != null) {
                        allProcessedVariables.add(paramEntry);
                    }

                    // Se for um array, adicionar também à ArrayTable
                    if (ArrayTable.isArrayType(paramType.getTypeName())) {
                        ArrayInfo arrayInfo = ArrayTable.parseArrayType(paramType.getTypeName());
                        if (arrayInfo != null) {
                            arrayTable.addArray(paramName, arrayInfo.getElementType(), arrayInfo.getSize(), lineNumber);
                        }
                    }
                }
            }
        }

        // Processar tipo de retorno
        GoType returnType = GoType.VOID; // Default se não especificado
        Go_Parser.ResultContext result = signature.result();
        if (result != null) {
            if (result instanceof Go_Parser.ResultSingleTypeContext) { // Tipo único de retorno
                returnType = convertStringToGoType(result.getText());
                System.out.println("DEBUG: Return Type = " + returnType);
            }
        }
        currentFunctionReturnType = returnType;
        System.out.println("DEBUG: Return Type = " + returnType);

        // Adicionar nó do tipo de retorno
        AST resultNode = new AST(NodeKind.RESULT_NODE, returnType);
        funcDeclNode.addChild(resultNode);

        // Adicionar função à tabela com tipo de retorno correto
        if (!functionTable.addFunction(functionName, paramNames, paramTypeNames, returnType, lineNumber)) {
            reportSemanticError("Failed to add function '" + functionName + "'");
        } else {
            // Marcar como definida (já que tem corpo)
            functionTable.markAsDefined(functionName);
        }

        // Processar corpo da função
        AST bodyNode = visit(ctx.block());
        funcDeclNode.addChild(bodyNode);

        // Sair do escopo da função
        varTable.exitScope();

        // Limpar rastreamento da função atual
        currentFunctionName = null;
        currentFunctionReturnType = null;

        return funcDeclNode;
    }

    // // --- LITERAIS ---
    @Override
    public AST visitIdExpr(Go_Parser.IdExprContext ctx) {
        String idName = ctx.ID().getText();
        int line = ctx.start.getLine();
        
        // Semântica: Verificar se a variável foi declarada.
        if (!varTable.exists(idName)) {
            reportSemanticError(ctx, "undefined variable '" + idName + "'");
        }
        
        // AST: Cria um nó para o identificador usando a factory correta. 
        AST idNode = AST.id(idName, line, 0);

        // Anota o tipo no nó da AST para uso futuro.
        VarEntry entry = varTable.lookup(idName);
        if (entry != null) {
            idNode.setAnnotatedType(entry.getType());
        } else {
            idNode.setAnnotatedType(GoType.UNKNOWN);
        }
        
        return idNode;
    }

    @Override
    public AST visitIntLiteral(Go_Parser.IntLiteralContext ctx) {
        String text = ctx.getText();
        int line = ctx.start.getLine();
        int value = Integer.parseInt(text);
        
        AST node = AST.intLit(value, line, 0);
        node.setAnnotatedType(GoType.INT); // <-- ADICIONAR ESTA LINHA
        return node;
    }

    @Override
    public AST visitFloatLiteral(Go_Parser.FloatLiteralContext ctx) {
        String text = ctx.getText();
        int line = ctx.start.getLine();
        float value = Float.parseFloat(text);

        AST node = AST.realLit(value, line, 0);
        node.setAnnotatedType(GoType.FLOAT64); // <-- ADICIONAR ESTA LINHA
        return node;
    }

    @Override
    public AST visitStringLiteral(Go_Parser.StringLiteralContext ctx) {
        String textWithQuotes = ctx.getText();
        int line = ctx.start.getLine();
        stringTable.addString(textWithQuotes);
        String value = textWithQuotes.substring(1, textWithQuotes.length() - 1);
        
        AST node = AST.strLit(value, line, 0);
        node.setAnnotatedType(GoType.STRING); // <-- ADICIONAR ESTA LINHA
        return node;
    }

    @Override
    public AST visitTrueLiteral(Go_Parser.TrueLiteralContext ctx) {
        int line = ctx.start.getLine();
        AST node = AST.boolLit(true, line, 0);
        node.setAnnotatedType(GoType.BOOL); // <-- ADICIONAR ESTA LINHA
        return node;
    }

    @Override
    public AST visitFalseLiteral(Go_Parser.FalseLiteralContext ctx) {
        int line = ctx.start.getLine();
        AST node = AST.boolLit(false, line, 0);
        node.setAnnotatedType(GoType.BOOL); // <-- ADICIONAR ESTA LINHA
        return node;
    }

    @Override
    public AST visitParenthesizedExpr(Go_Parser.ParenthesizedExprContext ctx) {
        // Para expressões com parênteses, simplesmente visitamos a expressão interna.
        // Os parênteses definem a precedência na análise, mas não precisam de um nó próprio na AST.
        return visit(ctx.expr());
    }

    @Override
    public AST visitTypeCastExpr(Go_Parser.TypeCastExprContext ctx) {
        // Processar a conversão de tipo usando a regra typeCast
        return visit(ctx.typeCast());
    }

    @Override
    public AST visitTypeConversion(Go_Parser.TypeConversionContext ctx) {
        String typeName = ctx.typeSpec().getText();
        int lineNumber = ctx.start.getLine();
        
        System.out.println("DEBUG: visitTypeConversion - typeName = " + typeName);
        
        // Obter o tipo de destino
        GoType targetType = GoType.fromString(typeName);
        if (targetType == GoType.UNKNOWN) {
            reportSemanticError(ctx, "unknown type: " + typeName);
            return null;
        }
        
        // Processar a expressão a ser convertida
        AST sourceExpr = visit(ctx.expr());
        if (sourceExpr == null) {
            reportSemanticError(ctx, "invalid expression in type conversion");
            return null;
        }
        
        GoType sourceType = sourceExpr.getAnnotatedType();
        if (sourceType == null || sourceType == GoType.UNKNOWN) {
            reportSemanticError(ctx, "cannot convert expression of unknown type");
            return null;
        }
        
        // Verificar se a conversão é válida
        if (targetType.isNumeric() && sourceType.isNumeric()) {
            // Conversão entre tipos numéricos é permitida
            AST conversionNode = AST.newSubtree(NodeKind.TYPE_CONV_NODE, targetType, sourceExpr);
            conversionNode.setAnnotatedType(targetType);
            System.out.println("DEBUG: Created type conversion from " + sourceType.getTypeName() + " to " + targetType.getTypeName());
            return conversionNode;
        } else if (targetType == sourceType) {
            // Conversão para o mesmo tipo (permitida, mas desnecessária)
            return sourceExpr;
        } else {
            reportSemanticError(ctx, "cannot convert " + sourceType.getTypeName() + " to " + targetType.getTypeName());
            return null;
        }
    }

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

    /**
     * Processa chamadas de função
     */
    @Override
    public AST visitCallExpression(Go_Parser.CallExpressionContext ctx) {
        String functionName = ctx.ID().getText();
        int lineNumber = ctx.start.getLine();

        // Criar nó da função (identificador)
        AST functionNode = AST.id(functionName, lineNumber, 0);

        // Criar lista de argumentos
        List<AST> arguments = new ArrayList<>();
        
        // Se há argumentos, processá-los
        if (ctx.expressionList() != null) {
            Go_Parser.ExprListContext exprListContext = (Go_Parser.ExprListContext) ctx.expressionList();
            List<Go_Parser.ExprContext> expressions = exprListContext.expr();

            for (Go_Parser.ExprContext exprCtx : expressions) {
                AST argNode = visit(exprCtx);
                if (argNode != null) {
                    arguments.add(argNode);
                }
            }
        }

        // Se a função não existe, verificar se é built-in e adicioná-la
        if (!functionTable.hasFunction(functionName)) {
            if (isBuiltInFunction(functionName)) {
                functionTable.addBuiltInFunctionIfNeeded(functionName);
            } else {
                reportSemanticError(ctx, "undefined function '" + functionName + "'");
            }
        }

        // Se agora a função existe, validar argumentos
        GoType returnType = GoType.UNKNOWN;
        if (functionTable.hasFunction(functionName)) {
            // Validar número de argumentos
            FunctionInfo funcInfo = functionTable.getFunction(functionName);
            if (funcInfo != null) {
                List<GoType> expectedParamTypes = funcInfo.getParameterTypes();
                returnType = funcInfo.getReturnType();

                if (arguments.size() != expectedParamTypes.size()) {
                    reportSemanticError(ctx,
                        "function '" + functionName + "' expects " + expectedParamTypes.size() +
                        " arguments, got " + arguments.size());
                } else {
                    // Validar tipos dos argumentos
                    for (int i = 0; i < arguments.size(); i++) {
                        AST argNode = arguments.get(i);
                        GoType expectedType = expectedParamTypes.get(i);
                        GoType argType = argNode.getAnnotatedType();
                        
                        if (argType == null) {
                            argType = GoType.UNKNOWN;
                        }

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

        // Criar e retornar o nó da chamada de função
        AST callNode = AST.call(functionNode, arguments, lineNumber, 0);
        callNode.setAnnotatedType(returnType);
        return callNode;
    }

    /**
     * Verifica se uma função é built-in do Go
     */
    private boolean isBuiltInFunction(String functionName) {
        return "println".equals(functionName) || "len".equals(functionName);
    }



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

    /**
    * Processa statements de return
    */
    @Override
    public AST visitReturnStatement(Go_Parser.ReturnStatementContext ctx) {
        System.out.println("DEBUG: Return Statement");
        
        // Criar o nó principal para o statement de return
        AST returnNode = new AST(NodeKind.RETURN_NODE, GoType.NO_TYPE);
        int lineNumber = ctx.start.getLine();
        
        // Verificação semântica: return deve estar dentro de uma função
        if (currentFunctionName == null || currentFunctionReturnType == null) {
            reportSemanticError("return statement outside of function");
            return returnNode; // Retorna nó mesmo com erro para não quebrar a AST
        }
        
        // Processar a expressão de retorno (se existir)
        if (ctx.expr() != null) {
            // Visitar a expressão e adicionar como filho do nó return
            AST exprNode = visit(ctx.expr());
            returnNode.addChild(exprNode);
            
            // Verificação semântica: verificar compatibilidade de tipos
            GoType returnType = exprNode.getAnnotatedType();
            if (returnType == null) {
                returnType = inferArgumentType(ctx.expr().getText());
            }
            
            System.out.println("DEBUG: Return Expression Type = " + returnType.getTypeName());
            
            if (!areTypesCompatible(returnType, currentFunctionReturnType)) {
                reportSemanticError(
                    "function '" + currentFunctionName + "' expects return type " +
                    currentFunctionReturnType.getTypeName() + ", but got " +
                    returnType.getTypeName());
            }
            
            // Anotar o tipo da expressão no nó return
            returnNode.setAnnotatedType(returnType);
            
        } else {
            // Return sem expressão - verificar se a função deveria retornar void
            if (currentFunctionReturnType != GoType.VOID) {
                reportSemanticError(
                    "function '" + currentFunctionName + "' expects return type " +
                    currentFunctionReturnType.getTypeName() + ", but got void");
            }
            
            // Anotar como void
            returnNode.setAnnotatedType(GoType.VOID);
        }
        
        return returnNode;
    }

    // --- STATEMENTS DE CONTROLE ---

    // /**
    // * Processa assignments simples (x = y)
    // */
     @Override
    public AST visitSimpleAssignStatement(Go_Parser.SimpleAssignStatementContext ctx) {
        AST lvalueNode = visit(ctx.lvalue());
        AST rvalueNode = visit(ctx.expr());

        if (lvalueNode != null && rvalueNode != null) {
            String varName = lvalueNode.text;
            GoType lvalueType = lvalueNode.getAnnotatedType();
            GoType rvalueType = rvalueNode.getAnnotatedType();
            VarEntry varEntry = varTable.lookup(varName);

            if (varEntry == null) {
                reportSemanticError(ctx, "cannot assign to undeclared variable '" + varName + "'");
            } else {
                if (varEntry.isConstant()) {
                    reportSemanticError(ctx, "cannot assign to constant '" + varName + "'");
                }

                if (lvalueNode.kind == NodeKind.INDEX_NODE) {
                    // TODO: Implementar a lógica de verificação para atribuição em arrays aqui.
                    System.out.println("DEBUG: Array assignment detected but not yet fully implemented.");
                }

                if (!areTypesCompatible(rvalueType, lvalueType)) {
                    reportSemanticError(ctx, "cannot use " + rvalueType.getTypeName() + 
                                        " as type " + lvalueType.getTypeName() + " in assignment");
                }
            }
        }
        
        // --- Construção da AST ---
        // Cria o nó de atribuição com as subárvores da esquerda e direita como filhos.
        return AST.assign(lvalueNode, rvalueNode, ctx.start.getLine(), 0);
    }

    @Override
    public AST visitIdLvalue(Go_Parser.IdLvalueContext ctx) {
        String varName = ctx.ID().getText();
        int line = ctx.start.getLine();

        VarEntry entry = varTable.lookup(varName);
        if (entry == null) {
            reportSemanticError(ctx, "undeclared variable: " + varName);
            // Retorna um nó de erro para não quebrar o resto da análise.
            AST errorNode = AST.id(varName, line, 0);
            errorNode.setAnnotatedType(GoType.UNKNOWN);
            return errorNode;
        }

        // Cria o nó do identificador e anota seu tipo a partir da tabela de símbolos.
        AST idNode = AST.id(varName, line, 0);
        idNode.setAnnotatedType(entry.getType());
        return idNode;
    }

    @Override
    public AST visitIfElseStatement(Go_Parser.IfElseStatementContext ctx) {
        // Cria o nó principal para a estrutura if.
        AST ifNode = new AST(NodeKind.IF_NODE, GoType.NO_TYPE);

        // --- 1. Processa a Condição ---
        // Visita a expressão da condição para obter sua subárvore e tipo.
        AST conditionNode = visit(ctx.expr());
        
        // Verificar se a condição é null
        if (conditionNode == null) {
            reportSemanticError(ctx, "if condition expression is null");
            return null;
        }
        
        ifNode.addChild(conditionNode); // O primeiro filho é sempre a condição.

        // Verificação Semântica: A condição de um if deve ser booleana.
        GoType conditionType = conditionNode.getAnnotatedType();
        if (conditionType != GoType.BOOL) {
            reportSemanticError(ctx.expr(), "if condition must be boolean, but has type " + conditionType.getTypeName());
        }

        // --- 2. Processa o Bloco "Then" ---
        // O primeiro bloco encontrado é sempre o corpo principal do if.
        AST thenNode = visit(ctx.block(0)); 
        ifNode.addChild(thenNode); // O segundo filho é sempre o bloco "then".

        // --- 3. Processa a Parte "Else" (se existir) ---
        if (ctx.ELSE() != null) {
            AST elseNode = null;
            if (ctx.ifStmt() != null) {
                // Caso: else if (...)
                // Visita recursivamente a próxima declaração if.
                elseNode = visit(ctx.ifStmt());
            } else if (ctx.block().size() > 1) {
                // Caso: else { ... }
                // Visita o segundo bloco de código.
                elseNode = visit(ctx.block(1));
            }
            
            if (elseNode != null) {
                ifNode.addChild(elseNode); // O terceiro filho (opcional) é o bloco "else".
            }
        }
        
        return ifNode;
    }

    @Override
    public AST visitForLoopStatement(Go_Parser.ForLoopStatementContext ctx) {
        // --- Lógica de Controle (Mantida do seu código original) ---
        varTable.enterScope(); // Laços 'for' criam um novo escopo.
        loopDepth++;

        AST forNode = new AST(NodeKind.FOR_CLAUSE_NODE, GoType.NO_TYPE); // Usamos FOR_CLAUSE_NODE para o nó principal

        // A gramática define 3 tipos de 'for'
        if (ctx.forClause() != null) {
            // Caso 1: for com cláusula completa (init; cond; post)
            AST clauseNode = visit(ctx.forClause());
            // Adiciona os filhos da cláusula (init, cond, post) diretamente ao nó do for
            for (AST child : clauseNode.getChildren()) {
                forNode.addChild(child);
            }
        } else if (ctx.expr() != null) {
            // Caso 2: for usado como 'while' (apenas com condição)
            AST conditionNode = visit(ctx.expr());
            if (conditionNode.getAnnotatedType() != GoType.BOOL) {
                reportSemanticError(ctx.expr(), "for condition must be boolean, got " + conditionNode.getAnnotatedType().getTypeName());
            }
            // Adiciona placeholders para init e post
            forNode.addChild(null); // init
            forNode.addChild(conditionNode);
            forNode.addChild(null); // post
        } else {
            // Caso 3: for infinito (sem cláusula ou condição)
            // Adiciona placeholders para tudo
            forNode.addChild(null); // init
            forNode.addChild(null); // cond
            forNode.addChild(null); // post
        }

        // Adiciona o corpo do laço como o último filho
        AST bodyNode = visit(ctx.block());
        forNode.addChild(bodyNode);

        // --- Fim da Lógica de Controle ---
        loopDepth--;
        varTable.exitScope();

        return forNode;
    }

    @Override
    public AST visitForClauseRule(Go_Parser.ForClauseRuleContext ctx) {
        // Este método apenas coleta os 3 componentes da cláusula.
        AST clauseNode = new AST(NodeKind.FOR_CLAUSE_NODE, GoType.NO_TYPE);

        // 1. Inicialização (opcional)
        clauseNode.addChild(ctx.simpleStmt(0) != null ? visit(ctx.simpleStmt(0)) : null);
        
        // 2. Condição (opcional)
        AST conditionNode = null;
        if (ctx.expr() != null) {
            conditionNode = visit(ctx.expr());
            if (conditionNode.getAnnotatedType() != GoType.BOOL) {
                reportSemanticError(ctx.expr(), "for condition must be boolean, got " + conditionNode.getAnnotatedType().getTypeName());
            }
        }
        clauseNode.addChild(conditionNode);

        // 3. Pós-execução (opcional)
        clauseNode.addChild(ctx.simpleStmt(1) != null ? visit(ctx.simpleStmt(1)) : null);

        return clauseNode;
    }

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
    @Override
    public AST visitBreakStatementRule(Go_Parser.BreakStatementRuleContext ctx) {
        // Garante que o 'break' só pode ocorrer dentro de um laço.
        if (loopDepth == 0) {
            reportSemanticError(ctx, "break statement not in loop");
        }
        
        // Construção da AST:
        // Cria um nó simples para representar o comando.
        return new AST(NodeKind.BREAK_NODE, GoType.NO_TYPE);
    }


    // /**
    // * Processa statements continue
    // */
    @Override
    public AST visitContinueStatementRule(Go_Parser.ContinueStatementRuleContext ctx) {
        // Garante que o 'continue' só pode ocorrer dentro de um laço.
        if (loopDepth == 0) {
            reportSemanticError(ctx, "continue statement not in loop");
        }
        
        // Construção da AST:
        // Cria um nó simples para representar o comando.
        return new AST(NodeKind.CONTINUE_NODE, GoType.NO_TYPE);
    }


    // return null;
    // }

    // // --- STATEMENTS SIMPLES ---

    @Override
    public AST visitExpressionSimpleStmt(Go_Parser.ExpressionSimpleStmtContext ctx) {
        // Cria o nó que representa um "statement de expressão".
        AST exprStmtNode = new AST(NodeKind.EXPR_STMT_NODE, GoType.NO_TYPE);

        // Visita a expressão interna para construir sua subárvore.
        AST innerExprNode = visit(ctx.expr());
        
        // Adiciona a subárvore da expressão como filha.
        exprStmtNode.addChild(innerExprNode);

        return exprStmtNode;
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
        stringTable.printTable();

        // Estatísticas das tabelas
        System.out.println("\n--- ESTATÍSTICAS ---");
        System.out.println("Variáveis/Constantes: " + varTable.getScopeDepth() + " escopo(s)");
        System.out.println("Funções declaradas: " + functionTable.size());
        System.out.println("Arrays declarados: " + arrayTable.size());
        System.out.println("Strings literais: " + stringTable.size());
    }

    // // Exibe a AST no formato DOT em stderr.
    // public void printAST() {
    //     if (root == null) {
    //         System.err.println("AST not built - no tree to print");
    //         return;
    //     }
    //     AST.printDot(root, this.varTable);
    // }
}
