package com.howners.gestion.exception.esignature;

/**
 * Exception thrown when there's an error with the e-signature provider (DocuSign, etc.)
 */
public class ESignatureProviderException extends ESignatureException {
    public ESignatureProviderException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ESignatureProviderException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
