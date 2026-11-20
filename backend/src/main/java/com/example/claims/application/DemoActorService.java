package com.example.claims.application;

import com.example.claims.domain.Claim;
import com.example.claims.domain.Role;
import com.example.claims.domain.User;
import com.example.claims.infrastructure.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class DemoActorService {
    private final UserRepository users;

    public DemoActorService(UserRepository users) {
        this.users = users;
    }

    public User requireUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown demo user"));
    }

    public void requireStaff(User actor) {
        if (actor.getRole() != Role.OFFICER && actor.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Claims staff access is required");
        }
    }

    public void requireAssignedOfficer(User actor, Claim claim) {
        requireStaff(actor);
        if (actor.getRole() == Role.OFFICER
                && (claim.getAssignedOfficer() == null || !claim.getAssignedOfficer().getId().equals(actor.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Claim must be assigned to you");
        }
    }

    public void requireCanRead(User actor, Claim claim) {
        if (actor.getRole() == Role.CLAIMANT && !claim.getClaimant().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Claim does not belong to this claimant");
        }
        requireStaffOrClaimant(actor);
    }

    public void requireStaffOrClaimant(User actor) {
        if (actor.getRole() != Role.CLAIMANT && actor.getRole() != Role.OFFICER
                && actor.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported demo role");
        }
    }
}
