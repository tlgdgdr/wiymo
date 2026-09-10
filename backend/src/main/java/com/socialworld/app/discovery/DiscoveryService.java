package com.socialworld.app.discovery;

import com.socialworld.app.avatar.AvatarService;
import com.socialworld.app.discovery.dto.DiscoveredUserResponse;
import com.socialworld.app.intention.Intention;
import com.socialworld.app.language.LanguageType;
import com.socialworld.app.language.UserLanguage;
import com.socialworld.app.language.UserLanguageRepository;
import com.socialworld.app.language.dto.UserLanguageResponse;
import com.socialworld.app.room.RoomPresence;
import com.socialworld.app.room.RoomPresenceRepository;
import com.socialworld.app.user.User;
import com.socialworld.app.user.UserRepository;
import com.socialworld.app.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simple weighted scoring over a bounded candidate pool — deliberately no ML.
 * Priority per spec: same intention > online > language compatibility >
 * shared room > recently active.
 */
@Service
@RequiredArgsConstructor
public class DiscoveryService {

    static final int SCORE_SAME_INTENTION = 50;
    static final int SCORE_ONLINE = 30;
    static final int SCORE_LANGUAGE_EXCHANGE = 25;
    static final int SCORE_SHARED_LANGUAGE = 15;
    static final int SCORE_SHARED_ROOM = 20;
    static final int SCORE_ACTIVE_LAST_HOUR = 10;
    static final int SCORE_ACTIVE_LAST_DAY = 5;
    static final int MAX_RESULTS = 20;

    private final UserRepository userRepository;
    private final UserLanguageRepository userLanguageRepository;
    private final RoomPresenceRepository roomPresenceRepository;
    private final AvatarService avatarService;

    public record Filters(
            Intention intention,
            String languageCode,
            String countryCode,
            boolean onlineOnly,
            Integer ageMin,
            Integer ageMax
    ) {
    }

    @Transactional(readOnly = true)
    public List<DiscoveredUserResponse> discover(UUID viewerId, Filters filters) {
        User viewer = userRepository.findById(viewerId).orElse(null);
        Intention targetIntention = filters.intention() != null
                ? filters.intention()
                : viewer != null ? viewer.getCurrentIntention() : null;

        List<User> candidates = userRepository
                .findDiscoveryCandidates(UserStatus.ACTIVE).stream()
                .filter(u -> !u.getId().equals(viewerId))
                .filter(u -> !filters.onlineOnly() || u.isOnline())
                .filter(u -> filters.countryCode() == null
                        || filters.countryCode().equalsIgnoreCase(u.getCountryCode()))
                .filter(u -> matchesAge(u, filters.ageMin(), filters.ageMax()))
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<UUID> candidateIds = candidates.stream().map(User::getId).toList();
        Map<UUID, List<UserLanguage>> languagesByUser = userLanguageRepository
                .findByUserIdIn(candidateIds).stream()
                .collect(Collectors.groupingBy(UserLanguage::getUserId));
        List<UserLanguage> viewerLanguages = userLanguageRepository
                .findByUserIdOrderByCreatedAt(viewerId);

        // Language filter applies after loading languages.
        String languageFilter = filters.languageCode() == null
                ? null : filters.languageCode().toLowerCase();

        Set<UUID> sameRoomUserIds = roomPresenceRepository.findByUserId(viewerId)
                .map(p -> roomPresenceRepository.findByRoomIdOrderBySlotIndex(p.getRoomId()).stream()
                        .map(RoomPresence::getUserId)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());

        record Scored(User user, int score) {
        }

        List<Scored> scored = new ArrayList<>();
        for (User candidate : candidates) {
            List<UserLanguage> candidateLanguages =
                    languagesByUser.getOrDefault(candidate.getId(), List.of());
            if (languageFilter != null && candidateLanguages.stream()
                    .noneMatch(l -> l.getLanguageCode().equals(languageFilter))) {
                continue;
            }
            scored.add(new Scored(candidate, score(
                    targetIntention, viewerLanguages, candidate, candidateLanguages,
                    sameRoomUserIds)));
        }

        return scored.stream()
                .sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparing(s -> s.user().getLastSeenAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_RESULTS)
                .map(s -> DiscoveredUserResponse.from(
                        s.user(),
                        languagesByUser.getOrDefault(s.user().getId(), List.of()).stream()
                                .map(UserLanguageResponse::from)
                                .toList(),
                        avatarService.getForUser(s.user().getId()),
                        s.score()))
                .toList();
    }

    private int score(Intention targetIntention, List<UserLanguage> viewerLanguages,
                      User candidate, List<UserLanguage> candidateLanguages,
                      Set<UUID> sameRoomUserIds) {
        int score = 0;

        if (targetIntention != null && targetIntention == candidate.getCurrentIntention()) {
            score += SCORE_SAME_INTENTION;
        }
        if (candidate.isOnline()) {
            score += SCORE_ONLINE;
        }
        score += languageScore(viewerLanguages, candidateLanguages);
        if (sameRoomUserIds.contains(candidate.getId())) {
            score += SCORE_SHARED_ROOM;
        }
        Instant lastSeen = candidate.getLastSeenAt();
        if (lastSeen != null) {
            Duration since = Duration.between(lastSeen, Instant.now());
            if (since.compareTo(Duration.ofHours(1)) <= 0) {
                score += SCORE_ACTIVE_LAST_HOUR;
            } else if (since.compareTo(Duration.ofHours(24)) <= 0) {
                score += SCORE_ACTIVE_LAST_DAY;
            }
        }
        return score;
    }

    /**
     * Exchange fit beats mere overlap: my NATIVE is their LEARNING (or the
     * reverse) scores highest; any shared language code still counts.
     */
    private int languageScore(List<UserLanguage> viewerLanguages,
                              List<UserLanguage> candidateLanguages) {
        if (viewerLanguages.isEmpty() || candidateLanguages.isEmpty()) {
            return 0;
        }
        Set<String> viewerNative = codesOf(viewerLanguages, LanguageType.NATIVE);
        Set<String> viewerLearning = codesOf(viewerLanguages, LanguageType.LEARNING);
        Set<String> candidateNative = codesOf(candidateLanguages, LanguageType.NATIVE);
        Set<String> candidateLearning = codesOf(candidateLanguages, LanguageType.LEARNING);

        boolean exchange = viewerNative.stream().anyMatch(candidateLearning::contains)
                || candidateNative.stream().anyMatch(viewerLearning::contains);
        if (exchange) {
            return SCORE_LANGUAGE_EXCHANGE;
        }

        Set<String> viewerAll = viewerLanguages.stream()
                .map(UserLanguage::getLanguageCode).collect(Collectors.toSet());
        boolean shared = candidateLanguages.stream()
                .map(UserLanguage::getLanguageCode)
                .anyMatch(viewerAll::contains);
        return shared ? SCORE_SHARED_LANGUAGE : 0;
    }

    private Set<String> codesOf(List<UserLanguage> languages, LanguageType type) {
        return languages.stream()
                .filter(l -> l.getType() == type)
                .map(UserLanguage::getLanguageCode)
                .collect(Collectors.toSet());
    }

    private boolean matchesAge(User user, Integer ageMin, Integer ageMax) {
        if (ageMin == null && ageMax == null) {
            return true;
        }
        int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
        return (ageMin == null || age >= ageMin) && (ageMax == null || age <= ageMax);
    }
}
