package com.socialworld.app.discovery;

import com.socialworld.app.avatar.AvatarService;
import com.socialworld.app.discovery.dto.DiscoveredUserResponse;
import com.socialworld.app.intention.Intention;
import com.socialworld.app.language.LanguageLevel;
import com.socialworld.app.language.LanguageType;
import com.socialworld.app.language.UserLanguage;
import com.socialworld.app.language.UserLanguageRepository;
import com.socialworld.app.room.RoomPresenceRepository;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserLanguageRepository userLanguageRepository;
    @Mock
    private RoomPresenceRepository roomPresenceRepository;
    @Mock
    private AvatarService avatarService;

    @InjectMocks
    private DiscoveryService discoveryService;

    private User viewer;

    private static final DiscoveryService.Filters NO_FILTERS =
            new DiscoveryService.Filters(null, null, null, false, null, null);

    @BeforeEach
    void setUp() {
        viewer = user("viewer", 28, Intention.DEEP_TALK, false, null);
        lenient().when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        lenient().when(roomPresenceRepository.findByUserId(viewer.getId())).thenReturn(Optional.empty());
        lenient().when(userLanguageRepository.findByUserIdOrderByCreatedAt(viewer.getId()))
                .thenReturn(List.of());
        lenient().when(userLanguageRepository.findByUserIdIn(any())).thenReturn(List.of());
    }

    private User user(String name, int age, Intention intention, boolean online, Instant lastSeen) {
        return User.builder()
                .username(name)
                .email(name + "@example.com")
                .passwordHash("hash")
                .birthDate(LocalDate.now().minusYears(age).minusDays(1))
                .currentIntention(intention)
                .online(online)
                .lastSeenAt(lastSeen)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private void candidates(User... users) {
        when(userRepository.findDiscoveryCandidates(UserStatus.ACTIVE))
                .thenReturn(List.of(users));
    }

    private UserLanguage lang(UUID userId, String code, LanguageType type) {
        return UserLanguage.builder().userId(userId).languageCode(code).type(type)
                .level(type == LanguageType.NATIVE ? LanguageLevel.NATIVE : LanguageLevel.B1).build();
    }

    @Test
    void excludesSelfFromResults() {
        candidates(viewer, user("other", 25, null, false, null));

        List<DiscoveredUserResponse> results = discoveryService.discover(viewer.getId(), NO_FILTERS);

        assertThat(results).extracting(DiscoveredUserResponse::username).containsExactly("other");
    }

    @Test
    void sameIntentionRanksAboveOnline() {
        User sameMood = user("samemood", 25, Intention.DEEP_TALK, false, null);
        User onlineOther = user("online", 25, Intention.CHILL, true, null);
        candidates(onlineOther, sameMood);

        List<DiscoveredUserResponse> results = discoveryService.discover(viewer.getId(), NO_FILTERS);

        assertThat(results.get(0).username()).isEqualTo("samemood");
        assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
    }

    @Test
    void onlineOnlyFilterHidesOfflineUsers() {
        candidates(user("offline", 25, null, false, null), user("online", 25, null, true, null));

        List<DiscoveredUserResponse> results = discoveryService.discover(viewer.getId(),
                new DiscoveryService.Filters(null, null, null, true, null, null));

        assertThat(results).extracting(DiscoveredUserResponse::username).containsExactly("online");
    }

    @Test
    void ageFilterApplies() {
        candidates(user("young", 20, null, false, null), user("older", 40, null, false, null));

        List<DiscoveredUserResponse> results = discoveryService.discover(viewer.getId(),
                new DiscoveryService.Filters(null, null, null, false, 30, 50));

        assertThat(results).extracting(DiscoveredUserResponse::username).containsExactly("older");
    }

    @Test
    void languageExchangeScoresAboveSharedLanguage() {
        User learner = user("learner", 25, null, false, null);   // learning my native
        User sharer = user("sharer", 25, null, false, null);     // just shares a language
        candidates(learner, sharer);

        when(userLanguageRepository.findByUserIdOrderByCreatedAt(viewer.getId())).thenReturn(List.of(
                lang(viewer.getId(), "tr", LanguageType.NATIVE),
                lang(viewer.getId(), "en", LanguageType.SPEAKING)));
        when(userLanguageRepository.findByUserIdIn(any())).thenReturn(List.of(
                lang(learner.getId(), "tr", LanguageType.LEARNING),
                lang(sharer.getId(), "en", LanguageType.SPEAKING)));

        List<DiscoveredUserResponse> results = discoveryService.discover(viewer.getId(), NO_FILTERS);

        assertThat(results.get(0).username()).isEqualTo("learner");
        assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
        assertThat(results.get(1).score()).isGreaterThan(0);
    }

    @Test
    void languageCodeFilterKeepsOnlySpeakers() {
        User polish = user("polish", 25, null, false, null);
        User other = user("other", 25, null, false, null);
        candidates(polish, other);
        when(userLanguageRepository.findByUserIdIn(any())).thenReturn(List.of(
                lang(polish.getId(), "pl", LanguageType.NATIVE)));

        List<DiscoveredUserResponse> results = discoveryService.discover(viewer.getId(),
                new DiscoveryService.Filters(null, "PL", null, false, null, null));

        assertThat(results).extracting(DiscoveredUserResponse::username).containsExactly("polish");
    }

    @Test
    void recentActivityBreaksTiesUpward() {
        User recent = user("recent", 25, null, false, Instant.now().minusSeconds(600));
        User stale = user("stale", 25, null, false, Instant.now().minusSeconds(60L * 60 * 48));
        candidates(stale, recent);

        List<DiscoveredUserResponse> results = discoveryService.discover(viewer.getId(), NO_FILTERS);

        assertThat(results.get(0).username()).isEqualTo("recent");
    }
}
