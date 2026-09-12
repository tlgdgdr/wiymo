package com.socialworld.app.config;

import com.socialworld.app.avatar.Avatar;
import com.socialworld.app.avatar.AvatarRepository;
import com.socialworld.app.chat.Message;
import com.socialworld.app.chat.MessageRepository;
import com.socialworld.app.chat.MessageType;
import com.socialworld.app.intention.Intention;
import com.socialworld.app.language.LanguageLevel;
import com.socialworld.app.language.LanguageType;
import com.socialworld.app.language.UserLanguage;
import com.socialworld.app.language.UserLanguageRepository;
import com.socialworld.app.room.Room;
import com.socialworld.app.room.RoomPresence;
import com.socialworld.app.room.RoomPresenceRepository;
import com.socialworld.app.room.RoomRepository;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import com.socialworld.app.wallet.Wallet;
import com.socialworld.app.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Local development only: ten demo accounts (password "password123"),
 * spread across intentions, languages and rooms, with a starter
 * conversation. Rooms, avatar assets and gifts are seeded by Flyway.
 * Never active outside the "local" profile.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserLanguageRepository userLanguageRepository;
    private final AvatarRepository avatarRepository;
    private final WalletRepository walletRepository;
    private final RoomRepository roomRepository;
    private final RoomPresenceRepository roomPresenceRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    private record Demo(String username, int age, String country, Intention intention,
                        String bio, String nativeLang, String otherLang, LanguageType otherType,
                        String body, String hair, String top) {
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsernameIgnoreCase("mira")) {
            return; // already seeded
        }
        String passwordHash = passwordEncoder.encode("password123");

        List<Demo> demos = List.of(
                new Demo("mira", 26, "TR", Intention.DEEP_TALK, "Gece insanıyım. Kahve ve uzun sohbetler.", "tr", "en", LanguageType.SPEAKING, "body_01", "hair_02", "top_01"),
                new Demo("leo", 29, "ES", Intention.PLAY_GAMES, "Undefeated at tic-tac-toe. Prove me wrong.", "es", "tr", LanguageType.LEARNING, "body_02", "hair_04", "top_02"),
                new Demo("ada", 24, "GB", Intention.CHILL, "Just here to vibe.", "en", null, null, "body_03", "hair_03", "top_03"),
                new Demo("nova", 31, "DE", Intention.FLIRT, "Ask me about my dog.", "de", "en", LanguageType.SPEAKING, "body_01", "hair_02", "top_02"),
                new Demo("kenji", 27, "JP", Intention.MEET_PEOPLE, "New in town, say hi!", "ja", "en", LanguageType.LEARNING, "body_02", "hair_01", "top_01"),
                new Demo("sofia", 23, "IT", Intention.CASUAL_CHAT, "Espresso opinions, strongly held.", "it", "en", LanguageType.SPEAKING, "body_01", "hair_02", "top_03"),
                new Demo("omar", 33, "EG", Intention.DEEP_TALK, "Philosophy and football.", "ar", "en", LanguageType.SPEAKING, "body_03", "hair_04", "top_01"),
                new Demo("lena", 28, "PL", Intention.PLAY_GAMES, "One more round, then I really am going to bed.", "pl", "tr", LanguageType.LEARNING, "body_01", "hair_03", "top_02"),
                new Demo("marco", 30, "BR", Intention.CHILL, "Music, memes, mate.", "pt", "en", LanguageType.SPEAKING, "body_02", "hair_01", "top_03"),
                new Demo("yuki", 25, "JP", Intention.FLIRT, "Cat person. Non-negotiable.", "ja", "en", LanguageType.SPEAKING, "body_01", "hair_02", "top_01"));

        List<Room> rooms = roomRepository.findByActiveTrueOrderByName();
        int seeded = 0;

        User previous = null;
        for (int i = 0; i < demos.size(); i++) {
            Demo demo = demos.get(i);
            boolean online = i % 2 == 0;
            User user = userRepository.save(User.builder()
                    .username(demo.username())
                    .email(demo.username() + "@demo.local")
                    .passwordHash(passwordHash)
                    .birthDate(LocalDate.now().minusYears(demo.age()).minusMonths(3))
                    .countryCode(demo.country())
                    .bio(demo.bio())
                    .currentIntention(demo.intention())
                    .online(online)
                    .lastSeenAt(Instant.now().minus(online ? 5 : 30L * (i + 1), ChronoUnit.MINUTES))
                    .status(UserStatus.ACTIVE)
                    .build());

            userLanguageRepository.save(language(user.getId(), demo.nativeLang(), LanguageType.NATIVE, LanguageLevel.NATIVE));
            if (demo.otherLang() != null) {
                userLanguageRepository.save(language(user.getId(), demo.otherLang(), demo.otherType(),
                        demo.otherType() == LanguageType.LEARNING ? LanguageLevel.A2 : LanguageLevel.B2));
            }

            avatarRepository.save(Avatar.builder()
                    .userId(user.getId())
                    .bodyKey(demo.body())
                    .faceKey("face_0" + (i % 3 + 1))
                    .eyesKey("eyes_0" + (i % 2 + 1))
                    .hairKey(demo.hair())
                    .topKey(demo.top())
                    .bottomKey("bottom_0" + (i % 3 + 1))
                    .shoesKey("shoes_0" + (i % 2 + 1))
                    .accessoryKey(i % 4 == 0 ? "glasses_01" : null)
                    .build());

            walletRepository.save(Wallet.builder().userId(user.getId()).coinBalance(100).build());

            // Online users sit in rooms matching their mood where possible.
            if (online && !rooms.isEmpty()) {
                Room room = rooms.stream()
                        .filter(r -> r.getIntention() == demo.intention())
                        .findFirst()
                        .orElse(rooms.get(i % rooms.size()));
                long occupied = roomPresenceRepository.countByRoomId(room.getId());
                roomPresenceRepository.save(RoomPresence.builder()
                        .userId(user.getId())
                        .roomId(room.getId())
                        .slotIndex((int) occupied)
                        .build());
            }

            // A short starter conversation between consecutive demo users.
            if (previous != null && i % 3 == 1) {
                messageRepository.save(message(previous.getId(), user.getId(), "hey! nasıl gidiyor?"));
                messageRepository.save(message(user.getId(), previous.getId(), "iyi! sen de buralarda mısın? ☕"));
            }
            previous = user;
            seeded++;
        }

        log.info("Seeded {} demo users (password: password123, e.g. mira / leo / ada)", seeded);
    }

    private UserLanguage language(UUID userId, String code, LanguageType type, LanguageLevel level) {
        return UserLanguage.builder().userId(userId).languageCode(code).type(type).level(level).build();
    }

    private Message message(UUID from, UUID to, String content) {
        return Message.builder().senderId(from).receiverId(to).content(content)
                .messageType(MessageType.TEXT).build();
    }
}
