package com.howners.gestion.dto.widget;

import java.util.List;

public record WidgetConfigDto(String id, boolean visible, int order, List<String> items) {}
