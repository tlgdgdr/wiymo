package com.socialworld.app.language.dto;

import com.socialworld.app.language.LanguageLevel;
import com.socialworld.app.language.LanguageType;
import com.socialworld.app.language.UserLanguage;

import java.util.UUID;

public record UserLanguageResponse(
        UUID id,
        String languageCode,
        LanguageType type,
        LanguageLevel level
) {
    public static UserLanguageResponse from(UserLanguage entity) {
        return new UserLanguageResponse(
                entity.getId(),
                entity.getLanguageCode(),
                entity.getType(),
                entity.getLevel());
    }
}
