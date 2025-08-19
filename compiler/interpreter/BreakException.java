package compiler.interpreter;

/**
 * Uma exceção especial usada para sinalizar a execução de um comando 'break'.
 */
public class BreakException extends RuntimeException {
    public BreakException() {
        super(null, null, false, false);
    }
}
