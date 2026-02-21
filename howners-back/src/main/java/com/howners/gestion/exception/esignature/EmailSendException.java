package com.howners.gestion.exception.esignature;

/**
 * Exception thrown when there's an error sending an email
 */
public class EmailSendException extends ESignatureException {
    public EmailSendException(String message, String errorCode) {
        super(message, errorCode);
    }

    public EmailSendException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
