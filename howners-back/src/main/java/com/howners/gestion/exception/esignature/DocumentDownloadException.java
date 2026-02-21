package com.howners.gestion.exception.esignature;

/**
 * Exception thrown when there's an error downloading a document
 */
public class DocumentDownloadException extends ESignatureException {
    public DocumentDownloadException(String message, String errorCode) {
        super(message, errorCode);
    }

    public DocumentDownloadException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
