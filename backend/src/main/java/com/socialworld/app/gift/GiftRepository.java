package com.socialworld.app.gift;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GiftRepository extends JpaRepository<Gift, UUID> {

    List<Gift> findByEnabledTrueOrderBySortOrder();

    Optional<Gift> findByIdAndEnabledTrue(UUID id);
}
