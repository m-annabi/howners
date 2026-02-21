package com.howners.gestion.exception.esignature;

/**
 * Exception thrown when a signature request is not found
 */
public class SignatureRequestNotFoundException extends ESignatureException {
    public SignatureRequestNotFoundException(String message, String errorCode) {
        super(message, errorCode);
    }
}
