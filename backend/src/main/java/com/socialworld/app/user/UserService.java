package com.socialworld.app.user;

import com.socialworld.app.auth.AuthService;
import com.socialworld.app.avatar.AvatarRepository;
import com.socialworld.app.avatar.AvatarService;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.intention.Intention;
import com.socialworld.app.language.UserLanguageRepository;
import com.socialworld.app.language.UserLanguageService;
import com.socialworld.app.room.RoomPresenceRepository;
import com.socialworld.app.moderation.BlockService;
import com.socialworld.app.user.dto.MeResponse;
import com.socialworld.app.user.dto.PublicProfileResponse;
import com.socialworld.app.user.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserLanguageService userLanguageService;
    private final AvatarService avatarService;
    private final BlockService blockService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final AvatarRepository avatarRepository;
    private final UserLanguageRepository userLanguageRepository;
    private final RoomPresenceRepository roomPresenceRepository;

    @Transactional(readOnly = true)
    public MeResponse getMe(UUID userId) {
        User user = requireUser(userId);
        return MeResponse.from(user, userLanguageService.listForUser(userId), avatarService.getForUser(userId));
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
        return MeResponse.from(user, userLanguageService.listForUser(userId), avatarService.getForUser(userId));
    }

    @Transactional
    public MeResponse updateIntention(UUID userId, Intention intention) {
        User user = requireUser(userId);
        user.setCurrentIntention(intention);
        return MeResponse.from(user, userLanguageService.listForUser(userId), avatarService.getForUser(userId));
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(UUID viewerId, UUID id) {
        if (!viewerId.equals(id) && blockService.isBlockedEitherWay(viewerId, id)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found.");
        }
        User user = userRepository.findById(id)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        return PublicProfileResponse.from(user, userLanguageService.listForUser(id), avatarService.getForUser(id));
    }

    @Transactional
    public void registerPushToken(UUID userId, String token) {
        requireUser(userId).setExpoPushToken(token);
    }

    /**
     * GDPR-style deletion: password re-check, then anonymize the account in
     * place (messages keep referential integrity but point at a shell),
     * strip avatar/languages/presence and kill every session.
     */
    @Transactional
    public void deleteAccount(UUID userId, String rawPassword) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        user.setStatus(UserStatus.DELETED);
        user.setUsername("deleted_" + suffix);
        user.setEmail("deleted_" + suffix + "@deleted.invalid");
        user.setBio(null);
        user.setGender(null);
        user.setCountryCode(null);
        user.setCurrentIntention(null);
        user.setOnline(false);
        user.setExpoPushToken(null);

        avatarRepository.deleteById(userId);
        userLanguageRepository.deleteAll(
                userLanguageRepository.findByUserIdOrderByCreatedAt(userId));
        roomPresenceRepository.deleteById(userId);
        authService.revokeAllSessions(userId);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
    }
}
