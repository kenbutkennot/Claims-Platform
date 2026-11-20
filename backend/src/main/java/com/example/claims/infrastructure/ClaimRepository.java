package com.example.claims.infrastructure;

import com.example.claims.domain.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findAllByOrderBySubmittedAtDesc();
    List<Claim> findAllByClaimantIdOrderBySubmittedAtDesc(UUID claimantId);
}
