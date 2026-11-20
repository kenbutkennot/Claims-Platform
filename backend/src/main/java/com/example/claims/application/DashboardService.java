package com.example.claims.application;

import com.example.claims.application.ClaimDtos.DashboardSummary;
import com.example.claims.application.ClaimDtos.OfficerWorkload;
import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.domain.Role;
import com.example.claims.domain.User;
import com.example.claims.infrastructure.ClaimRepository;
import com.example.claims.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {
    private final ClaimRepository claims;
    private final UserRepository users;
    private final DemoActorService actors;

    public DashboardService(ClaimRepository claims, UserRepository users, DemoActorService actors) {
        this.claims = claims;
        this.users = users;
        this.actors = actors;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(UUID actorId) {
        actors.requireStaff(actors.requireUser(actorId));
        List<Claim> all = claims.findAll();
        BigDecimal estimatedExposure = all.stream()
                .filter(claim -> claim.getStatus() != ClaimStatus.REJECTED && claim.getStatus() != ClaimStatus.SETTLED)
                .map(Claim::getEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal approvedUnsettled = all.stream()
                .filter(claim -> claim.getStatus() == ClaimStatus.APPROVED)
                .map(Claim::getApprovedAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardSummary(all.size(),
                count(all, ClaimStatus.SUBMITTED), count(all, ClaimStatus.UNDER_REVIEW),
                count(all, ClaimStatus.INFORMATION_REQUIRED), count(all, ClaimStatus.APPROVED),
                count(all, ClaimStatus.REJECTED), estimatedExposure, approvedUnsettled);
    }

    @Transactional(readOnly = true)
    public List<OfficerWorkload> workload(UUID actorId) {
        actors.requireStaff(actors.requireUser(actorId));
        List<Claim> all = claims.findAll();
        return users.findByRoleOrderByName(Role.OFFICER).stream()
                .map(officer -> {
                    List<Claim> assigned = all.stream()
                            .filter(claim -> claim.getAssignedOfficer() != null
                                    && claim.getAssignedOfficer().getId().equals(officer.getId()))
                            .toList();
                    long active = assigned.stream().filter(claim -> claim.getStatus() != ClaimStatus.REJECTED
                            && claim.getStatus() != ClaimStatus.SETTLED).count();
                    return new OfficerWorkload(officer.getId(), officer.getName(), assigned.size(), active);
                })
                .toList();
    }

    private long count(List<Claim> all, ClaimStatus status) {
        return all.stream().filter(claim -> claim.getStatus() == status).count();
    }
}
