package com.socialworld.app.friendship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {

    /** The live connection between two users in either direction, if any. */
    @Query("""
            select c from Connection c
            where ((c.requesterId = :a and c.addresseeId = :b)
               or (c.requesterId = :b and c.addresseeId = :a))
              and c.status in (com.socialworld.app.friendship.ConnectionStatus.PENDING,
                               com.socialworld.app.friendship.ConnectionStatus.ACCEPTED,
                               com.socialworld.app.friendship.ConnectionStatus.BLOCKED)
            """)
    Optional<Connection> findActivePair(@Param("a") UUID a, @Param("b") UUID b);

    @Query("""
            select c from Connection c
            where c.requesterId = :userId or c.addresseeId = :userId
            order by c.updatedAt desc
            """)
    List<Connection> findAllForUser(@Param("userId") UUID userId);
}
