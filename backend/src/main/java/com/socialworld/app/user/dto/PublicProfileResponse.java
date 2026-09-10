package com.socialworld.app.user.dto;

import com.socialworld.app.intention.Intention;
import com.socialworld.app.language.dto.UserLanguageResponse;
import com.socialworld.app.user.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

/**
 * Profile as other users see it. Exposes age, never the birth date;
 * never email, status internals, or credentials.
 */
public record PublicProfileResponse(
        UUID id,
        String username,
        int age,
        String countryCode,
        String bio,
        Intention currentIntention,
        List<UserLanguageResponse> languages,
        boolean online,
        Instant lastSeenAt
) {
    public static PublicProfileResponse from(User user, List<UserLanguageResponse> languages) {
        return new PublicProfileResponse(
                user.getId(),
                user.getUsername(),
                Period.between(user.getBirthDate(), LocalDate.now()).getYears(),
                user.getCountryCode(),
                user.getBio(),
                user.getCurrentIntention(),
                languages,
                user.isOnline(),
                user.getLastSeenAt());
    }
}
