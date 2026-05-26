package com.howners.gestion.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PublishRentalRequest(
        @NotBlank String title,
        String description,
        LocalDate availableFrom
) {}
