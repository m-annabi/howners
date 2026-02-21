package com.howners.gestion.dto.template;

public record PreviewTemplateResponse(
        String filledContent,
        String rentalPropertyName,
        String tenantFullName
) {
}
