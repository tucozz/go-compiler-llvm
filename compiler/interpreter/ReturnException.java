package compiler.interpreter;

/**
 * Uma exceção especial usada para sinalizar a execução de um comando 'return'.
 * Carrega o valor de retorno da função.
 */
public class ReturnException extends RuntimeException {
    public final Object returnValue;

    public ReturnException(Object returnValue) {
        // Desativa a criação da stack trace, pois não precisamos dela e é dispendiosa.
        super(null, null, false, false); 
        this.returnValue = returnValue;
    }
}
