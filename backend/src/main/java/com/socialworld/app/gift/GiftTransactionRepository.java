package com.socialworld.app.gift;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GiftTransactionRepository extends JpaRepository<GiftTransaction, UUID> {
}
