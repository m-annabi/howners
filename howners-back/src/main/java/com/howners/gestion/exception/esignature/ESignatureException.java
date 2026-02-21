package com.howners.gestion.exception.esignature;

/**
 * Base exception for all e-signature related errors
 */
public class ESignatureException extends RuntimeException {
    private final String errorCode;

    public ESignatureException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ESignatureException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
