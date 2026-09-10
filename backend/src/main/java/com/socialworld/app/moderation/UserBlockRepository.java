package com.socialworld.app.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    Optional<UserBlock> findByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId);

    /** True when either side has blocked the other. */
    @Query("""
            select count(b) > 0 from UserBlock b
            where (b.blockerId = :a and b.blockedUserId = :b)
               or (b.blockerId = :b and b.blockedUserId = :a)
            """)
    boolean existsBetween(@Param("a") UUID a, @Param("b") UUID b);

    /** Everyone involved in a block with this user, in either direction. */
    @Query("""
            select case when b.blockerId = :userId then b.blockedUserId else b.blockerId end
            from UserBlock b
            where b.blockerId = :userId or b.blockedUserId = :userId
            """)
    List<UUID> findBlockedPartnerIds(@Param("userId") UUID userId);
}
