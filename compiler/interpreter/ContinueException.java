package compiler.interpreter;

/**
 * Uma exceção especial usada para sinalizar a execução de um comando 'continue'.
 */
public class ContinueException extends RuntimeException {
    public ContinueException() {
        super(null, null, false, false);
    }
}
