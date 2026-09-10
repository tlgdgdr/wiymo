package com.socialworld.app.language;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserLanguageRepository extends JpaRepository<UserLanguage, UUID> {

    List<UserLanguage> findByUserIdOrderByCreatedAt(UUID userId);

    boolean existsByUserIdAndLanguageCodeAndType(UUID userId, String languageCode, LanguageType type);

    long countByUserId(UUID userId);

    List<UserLanguage> findByUserIdIn(Collection<UUID> userIds);
}
