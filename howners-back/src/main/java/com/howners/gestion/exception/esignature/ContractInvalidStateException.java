package com.howners.gestion.exception.esignature;

/**
 * Exception thrown when a contract is in an invalid state for the requested operation
 */
public class ContractInvalidStateException extends ESignatureException {
    public ContractInvalidStateException(String message, String errorCode) {
        super(message, errorCode);
    }
}
