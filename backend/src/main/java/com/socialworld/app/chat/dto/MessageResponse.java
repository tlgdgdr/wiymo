package com.socialworld.app.chat.dto;

import com.socialworld.app.chat.Message;
import com.socialworld.app.chat.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        UUID receiverId,
        String content,
        MessageType messageType,
        Instant createdAt,
        Instant readAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt(),
                message.getReadAt());
    }
}
