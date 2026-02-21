package com.howners.gestion.dto.message;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID partnerId,
        String partnerName,
        String lastMessageBody,
        Boolean lastMessageRead,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
