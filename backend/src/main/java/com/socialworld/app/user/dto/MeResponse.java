package com.socialworld.app.user.dto;

import com.socialworld.app.avatar.dto.AvatarResponse;
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
        List<UserLanguageResponse> languages,
        AvatarResponse avatar
) {
    public static MeResponse from(User user, List<UserLanguageResponse> languages, AvatarResponse avatar) {
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getBirthDate(),
                user.getCountryCode(),
                user.getBio(),
                user.getGender(),
                user.getCurrentIntention(),
                languages,
                avatar);
    }
}
