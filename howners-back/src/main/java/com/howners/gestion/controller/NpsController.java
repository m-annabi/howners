package com.howners.gestion.controller;

import com.howners.gestion.domain.feedback.NpsResponse;
import com.howners.gestion.repository.NpsResponseRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NpsController {

    private final NpsResponseRepository repository;
    private final UserRepository userRepository;

    @GetMapping("/nps/status")
    public ResponseEntity<Map<String, Boolean>> hasAnswered() {
        UUID userId = AuthService.getCurrentUserId();
        return ResponseEntity.ok(Map.of("answered", repository.existsByUserId(userId)));
    }

    @PostMapping("/nps")
    public ResponseEntity<Void> submit(@org.springframework.web.bind.annotation.RequestBody NpsSubmission body) {
        UUID userId = AuthService.getCurrentUserId();
        NpsResponse r = NpsResponse.builder()
                .user(userRepository.getReferenceById(userId))
                .score(body.score)
                .comment(body.comment)
                .build();
        repository.save(r);
        return ResponseEntity.noContent().build();
    }

    public static class NpsSubmission {
        @NotNull
        @Min(0) @Max(10)
        public Integer score;
        public String comment;
    }
}
