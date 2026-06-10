package com.howners.gestion.dto.email;

import lombok.Builder;

@Builder
public record PaymentReminderEmailData(
        String recipientEmail,
        String recipientName,
        String ownerName,
        String propertyName,
        String propertyAddress,
        String amount,
        String currency,
        String dueDate,
        String paymentUrl,
        boolean isOverdue
) {}
