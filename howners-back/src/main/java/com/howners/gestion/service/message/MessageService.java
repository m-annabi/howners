package com.howners.gestion.service.message;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.message.Message;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.message.ConversationResponse;
import com.howners.gestion.dto.message.CreateMessageRequest;
import com.howners.gestion.dto.message.MessageResponse;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ApplicationRepository;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.MessageRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public MessageResponse send(CreateMessageRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User recipient = userRepository.findById(request.recipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

        Message.MessageBuilder builder = Message.builder()
                .sender(sender)
                .recipient(recipient)
                .subject(request.subject())
                .body(request.body());

        if (request.listingId() != null) {
            Listing listing = listingRepository.findById(request.listingId()).orElse(null);
            builder.listing(listing);
        }

        if (request.applicationId() != null) {
            Application application = applicationRepository.findById(request.applicationId()).orElse(null);
            builder.application(application);
        }

        if (request.parentId() != null) {
            Message parent = messageRepository.findById(request.parentId()).orElse(null);
            builder.parent(parent);
        }

        Message message = builder.build();
        message = messageRepository.save(message);
        log.info("Message sent from {} to {}", currentUserId, request.recipientId());
        return MessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations() {
        UUID currentUserId = AuthService.getCurrentUserId();
        List<Message> latestMessages = messageRepository.findLatestMessagePerConversation(currentUserId);

        return latestMessages.stream().map(msg -> {
            UUID partnerId = msg.getSender().getId().equals(currentUserId)
                    ? msg.getRecipient().getId()
                    : msg.getSender().getId();
            String partnerName = msg.getSender().getId().equals(currentUserId)
                    ? msg.getRecipient().getFullName()
                    : msg.getSender().getFullName();

            // Count unread from this partner
            List<Message> conversation = messageRepository.findConversation(currentUserId, partnerId);
            long unread = conversation.stream()
                    .filter(m -> m.getRecipient().getId().equals(currentUserId) && !m.getIsRead())
                    .count();

            return new ConversationResponse(
                    partnerId,
                    partnerName,
                    msg.getBody().length() > 100 ? msg.getBody().substring(0, 100) + "..." : msg.getBody(),
                    msg.getIsRead(),
                    msg.getCreatedAt(),
                    unread
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getConversation(UUID otherUserId) {
        UUID currentUserId = AuthService.getCurrentUserId();
        return messageRepository.findConversation(currentUserId, otherUserId)
                .stream().map(MessageResponse::from).toList();
    }

    @Transactional
    public void markAsRead(UUID messageId) {
        UUID currentUserId = AuthService.getCurrentUserId();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (message.getRecipient().getId().equals(currentUserId) && !message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);
        }
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        UUID currentUserId = AuthService.getCurrentUserId();
        return messageRepository.countUnreadByRecipientId(currentUserId);
    }
}
