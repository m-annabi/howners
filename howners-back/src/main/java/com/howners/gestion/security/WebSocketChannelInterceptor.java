package com.howners.gestion.security;

import com.howners.gestion.security.jwt.JwtTokenProvider;
import com.howners.gestion.service.user.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChannelInterceptor.class);

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                UUID userId = tokenProvider.getUserIdFromToken(token);
                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                if (!(userDetails instanceof UserPrincipal principal) || !principal.isEnabled()
                        || tokenProvider.getTokenVersionFromToken(token) != principal.getTokenVersion()) {
                    throw new AccessDeniedException("Session invalide ou révoquée");
                }
                if (accessor.getSessionAttributes() == null) {
                    throw new AccessDeniedException("Session WebSocket absente");
                }
                accessor.getSessionAttributes().put("authToken", token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                accessor.setUser(auth);
                log.debug("WebSocket authenticated user: {}", userId);
            } else {
                throw new AccessDeniedException("Authentification requise");
            }
        }

        if (accessor != null && (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                || StompCommand.SEND.equals(accessor.getCommand()))) {
            String token = accessor.getSessionAttributes() == null ? null
                    : (String) accessor.getSessionAttributes().get("authToken");
            if (accessor.getUser() == null || !StringUtils.hasText(token) || !tokenProvider.validateToken(token)) {
                throw new AccessDeniedException("Authentification requise");
            }
            UserDetails current = customUserDetailsService.loadUserById(tokenProvider.getUserIdFromToken(token));
            if (!(current instanceof UserPrincipal principal) || !principal.isEnabled()
                    || principal.getTokenVersion() != tokenProvider.getTokenVersionFromToken(token)) {
                throw new AccessDeniedException("Session révoquée");
            }
            // Les messages sont envoyés par l'API REST ; le socket ne permet que la réception privée.
            if (StompCommand.SEND.equals(accessor.getCommand())
                    || !"/user/queue/messages".equals(accessor.getDestination())) {
                throw new AccessDeniedException("Destination interdite");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // Fallback: token header directly
        return accessor.getFirstNativeHeader("token");
    }
}
