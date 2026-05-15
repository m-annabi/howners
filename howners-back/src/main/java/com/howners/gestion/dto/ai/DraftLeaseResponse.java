package com.howners.gestion.dto.ai;

public record DraftLeaseResponse(
        String content,
        String engine,
        String disclaimer
) {}
