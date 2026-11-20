package com.example.claims.infrastructure;

import com.example.claims.domain.ClaimInformationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InformationRequestRepository extends JpaRepository<ClaimInformationRequest, UUID> {
    List<ClaimInformationRequest> findAllByClaimIdOrderByRequestedAtDesc(UUID claimId);
    Optional<ClaimInformationRequest> findByIdAndClaimId(UUID id, UUID claimId);
}
