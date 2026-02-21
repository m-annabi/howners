package com.howners.gestion.exception.esignature;

/**
 * Exception thrown when webhook validation fails
 */
public class WebhookValidationException extends ESignatureException {
    public WebhookValidationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public WebhookValidationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
