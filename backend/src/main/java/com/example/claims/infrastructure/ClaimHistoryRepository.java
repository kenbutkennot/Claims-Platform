package com.example.claims.infrastructure;

import com.example.claims.domain.ClaimHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimHistoryRepository extends JpaRepository<ClaimHistory, UUID> {
    List<ClaimHistory> findAllByClaimIdOrderByCreatedAtAsc(UUID claimId);
}
