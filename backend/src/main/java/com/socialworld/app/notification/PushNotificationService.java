package com.socialworld.app.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialworld.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Fire-and-forget pushes through Expo's push API — free, no Firebase/APNs
 * setup while the app runs inside Expo. Failures are logged and swallowed:
 * a push must never break a chat or gift transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private static final URI EXPO_PUSH_API = URI.create("https://exp.host/--/api/v2/push/send");

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${app.push.enabled:true}")
    private boolean enabled;

    /** Sends asynchronously if the user has registered a push token. */
    public void notifyUser(UUID userId, String title, String body) {
        if (!enabled) {
            return;
        }
        userRepository.findById(userId)
                .map(u -> u.getExpoPushToken())
                .filter(token -> token != null && !token.isBlank())
                .ifPresent(token -> send(token, title, body));
    }

    private void send(String token, String title, String body) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "to", token,
                    "title", title,
                    "body", body,
                    "sound", "default"));
            HttpRequest request = HttpRequest.newBuilder(EXPO_PUSH_API)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((response, error) -> {
                        if (error != null) {
                            log.debug("Push delivery failed: {}", error.getMessage());
                        } else if (response.statusCode() >= 400) {
                            log.debug("Expo push API returned {}: {}", response.statusCode(), response.body());
                        }
                    });
        } catch (Exception e) {
            log.debug("Push serialization failed: {}", e.getMessage());
        }
    }
}
