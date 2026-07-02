package com.howners.gestion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phone,

    @Size(max = 255, message = "Address must not exceed 255 characters")
    String addressLine1,

    @Size(max = 255, message = "Address must not exceed 255 characters")
    String addressLine2,

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    String postalCode,

    @Size(max = 120, message = "City must not exceed 120 characters")
    String city,

    @Size(max = 80, message = "Country must not exceed 80 characters")
    String country
) {}
