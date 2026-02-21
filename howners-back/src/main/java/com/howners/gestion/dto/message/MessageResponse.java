package com.howners.gestion.dto.message;

import com.howners.gestion.domain.message.Message;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        String senderName,
        UUID recipientId,
        String recipientName,
        UUID listingId,
        UUID applicationId,
        String subject,
        String body,
        Boolean isRead,
        LocalDateTime readAt,
        UUID parentId,
        LocalDateTime createdAt
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getSender().getId(),
                m.getSender().getFullName(),
                m.getRecipient().getId(),
                m.getRecipient().getFullName(),
                m.getListing() != null ? m.getListing().getId() : null,
                m.getApplication() != null ? m.getApplication().getId() : null,
                m.getSubject(),
                m.getBody(),
                m.getIsRead(),
                m.getReadAt(),
                m.getParent() != null ? m.getParent().getId() : null,
                m.getCreatedAt()
        );
    }
}
