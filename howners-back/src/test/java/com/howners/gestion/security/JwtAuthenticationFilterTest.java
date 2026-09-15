package com.howners.gestion.security;

import com.howners.gestion.security.jwt.JwtAuthenticationFilter;
import com.howners.gestion.security.jwt.JwtTokenProvider;
import com.howners.gestion.service.user.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }
    @Test void disabledUserCannotAuthenticateWithPreviouslyIssuedJwt() throws Exception {
        var tokens = mock(JwtTokenProvider.class);
        var users = mock(CustomUserDetailsService.class);
        UUID id = UUID.randomUUID();
        when(tokens.validateToken("token")).thenReturn(true);
        when(tokens.getUserIdFromToken("token")).thenReturn(id);
        when(users.loadUserById(id)).thenReturn(new UserPrincipal(id, "test@example.com", "", "OWNER", false, 0));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        new JwtAuthenticationFilter(tokens, users).doFilter(request, new MockHttpServletResponse(),
                (req, res) -> assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull());
    }
}
