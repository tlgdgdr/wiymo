package com.socialworld.app.gift;

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

@Entity
@Table(name = "gifts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gift {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "icon_url", nullable = false, length = 500)
    private String iconUrl;

    /** Reserved for animated gifts later. */
    @Column(name = "animation_url", length = 500)
    private String animationUrl;

    @Column(name = "coin_price", nullable = false)
    private int coinPrice;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GiftCategory category;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
