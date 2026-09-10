package com.socialworld.app.user.dto;

import com.socialworld.app.intention.Intention;
import com.socialworld.app.language.dto.UserLanguageResponse;
import com.socialworld.app.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The caller's own account. Never contains the password hash or tokens. */
public record MeResponse(
        UUID id,
        String username,
        String email,
        LocalDate birthDate,
        String countryCode,
        String bio,
        String gender,
        Intention currentIntention,
        List<UserLanguageResponse> languages
) {
    public static MeResponse from(User user, List<UserLanguageResponse> languages) {
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getBirthDate(),
                user.getCountryCode(),
                user.getBio(),
                user.getGender(),
                user.getCurrentIntention(),
                languages);
    }
}
