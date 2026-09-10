package com.socialworld.app.discovery.dto;

import com.socialworld.app.avatar.dto.AvatarResponse;
import com.socialworld.app.intention.Intention;
import com.socialworld.app.language.dto.UserLanguageResponse;
import com.socialworld.app.user.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

/** A discovery result; score is exposed so ranking is debuggable. */
public record DiscoveredUserResponse(
        UUID id,
        String username,
        int age,
        String countryCode,
        String bio,
        Intention currentIntention,
        List<UserLanguageResponse> languages,
        AvatarResponse avatar,
        boolean online,
        Instant lastSeenAt,
        int score
) {
    public static DiscoveredUserResponse from(User user, List<UserLanguageResponse> languages,
                                              AvatarResponse avatar, int score) {
        return new DiscoveredUserResponse(
                user.getId(),
                user.getUsername(),
                Period.between(user.getBirthDate(), LocalDate.now()).getYears(),
                user.getCountryCode(),
                user.getBio(),
                user.getCurrentIntention(),
                languages,
                avatar,
                user.isOnline(),
                user.getLastSeenAt(),
                score);
    }
}
