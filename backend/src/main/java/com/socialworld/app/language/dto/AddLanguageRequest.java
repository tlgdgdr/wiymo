package com.socialworld.app.language.dto;

import com.socialworld.app.language.LanguageLevel;
import com.socialworld.app.language.LanguageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AddLanguageRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z]{2,3}$", message = "must be an ISO 639 language code")
        String languageCode,

        @NotNull
        LanguageType type,

        /** Ignored for NATIVE entries (level is forced to NATIVE); required otherwise. */
        LanguageLevel level
) {
}
