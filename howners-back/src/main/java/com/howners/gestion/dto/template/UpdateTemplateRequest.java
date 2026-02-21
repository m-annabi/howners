package com.howners.gestion.dto.template;

import jakarta.validation.constraints.Size;

public record UpdateTemplateRequest(
        @Size(max = 255, message = "Template name must not exceed 255 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Size(max = 51200, message = "Template content must not exceed 50KB")
        String content,

        Boolean isActive
) {
}
