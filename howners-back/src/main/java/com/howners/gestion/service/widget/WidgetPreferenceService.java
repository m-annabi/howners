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
                    new WidgetConfigDto("kpi-revenue",        true, 0, null),
                    new WidgetConfigDto("kpi-expenses",       true, 1, null),
                    new WidgetConfigDto("kpi-net",            true, 2, null),
                    new WidgetConfigDto("kpi-pending",        true, 3, null),
                    new WidgetConfigDto("kpi-overdue",        true, 4, null),
                    new WidgetConfigDto("monthly-chart",      true, 5, null),
                    new WidgetConfigDto("expense-categories", true, 6, null)
            );
            default -> List.of(
                    new WidgetConfigDto("stat-properties",     true,  0, null),
                    new WidgetConfigDto("stat-rentals",        true,  1, null),
                    new WidgetConfigDto("stat-revenue",        true,  2, null),
                    new WidgetConfigDto("stat-pending",        true,  3, null),
                    new WidgetConfigDto("action-items",        true,  4, null),
                    new WidgetConfigDto("recent-activity",     true,  5, null),
                    new WidgetConfigDto("top-tenants",         true,  6, null),
                    new WidgetConfigDto("shortcut-properties", true,  7, null),
                    new WidgetConfigDto("shortcut-rentals",    true,  8, null),
                    new WidgetConfigDto("shortcut-contracts",  true,  9, null),
                    new WidgetConfigDto("shortcut-payments",   true, 10, null),
                    new WidgetConfigDto("shortcut-listings",   true, 11, null),
                    new WidgetConfigDto("shortcut-invoices",   true, 12, null),
                    new WidgetConfigDto("shortcut-expenses",   true, 13, null),
                    new WidgetConfigDto("shortcut-messages",   true, 14, null),
                    new WidgetConfigDto("shortcut-financial",  true, 15, null)
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
