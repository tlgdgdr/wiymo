package com.socialworld.app.friendship.dto;

import com.socialworld.app.avatar.dto.AvatarResponse;
import com.socialworld.app.friendship.Connection;
import com.socialworld.app.friendship.ConnectionStatus;
import com.socialworld.app.user.User;

import java.time.Instant;
import java.util.UUID;

/** A connection as seen by one side; incoming means the other user asked. */
public record ConnectionResponse(
        UUID id,
        UUID partnerId,
        String partnerUsername,
        boolean partnerOnline,
        AvatarResponse partnerAvatar,
        ConnectionStatus status,
        boolean incoming,
        Instant createdAt
) {
    public static ConnectionResponse from(Connection connection, UUID viewerId,
                                          User partner, AvatarResponse partnerAvatar) {
        return new ConnectionResponse(
                connection.getId(),
                partner.getId(),
                partner.getUsername(),
                partner.isOnline(),
                partnerAvatar,
                connection.getStatus(),
                connection.getAddresseeId().equals(viewerId),
                connection.getCreatedAt());
    }
}
