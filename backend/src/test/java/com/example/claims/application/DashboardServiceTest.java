package com.example.claims.application;

import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.domain.ClaimType;
import com.example.claims.domain.Role;
import com.example.claims.domain.User;
import com.example.claims.infrastructure.ClaimRepository;
import com.example.claims.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {
    @Test
    void exposureExcludesRejectedAndSettledClaims() {
        UUID managerId = UUID.randomUUID();
        User manager = new User(managerId, "Darren Koh", "darren@example.test", Role.MANAGER, Instant.now());
        User claimant = new User(UUID.randomUUID(), "Maya Tan", "maya@example.test", Role.CLAIMANT, Instant.now());
        User officer = new User(UUID.randomUUID(), "Rafiq Lim", "rafiq@example.test", Role.OFFICER, Instant.now());
        Claim submitted = claim(claimant, "100.25");
        Claim approved = claim(claimant, "200.50");
        approved.assign(officer, Instant.now());
        approved.decide(true, new BigDecimal("150.40"), "Approved", Instant.now());
        Claim rejected = claim(claimant, "300.00");
        rejected.assign(officer, Instant.now());
        rejected.decide(false, null, "Rejected", Instant.now());
        Claim settled = claim(claimant, "400.00");
        settled.assign(officer, Instant.now());
        settled.decide(true, new BigDecimal("350.00"), "Approved", Instant.now());
        settled.settle(new BigDecimal("350.00"), Instant.now());

        ClaimRepository claims = mock(ClaimRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(users.findById(any())).thenReturn(Optional.of(manager));
        when(claims.findAll()).thenReturn(List.of(submitted, approved, rejected, settled));

        var summary = new DashboardService(claims, users, new DemoActorService(users)).summary(managerId);
        assertThat(summary.estimatedOutstandingExposure()).isEqualByComparingTo("300.75");
        assertThat(summary.approvedUnsettledAmount()).isEqualByComparingTo("150.40");
        assertThat(summary.approvedUnsettled()).isEqualTo(1);
    }

    private Claim claim(User claimant, String amount) {
        return new Claim(UUID.randomUUID(), "CLM-" + UUID.randomUUID().toString().substring(0, 8),
                claimant, ClaimType.PROPERTY, LocalDate.now(), "Test", new BigDecimal(amount),
                null, null, Instant.now());
    }
}
