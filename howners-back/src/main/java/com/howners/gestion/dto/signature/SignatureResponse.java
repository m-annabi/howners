package com.howners.gestion.dto.signature;

import com.howners.gestion.domain.signature.Signature;
import com.howners.gestion.domain.signature.SignatureStatus;
import com.howners.gestion.domain.signature.SignatureType;

import java.time.LocalDateTime;
import java.util.UUID;

public record SignatureResponse(
        UUID id,
        UUID contractId,
        UUID signerId,
        String signerFullName,
        SignatureType signatureType,
        SignatureStatus status,
        String provider,
        String ipAddress,
        LocalDateTime signedAt,
        LocalDateTime createdAt
) {
    public static SignatureResponse from(Signature signature) {
        return new SignatureResponse(
                signature.getId(),
                signature.getContract() != null ? signature.getContract().getId() : null,
                signature.getSigner().getId(),
                getFullName(signature.getSigner()),
                signature.getSignatureType(),
                signature.getStatus(),
                signature.getProvider(),
                signature.getIpAddress(),
                signature.getSignedAt(),
                signature.getCreatedAt()
        );
    }

    private static String getFullName(com.howners.gestion.domain.user.User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
