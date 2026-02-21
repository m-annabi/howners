package com.howners.gestion.controller;

import com.howners.gestion.dto.message.ConversationResponse;
import com.howners.gestion.dto.message.CreateMessageRequest;
import com.howners.gestion.dto.message.MessageResponse;
import com.howners.gestion.service.message.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"${app.cors.allowed-origins}"})
@PreAuthorize("isAuthenticated()")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> send(@Valid @RequestBody CreateMessageRequest request) {
        log.info("Sending message to: {}", request.recipientId());
        MessageResponse message = messageService.send(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations() {
        log.info("Fetching conversations");
        List<ConversationResponse> conversations = messageService.getConversations();
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversation/{userId}")
    public ResponseEntity<List<MessageResponse>> getConversation(@PathVariable UUID userId) {
        log.info("Fetching conversation with: {}", userId);
        List<MessageResponse> messages = messageService.getConversation(userId);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        messageService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = messageService.getUnreadCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
