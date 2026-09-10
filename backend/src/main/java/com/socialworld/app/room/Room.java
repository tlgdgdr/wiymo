package com.socialworld.app.room;

import com.socialworld.app.intention.Intention;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** A themed social environment: background art plus predefined avatar slots. */
@Entity
@Table(name = "rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomTheme theme;

    /** The mood this room is recommended for. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Intention intention;

    @Column(name = "background_image_url", nullable = false, length = 500)
    private String backgroundImageUrl;

    @Column(name = "max_users", nullable = false)
    private int maxUsers;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
