package com.socialworld.app.gift;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Immutable record of one gift being sent; the audit trail for coins. */
@Entity
@Table(name = "gift_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftTransaction {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "gift_id", nullable = false)
    private UUID giftId;

    /** Price at the time of sending, in case gift prices change later. */
    @Column(name = "coin_amount", nullable = false)
    private int coinAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
