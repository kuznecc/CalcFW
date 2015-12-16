package org.bober.calculation;

public class ProductionFlowException extends Exception{

    public ProductionFlowException(String message) {
        super(message);
    }

    public ProductionFlowException(String message, Throwable cause) {
        super(message + " | " + cause.getMessage(), cause);
    }
}
