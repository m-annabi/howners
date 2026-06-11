package com.howners.gestion.service.notification;

import com.howners.gestion.domain.notification.Notification;
import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.notification.NotificationResponse;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.NotificationRepository;
import com.howners.gestion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Crée une notification pour un utilisateur.
     */
    @Transactional
    public Notification create(UUID userId, NotificationType type, String title, String message, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .read(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification créée pour l'utilisateur {} : [{}] {}", userId, type, title);
        return notification;
    }

    /**
     * Retourne le nombre de notifications non lues pour un utilisateur.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Retourne les notifications récentes d'un utilisateur (paginées).
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getRecent(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    /**
     * Marque une notification comme lue.
     */
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId.toString()));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à modifier cette notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("Toutes les notifications marquées comme lues pour l'utilisateur {}", userId);
    }
}
