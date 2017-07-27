package org.bober.calculation;

public class CalcFlowException extends Exception{

    public CalcFlowException(String message) {
        super(message);
    }

    public CalcFlowException(String message, Throwable cause) {
        super(message + " | " + cause.getMessage(), cause);
    }
}
