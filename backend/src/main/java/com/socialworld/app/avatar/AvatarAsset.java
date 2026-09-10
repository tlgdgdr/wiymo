package com.socialworld.app.avatar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One selectable avatar layer (a transparent PNG). Only the asset key is
 * stored on user avatars; pricing/premium flags exist so paid cosmetics can
 * be introduced later without schema changes.
 */
@Entity
@Table(name = "avatar_assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarAsset {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvatarCategory category;

    /** Stable key referenced by user avatars, e.g. "body_01". */
    @Column(name = "asset_key", nullable = false, unique = true, length = 50)
    private String assetKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean premium = false;

    @Column(name = "coin_price", nullable = false)
    @Builder.Default
    private int coinPrice = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
