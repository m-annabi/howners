package com.howners.gestion.dto.notification;

import com.howners.gestion.domain.notification.Notification;
import com.howners.gestion.domain.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.getRead(),
                notification.getCreatedAt()
        );
    }
}
