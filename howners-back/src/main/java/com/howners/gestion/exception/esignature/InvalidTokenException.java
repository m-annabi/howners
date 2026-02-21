package com.howners.gestion.exception.esignature;

/**
 * Exception thrown when a token is invalid or doesn't match
 */
public class InvalidTokenException extends ESignatureException {
    public InvalidTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
}
