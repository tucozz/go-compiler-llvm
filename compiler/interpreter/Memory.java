package compiler.interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Memory {

    private final Stack<Map<String, Object>> scopeStack;

    public Memory() {
        this.scopeStack = new Stack<>();
        enterScope(); // Escopo global
    }

    public void enterScope() {
        scopeStack.push(new HashMap<>());
    }

    public void exitScope() {
        if (scopeStack.size() > 1) {
            scopeStack.pop();
        }
    }

    /**
     * Declara uma nova variável no escopo ATUAL. Usado para 'var', ':=', e parâmetros de função.
     */
    public void declare(String name, Object value) {
        scopeStack.peek().put(name, value);
    }

    /**
     * Atualiza o valor de uma variável existente, procurando do escopo mais interno
     * para o mais externo. Usado para '='.
     */
    public void update(String name, Object value) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Object> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                scope.put(name, value);
                return;
            }
        }
        // Este caso não deve acontecer se o analisador semântico funcionou.
    }

    public Object fetch(String name) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, Object> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null; 
    }
}
