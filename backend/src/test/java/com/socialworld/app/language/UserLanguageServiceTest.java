package com.socialworld.app.language;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.language.dto.AddLanguageRequest;
import com.socialworld.app.language.dto.UserLanguageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLanguageServiceTest {

    @Mock
    private UserLanguageRepository repository;

    @InjectMocks
    private UserLanguageService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void add_normalizesCodeAndSaves() {
        when(repository.existsByUserIdAndLanguageCodeAndType(userId, "pl", LanguageType.LEARNING)).thenReturn(false);
        when(repository.countByUserId(userId)).thenReturn(2L);
        when(repository.save(any(UserLanguage.class))).thenAnswer(inv -> inv.getArgument(0));

        UserLanguageResponse response = service.add(userId,
                new AddLanguageRequest("PL", LanguageType.LEARNING, LanguageLevel.A2));

        assertThat(response.languageCode()).isEqualTo("pl");
        assertThat(response.level()).isEqualTo(LanguageLevel.A2);
    }

    @Test
    void add_nativeForcesNativeLevel() {
        when(repository.existsByUserIdAndLanguageCodeAndType(userId, "tr", LanguageType.NATIVE)).thenReturn(false);
        when(repository.countByUserId(userId)).thenReturn(0L);
        when(repository.save(any(UserLanguage.class))).thenAnswer(inv -> inv.getArgument(0));

        UserLanguageResponse response = service.add(userId,
                new AddLanguageRequest("tr", LanguageType.NATIVE, LanguageLevel.A1));

        assertThat(response.level()).isEqualTo(LanguageLevel.NATIVE);
    }

    @Test
    void add_nonNativeRequiresCefrLevel() {
        assertThatThrownBy(() -> service.add(userId,
                new AddLanguageRequest("en", LanguageType.SPEAKING, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> service.add(userId,
                new AddLanguageRequest("en", LanguageType.SPEAKING, LanguageLevel.NATIVE)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void add_rejectsDuplicate() {
        when(repository.existsByUserIdAndLanguageCodeAndType(userId, "en", LanguageType.SPEAKING)).thenReturn(true);

        assertThatThrownBy(() -> service.add(userId,
                new AddLanguageRequest("en", LanguageType.SPEAKING, LanguageLevel.B2)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.LANGUAGE_ALREADY_ADDED);
        verify(repository, never()).save(any());
    }

    @Test
    void add_rejectsWhenLimitReached() {
        when(repository.existsByUserIdAndLanguageCodeAndType(any(), any(), any())).thenReturn(false);
        when(repository.countByUserId(userId)).thenReturn(10L);

        assertThatThrownBy(() -> service.add(userId,
                new AddLanguageRequest("en", LanguageType.SPEAKING, LanguageLevel.B2)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.LANGUAGE_LIMIT_REACHED);
    }

    @Test
    void remove_rejectsForeignEntry() {
        UserLanguage foreign = UserLanguage.builder()
                .userId(UUID.randomUUID())
                .languageCode("en")
                .type(LanguageType.SPEAKING)
                .level(LanguageLevel.B2)
                .build();
        when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.remove(userId, foreign.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        verify(repository, never()).delete(any());
    }

    @Test
    void remove_deletesOwnEntry() {
        UserLanguage own = UserLanguage.builder()
                .userId(userId)
                .languageCode("en")
                .type(LanguageType.SPEAKING)
                .level(LanguageLevel.B2)
                .build();
        when(repository.findById(own.getId())).thenReturn(Optional.of(own));

        service.remove(userId, own.getId());

        verify(repository).delete(own);
    }
}
