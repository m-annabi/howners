package com.howners.gestion.dto.response;

import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String addressLine1,
        String addressLine2,
        String postalCode,
        String city,
        String country,
        Role role,
        Boolean enabled,
        LocalDateTime createdAt,
        String paymentInstructions,
        Boolean acceptOnlinePayments,
        String stripeConnectStatus
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getAddressLine1(),
                user.getAddressLine2(),
                user.getPostalCode(),
                user.getCity(),
                user.getCountry(),
                user.getRole(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getPaymentInstructions(),
                user.getAcceptOnlinePayments(),
                user.getStripeConnectStatus()
        );
    }
}
