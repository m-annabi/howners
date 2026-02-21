package com.howners.gestion.dto.template;

import java.util.List;

public record TemplateVariablesResponse(
        List<VariableInfo> variables
) {
    public record VariableInfo(
            String key,
            String label,
            String category,
            String example
    ) {
    }
}
