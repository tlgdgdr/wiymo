package com.socialworld.app.chat.dto;

import com.socialworld.app.avatar.dto.AvatarResponse;

import java.util.UUID;

/** One row in the chat list: the partner plus the latest message. */
public record ConversationResponse(
        UUID partnerId,
        String partnerUsername,
        boolean partnerOnline,
        AvatarResponse partnerAvatar,
        MessageResponse lastMessage,
        long unreadCount
) {
}
