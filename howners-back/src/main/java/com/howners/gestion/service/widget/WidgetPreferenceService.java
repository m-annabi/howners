package com.howners.gestion.service.widget;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.domain.widget.UserWidgetPreference;
import com.howners.gestion.dto.widget.WidgetConfigDto;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.repository.UserWidgetPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WidgetPreferenceService {

    private final UserWidgetPreferenceRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public List<WidgetConfigDto> getPreferences(UUID userId, String page) {
        return repository.findByUserIdAndPage(userId, page)
                .map(pref -> parseJson(pref.getPreferences()))
                .orElseGet(() -> getDefaults(page));
    }

    public List<WidgetConfigDto> savePreferences(UUID userId, String page, List<WidgetConfigDto> widgets) {
        User user = userRepository.getReferenceById(userId);
        UserWidgetPreference pref = repository.findByUserIdAndPage(userId, page)
                .orElseGet(() -> UserWidgetPreference.builder().user(user).page(page).build());
        pref.setPreferences(toJson(widgets));
        repository.save(pref);
        return widgets;
    }

    private List<WidgetConfigDto> getDefaults(String page) {
        return switch (page) {
            case "financial" -> List.of(
                    new WidgetConfigDto("financial-kpis", true, 0),
                    new WidgetConfigDto("monthly-chart", true, 1),
                    new WidgetConfigDto("expense-categories", true, 2)
            );
            default -> List.of(
                    new WidgetConfigDto("overview", true, 0),
                    new WidgetConfigDto("action-items", true, 1),
                    new WidgetConfigDto("top-tenants", true, 2),
                    new WidgetConfigDto("recent-activity", true, 3),
                    new WidgetConfigDto("quick-actions", true, 4)
            );
        };
    }

    private List<WidgetConfigDto> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse widget preferences JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String toJson(List<WidgetConfigDto> widgets) {
        try {
            return objectMapper.writeValueAsString(widgets);
        } catch (Exception e) {
            log.error("Failed to serialize widget preferences: {}", e.getMessage());
            return "[]";
        }
    }
}
