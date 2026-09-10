package com.socialworld.app.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select m from Message m
            where (m.senderId = :a and m.receiverId = :b)
               or (m.senderId = :b and m.receiverId = :a)
            order by m.createdAt desc
            """)
    List<Message> findConversation(@Param("a") UUID a, @Param("b") UUID b, Pageable pageable);

    @Query("""
            select m from Message m
            where ((m.senderId = :a and m.receiverId = :b)
               or (m.senderId = :b and m.receiverId = :a))
              and m.createdAt < :before
            order by m.createdAt desc
            """)
    List<Message> findConversationBefore(@Param("a") UUID a, @Param("b") UUID b,
                                         @Param("before") Instant before, Pageable pageable);

    /** Latest message per conversation partner (PostgreSQL DISTINCT ON). */
    @Query(value = """
            SELECT id, sender_id, receiver_id, content, message_type, created_at, read_at
            FROM (
                SELECT DISTINCT ON (x.partner) x.*
                FROM (
                    SELECT m.*,
                           CASE WHEN m.sender_id = :uid THEN m.receiver_id ELSE m.sender_id END AS partner
                    FROM messages m
                    WHERE m.sender_id = :uid OR m.receiver_id = :uid
                ) x
                ORDER BY x.partner, x.created_at DESC
            ) t
            """, nativeQuery = true)
    List<Message> findLatestMessagePerPartner(@Param("uid") UUID uid);

    @Query("""
            select m.senderId, count(m) from Message m
            where m.receiverId = :uid and m.readAt is null
            group by m.senderId
            """)
    List<Object[]> countUnreadBySender(@Param("uid") UUID uid);

    @Modifying
    @Query("""
            update Message m set m.readAt = :now
            where m.senderId = :partnerId and m.receiverId = :uid and m.readAt is null
            """)
    int markConversationRead(@Param("uid") UUID uid,
                             @Param("partnerId") UUID partnerId,
                             @Param("now") Instant now);
}
