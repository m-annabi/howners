package com.howners.gestion.controller;

import com.howners.gestion.dto.request.LoginRequest;
import com.howners.gestion.dto.request.RegisterRequest;
import com.howners.gestion.dto.request.ResendVerificationRequest;
import com.howners.gestion.dto.request.UpdateProfileRequest;
import com.howners.gestion.dto.request.VerifyEmailRequest;
import com.howners.gestion.dto.response.AuthMessageResponse;
import com.howners.gestion.dto.response.AuthResponse;
import com.howners.gestion.dto.response.UserResponse;
import org.springframework.http.HttpStatus;
import com.howners.gestion.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 202 : compte en attente de vérification par e-mail (plus d'auto-connexion, plus de jeton renvoyé).
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthMessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request.token()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthMessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request.email()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateCurrentUser(request));
    }

    /** Révoque toutes les sessions/jetons de l'utilisateur courant (déconnexion globale). */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAllSessions() {
        authService.logoutAllSessions();
        return ResponseEntity.noContent().build();
    }
}
