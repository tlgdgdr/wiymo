package com.socialworld.app.language;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.language.dto.AddLanguageRequest;
import com.socialworld.app.language.dto.UserLanguageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLanguageService {

    static final int MAX_LANGUAGES_PER_USER = 10;

    private final UserLanguageRepository userLanguageRepository;

    @Transactional(readOnly = true)
    public List<UserLanguageResponse> listForUser(UUID userId) {
        return userLanguageRepository.findByUserIdOrderByCreatedAt(userId).stream()
                .map(UserLanguageResponse::from)
                .toList();
    }

    @Transactional
    public UserLanguageResponse add(UUID userId, AddLanguageRequest request) {
        String code = request.languageCode().toLowerCase();

        LanguageLevel level;
        if (request.type() == LanguageType.NATIVE) {
            level = LanguageLevel.NATIVE;
        } else {
            if (request.level() == null || request.level() == LanguageLevel.NATIVE) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR,
                        "level: a CEFR level (A1-C2) is required for non-native languages");
            }
            level = request.level();
        }

        if (userLanguageRepository.existsByUserIdAndLanguageCodeAndType(userId, code, request.type())) {
            throw new ApiException(ErrorCode.LANGUAGE_ALREADY_ADDED);
        }
        if (userLanguageRepository.countByUserId(userId) >= MAX_LANGUAGES_PER_USER) {
            throw new ApiException(ErrorCode.LANGUAGE_LIMIT_REACHED);
        }

        UserLanguage saved = userLanguageRepository.save(UserLanguage.builder()
                .userId(userId)
                .languageCode(code)
                .type(request.type())
                .level(level)
                .build());
        return UserLanguageResponse.from(saved);
    }

    @Transactional
    public void remove(UUID userId, UUID languageId) {
        UserLanguage language = userLanguageRepository.findById(languageId)
                .filter(l -> l.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Language entry not found."));
        userLanguageRepository.delete(language);
    }
}
