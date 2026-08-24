package com.howners.gestion.service.message;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.message.Message;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.message.ConversationResponse;
import com.howners.gestion.dto.message.CreateMessageRequest;
import com.howners.gestion.dto.message.MessageResponse;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ApplicationRepository;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.MessageRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final RentalRepository rentalRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse send(CreateMessageRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User recipient = userRepository.findById(request.recipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));

        assertCanMessage(sender, recipient, request.listingId(), request.applicationId());

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

        MessageResponse response = MessageResponse.from(message);
        // Principal.getName() = email (voir UserPrincipal.getUsername())
        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/messages",
                response
        );
        return response;
    }

    /**
     * Un message n'est autorisé qu'entre personnes ayant un lien légitime :
     * un échange déjà entamé, une annonce (le prospect contacte le bailleur),
     * une candidature (candidat ↔ bailleur), ou un bail commun (bailleur ↔ locataire).
     * Empêche d'écrire à un utilisateur arbitraire par simple connaissance de son ID.
     */
    private void assertCanMessage(User sender, User recipient, UUID listingId, UUID applicationId) {
        UUID a = sender.getId();
        UUID b = recipient.getId();

        if (a.equals(b)) {
            throw new ForbiddenException("Vous ne pouvez pas vous envoyer un message à vous-même.");
        }

        // Un admin (support) peut échanger avec n'importe qui.
        if (sender.getRole() == Role.ADMIN || recipient.getRole() == Role.ADMIN) {
            return;
        }

        // Échange déjà entamé légitimement → réponses autorisées.
        if (!messageRepository.findConversation(a, b).isEmpty()) {
            return;
        }

        // Contexte annonce : l'un des deux en est le propriétaire (le prospect contacte le bailleur).
        if (listingId != null) {
            Listing listing = listingRepository.findById(listingId).orElse(null);
            UUID ownerId = listing != null && listing.getProperty() != null && listing.getProperty().getOwner() != null
                    ? listing.getProperty().getOwner().getId() : null;
            if (ownerId != null && (ownerId.equals(a) || ownerId.equals(b))) {
                return;
            }
        }

        // Contexte candidature : la paire doit être {candidat, propriétaire de l'annonce}.
        if (applicationId != null) {
            Application app = applicationRepository.findById(applicationId).orElse(null);
            if (app != null && app.getApplicant() != null && app.getListing() != null
                    && app.getListing().getProperty() != null && app.getListing().getProperty().getOwner() != null) {
                UUID applicantId = app.getApplicant().getId();
                UUID ownerId = app.getListing().getProperty().getOwner().getId();
                if (Set.of(a, b).equals(Set.of(applicantId, ownerId))) {
                    return;
                }
            }
        }

        // Bail commun : l'un est propriétaire du bien loué à l'autre (dans un sens ou l'autre).
        if (!rentalRepository.findByOwnerIdAndTenantId(a, b).isEmpty()
                || !rentalRepository.findByOwnerIdAndTenantId(b, a).isEmpty()) {
            return;
        }

        throw new ForbiddenException(
                "Vous ne pouvez contacter que les personnes liées à vos annonces, candidatures ou baux.");
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
