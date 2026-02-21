package com.howners.gestion.domain.audit;

public enum AuditAction {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    PAYMENT_CREATED,
    PAYMENT_CONFIRMED,
    CONTRACT_SIGNED,
    DATA_EXPORT,
    DATA_ERASURE
}
