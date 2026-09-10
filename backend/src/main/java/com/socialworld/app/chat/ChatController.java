package com.socialworld.app.chat;

import com.socialworld.app.auth.AuthenticatedUser;
import com.socialworld.app.chat.dto.ConversationResponse;
import com.socialworld.app.chat.dto.MessageResponse;
import com.socialworld.app.chat.dto.SendMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Chat")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "My conversations, newest first, with unread counts")
    public List<ConversationResponse> conversations(@AuthenticationPrincipal AuthenticatedUser user) {
        return chatService.getConversations(user.id());
    }

    @GetMapping("/{userId}/messages")
    @Operation(summary = "Messages with a user (newest first, 50 per page via ?before=); marks them read")
    public List<MessageResponse> messages(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable UUID userId,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before) {
        return chatService.getConversationMessages(user.id(), userId, before);
    }

    @PostMapping("/{userId}/messages")
    @Operation(summary = "Send a text message (REST fallback; WebSocket /ws/chat is the live path)")
    public ResponseEntity<MessageResponse> send(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable UUID userId,
                                                @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(user.id(), userId, request.content(), MessageType.TEXT));
    }
}
