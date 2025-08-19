package compiler.interpreter;

import java.util.EmptyStackException;
import java.util.Stack;

public class OperandStack {

    private final Stack<Object> stack;

    public OperandStack() {
        this.stack = new Stack<>();
    }

    private void checkEmpty() {
        if (stack.isEmpty()) {
            throw new EmptyStackException();
        }
    }

    // --- Métodos para Inteiros ---
    public void pushInt(int value) {
        stack.push(value);
    }

    public int popInt() {
        checkEmpty();
        return (Integer) stack.pop();
    }

    // --- Métodos para Floats ---
    public void pushFloat(float value) {
        stack.push(value);
    }

    public float popFloat() {
        checkEmpty();
        return (Float) stack.pop();
    }

    // --- Métodos para Booleanos ---
    public void pushBool(boolean value) {
        stack.push(value);
    }

    public boolean popBool() {
        checkEmpty();
        return (Boolean) stack.pop();
    }
    
    // --- Métodos para Strings ---
    public void pushString(String value) {
        stack.push(value);
    }

    public String popString() {
        checkEmpty();
        return (String) stack.pop();
    }
    
    public Object peek() {
        checkEmpty();
        return stack.peek();
    }
}
