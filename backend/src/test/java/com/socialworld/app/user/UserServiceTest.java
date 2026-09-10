package com.socialworld.app.user;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.intention.Intention;
import com.socialworld.app.language.UserLanguageService;
import com.socialworld.app.user.dto.MeResponse;
import com.socialworld.app.user.dto.PublicProfileResponse;
import com.socialworld.app.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserLanguageService userLanguageService;

    @InjectMocks
    private UserService userService;

    private User user(UserStatus status) {
        return User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hash")
                .birthDate(LocalDate.now().minusYears(25).minusDays(1))
                .status(status)
                .build();
    }

    @Test
    void updateIntention_setsIntention() {
        User user = user(UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userLanguageService.listForUser(any())).thenReturn(List.of());

        MeResponse response = userService.updateIntention(user.getId(), Intention.DEEP_TALK);

        assertThat(user.getCurrentIntention()).isEqualTo(Intention.DEEP_TALK);
        assertThat(response.currentIntention()).isEqualTo(Intention.DEEP_TALK);
    }

    @Test
    void publicProfile_exposesAgeButNeverBirthDateOrEmail() {
        User user = user(UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userLanguageService.listForUser(any())).thenReturn(List.of());

        PublicProfileResponse profile = userService.getPublicProfile(user.getId());

        assertThat(profile.age()).isEqualTo(25);
        assertThat(profile.username()).isEqualTo("alice");
        // Compile-time guarantee: PublicProfileResponse has no email/birthDate
        // fields; assert the serialized contract stays that way.
        assertThat(PublicProfileResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("email", "birthDate", "passwordHash");
    }

    @Test
    void publicProfile_hidesNonActiveUsers() {
        User user = user(UserStatus.SUSPENDED);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getPublicProfile(user.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void updateProfile_appliesOnlyProvidedFields() {
        User user = user(UserStatus.ACTIVE);
        user.setBio("old bio");
        user.setCountryCode("TR");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userLanguageService.listForUser(any())).thenReturn(List.of());

        userService.updateProfile(user.getId(), new UpdateProfileRequest(null, "PL", null));

        assertThat(user.getBio()).isEqualTo("old bio");
        assertThat(user.getCountryCode()).isEqualTo("PL");
    }

    @Test
    void updateProfile_blankBioClearsIt() {
        User user = user(UserStatus.ACTIVE);
        user.setBio("old bio");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userLanguageService.listForUser(any())).thenReturn(List.of());

        userService.updateProfile(user.getId(), new UpdateProfileRequest("  ", null, null));

        assertThat(user.getBio()).isNull();
    }

    @Test
    void getMe_unknownUserThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getMe(id))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
