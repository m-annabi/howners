package com.howners.gestion.controller;

import com.howners.gestion.dto.widget.WidgetConfigDto;
import com.howners.gestion.dto.widget.WidgetPreferencesRequest;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.widget.WidgetPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preferences/widgets")
@RequiredArgsConstructor
public class WidgetPreferenceController {

    private final WidgetPreferenceService widgetPreferenceService;

    @GetMapping("/{page}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WidgetConfigDto>> getPreferences(
            @PathVariable String page,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(widgetPreferenceService.getPreferences(principal.getId(), page));
    }

    @PutMapping("/{page}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WidgetConfigDto>> savePreferences(
            @PathVariable String page,
            @RequestBody WidgetPreferencesRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                widgetPreferenceService.savePreferences(principal.getId(), page, request.widgets()));
    }
}
