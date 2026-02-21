package com.howners.gestion.exception.esignature;

/**
 * Exception thrown when a contract is not found
 */
public class ContractNotFoundException extends ESignatureException {
    public ContractNotFoundException(String message, String errorCode) {
        super(message, errorCode);
    }
}
