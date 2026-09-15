package com.howners.gestion.security;

import com.howners.gestion.security.jwt.JwtTokenProvider;
import com.howners.gestion.service.user.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import java.util.HashMap;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketChannelInterceptorTest {
    JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    CustomUserDetailsService users = mock(CustomUserDetailsService.class);
    WebSocketChannelInterceptor interceptor = new WebSocketChannelInterceptor(tokens, users);
    UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "test@example.com", "", "OWNER", true, 2);

    @BeforeEach void setup() {
        when(tokens.validateToken("valid")).thenReturn(true);
        when(tokens.getUserIdFromToken("valid")).thenReturn(principal.getId());
        when(tokens.getTokenVersionFromToken("valid")).thenReturn(2);
        when(users.loadUserById(principal.getId())).thenReturn(principal);
    }
    StompHeaderAccessor connect(String token) {
        var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>());
        if (token != null) accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        return accessor;
    }
    void send(StompHeaderAccessor accessor) {
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class));
    }
    @Test void rejectsMissingAndInvalidTokens() {
        assertThatThrownBy(() -> send(connect(null))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> send(connect("invalid"))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void rejectsRevokedToken() {
        when(tokens.getTokenVersionFromToken("valid")).thenReturn(1);
        assertThatThrownBy(() -> send(connect("valid"))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void rejectsDisabledUser() {
        principal.setEnabled(false);
        assertThatThrownBy(() -> send(connect("valid"))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void allowsOnlyPrivateSubscriptionAndRechecksRevocation() {
        var connection = connect("valid");
        send(connection);
        assertThat(connection.getUser()).isNotNull();
        var subscription = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscription.setSessionAttributes(connection.getSessionAttributes());
        subscription.setUser(connection.getUser());
        subscription.setDestination("/user/queue/messages");
        subscription.setLeaveMutable(true);
        assertThatCode(() -> send(subscription)).doesNotThrowAnyException();
        subscription.setDestination("/topic/messages");
        assertThatThrownBy(() -> send(subscription)).isInstanceOf(AccessDeniedException.class);
        subscription.setDestination("/user/queue/messages");
        principal.setTokenVersion(3);
        assertThatThrownBy(() -> send(subscription)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void rejectsClientMessages() {
        var connection = connect("valid");
        send(connection);
        var outgoing = StompHeaderAccessor.create(StompCommand.SEND);
        outgoing.setUser(connection.getUser());
        outgoing.setSessionAttributes(connection.getSessionAttributes());
        outgoing.setDestination("/user/queue/messages");
        assertThatThrownBy(() -> send(outgoing)).isInstanceOf(AccessDeniedException.class);
    }
}
