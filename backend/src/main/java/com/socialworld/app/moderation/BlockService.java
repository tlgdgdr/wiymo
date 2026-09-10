package com.socialworld.app.moderation;

import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.common.exception.ErrorCode;
import com.socialworld.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The single authority on who may interact with whom. Chat, gifts,
 * profiles, discovery, rooms and connections all consult this service.
 */
@Service
@RequiredArgsConstructor
public class BlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    @Transactional
    public void block(UUID blockerId, UUID targetId) {
        if (blockerId.equals(targetId)) {
            throw new ApiException(ErrorCode.SELF_ACTION_NOT_ALLOWED, "You cannot block yourself.");
        }
        userRepository.findById(targetId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        // Idempotent: blocking twice is fine.
        if (userBlockRepository.findByBlockerIdAndBlockedUserId(blockerId, targetId).isEmpty()) {
            userBlockRepository.save(UserBlock.builder()
                    .blockerId(blockerId)
                    .blockedUserId(targetId)
                    .build());
        }
    }

    @Transactional
    public void unblock(UUID blockerId, UUID targetId) {
        userBlockRepository.findByBlockerIdAndBlockedUserId(blockerId, targetId)
                .ifPresent(userBlockRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(UUID a, UUID b) {
        return userBlockRepository.existsBetween(a, b);
    }

    /**
     * Guard used by interaction paths (chat, gifts, connections). The error
     * is deliberately neutral so it doesn't reveal who blocked whom.
     */
    @Transactional(readOnly = true)
    public void assertInteractionAllowed(UUID a, UUID b) {
        if (isBlockedEitherWay(a, b)) {
            throw new ApiException(ErrorCode.USER_UNAVAILABLE);
        }
    }

    @Transactional(readOnly = true)
    public Set<UUID> blockedPartnerIds(UUID userId) {
        return userBlockRepository.findBlockedPartnerIds(userId).stream()
                .collect(Collectors.toSet());
    }
}
