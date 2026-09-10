package com.socialworld.app.user;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.intention.Intention;
import com.socialworld.app.language.UserLanguageService;
import com.socialworld.app.user.dto.MeResponse;
import com.socialworld.app.user.dto.PublicProfileResponse;
import com.socialworld.app.user.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserLanguageService userLanguageService;

    @Transactional(readOnly = true)
    public MeResponse getMe(UUID userId) {
        User user = requireUser(userId);
        return MeResponse.from(user, userLanguageService.listForUser(userId));
    }

    @Transactional
    public MeResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        if (request.bio() != null) {
            user.setBio(request.bio().isBlank() ? null : request.bio().trim());
        }
        if (request.countryCode() != null) {
            user.setCountryCode(request.countryCode());
        }
        if (request.gender() != null) {
            user.setGender(request.gender().isBlank() ? null : request.gender().trim());
        }
        return MeResponse.from(user, userLanguageService.listForUser(userId));
    }

    @Transactional
    public MeResponse updateIntention(UUID userId, Intention intention) {
        User user = requireUser(userId);
        user.setCurrentIntention(intention);
        return MeResponse.from(user, userLanguageService.listForUser(userId));
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        return PublicProfileResponse.from(user, userLanguageService.listForUser(id));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
    }
}
