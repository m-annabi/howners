package com.howners.gestion.exception.esignature;

/**
 * Exception thrown when a signature token has expired
 */
public class TokenExpiredException extends ESignatureException {
    public TokenExpiredException(String message, String errorCode) {
        super(message, errorCode);
    }
}
