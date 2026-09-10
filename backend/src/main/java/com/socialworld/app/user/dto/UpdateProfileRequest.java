package com.socialworld.app.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** All fields optional; only non-null fields are applied. */
public record UpdateProfileRequest(
        @Size(max = 500)
        String bio,

        @Pattern(regexp = "^[A-Z]{2}$", message = "must be an ISO 3166-1 alpha-2 code")
        String countryCode,

        @Size(max = 30)
        String gender
) {
}
