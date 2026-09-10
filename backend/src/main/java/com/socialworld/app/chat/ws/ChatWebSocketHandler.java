package com.socialworld.app.chat.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialworld.app.chat.ChatService;
import com.socialworld.app.chat.MessageType;
import com.socialworld.app.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.UUID;

/**
 * Minimal JSON protocol over a raw WebSocket:
 *   client -> server: {"type": "chat.send", "receiverId": "<uuid>", "content": "..."}
 *   server -> client: {"type": "message", "payload": MessageResponse}
 *                     {"type": "error",   "payload": {"code": "...", "message": "..."}}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatSessionRegistry sessionRegistry;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(userId(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(userId(session), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        UUID senderId = userId(session);
        try {
            JsonNode node = objectMapper.readTree(textMessage.getPayload());
            String type = node.path("type").asText("");
            if ("chat.send".equals(type)) {
                UUID receiverId = UUID.fromString(node.path("receiverId").asText());
                String content = node.path("content").asText("");
                chatService.sendMessage(senderId, receiverId, content, MessageType.TEXT);
            } else if (!"ping".equals(type)) {
                sendError(session, "VALIDATION_ERROR", "Unknown message type: " + type);
            }
        } catch (ApiException e) {
            sendError(session, e.getCode().name(), e.getMessage());
        } catch (Exception e) {
            log.debug("Malformed WebSocket message from {}: {}", senderId, e.getMessage());
            sendError(session, "VALIDATION_ERROR", "Malformed message.");
        }
    }

    private void sendError(WebSocketSession session, String code, String message) {
        try {
            String json = objectMapper.writeValueAsString(
                    new ChatService.WsEvent<>("error", Map.of("code", code, "message", message)));
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.debug("Failed to send WebSocket error: {}", e.getMessage());
        }
    }

    private UUID userId(WebSocketSession session) {
        return (UUID) session.getAttributes().get(AuthHandshakeInterceptor.USER_ID_ATTRIBUTE);
    }
}
