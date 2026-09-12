package com.socialworld.app.chat.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialworld.app.chat.ChatService;
import com.socialworld.app.chat.MessageType;
import com.socialworld.app.common.exception.ApiException;
import com.socialworld.app.game.GameService;
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
 *                     {"type": "game.move", "gameId": "<uuid>", "cell": 0-8}
 *   server -> client: {"type": "message",    "payload": MessageResponse}
 *                     {"type": "game.state", "payload": GameStateResponse}
 *                     {"type": "error",      "payload": {"code": "...", "message": "..."}}
 *
 * Everything a client sends is an intent, never state: the server decides what
 * it means and pushes the result back to both sides.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatSessionRegistry sessionRegistry;
    private final ChatService chatService;
    private final GameService gameService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(userId(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = userId(session);
        sessionRegistry.unregister(userId, session);
        // Only once their last socket is gone: a phone switching networks, or a
        // second device, must not cost someone a game they are still playing.
        if (!sessionRegistry.isOnline(userId)) {
            try {
                gameService.forfeitAll(userId);
            } catch (Exception e) {
                log.warn("Failed to close games for disconnected user {}: {}", userId, e.getMessage());
            }
        }
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
            } else if ("game.move".equals(type)) {
                UUID gameId = UUID.fromString(node.path("gameId").asText());
                gameService.play(senderId, gameId, node.path("cell").asInt(-1));
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
