package com.socialworld.app.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    /** Discovery candidate pool: recent actives first (MVP-scale). */
    @Query(value = """
            SELECT * FROM users
            WHERE status = :#{#status.name()}
            ORDER BY last_seen_at DESC NULLS LAST
            LIMIT 200
            """, nativeQuery = true)
    List<User> findDiscoveryCandidates(@Param("status") UserStatus status);
}
