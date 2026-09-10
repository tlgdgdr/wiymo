package com.socialworld.app.avatar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A user's avatar: one asset key per layer category. Body is always set;
 * the other layers are optional.
 */
@Entity
@Table(name = "avatars")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Avatar {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "body_key", nullable = false, length = 50)
    private String bodyKey;

    @Column(name = "face_key", length = 50)
    private String faceKey;

    @Column(name = "eyes_key", length = 50)
    private String eyesKey;

    @Column(name = "hair_key", length = 50)
    private String hairKey;

    @Column(name = "top_key", length = 50)
    private String topKey;

    @Column(name = "bottom_key", length = 50)
    private String bottomKey;

    @Column(name = "shoes_key", length = 50)
    private String shoesKey;

    @Column(name = "accessory_key", length = 50)
    private String accessoryKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
