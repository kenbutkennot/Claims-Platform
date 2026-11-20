package com.example.claims.application;

import com.example.claims.application.ClaimDtos.AssignRequest;
import com.example.claims.application.ClaimDtos.ClaimDto;
import com.example.claims.application.ClaimDtos.CreateClaimRequest;
import com.example.claims.application.ClaimDtos.DecisionRequest;
import com.example.claims.application.ClaimDtos.HistoryDto;
import com.example.claims.application.ClaimDtos.InformationRequestBody;
import com.example.claims.application.ClaimDtos.InformationRequestDto;
import com.example.claims.application.ClaimDtos.InformationResponseBody;
import com.example.claims.application.ClaimDtos.SettlementRequest;
import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimHistory;
import com.example.claims.domain.ClaimInformationRequest;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.domain.ClaimType;
import com.example.claims.domain.InformationRequestStatus;
import com.example.claims.domain.Role;
import com.example.claims.domain.User;
import com.example.claims.infrastructure.ClaimHistoryRepository;
import com.example.claims.infrastructure.ClaimRepository;
import com.example.claims.infrastructure.InformationRequestRepository;
import com.example.claims.infrastructure.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ClaimService {
    private final ClaimRepository claims;
    private final UserRepository users;
    private final InformationRequestRepository informationRequests;
    private final ClaimHistoryRepository history;
    private final DemoActorService actors;
    private final Clock clock;

    @Autowired
    public ClaimService(ClaimRepository claims, UserRepository users,
                        InformationRequestRepository informationRequests,
                        ClaimHistoryRepository history, DemoActorService actors) {
        this(claims, users, informationRequests, history, actors, Clock.systemUTC());
    }

    ClaimService(ClaimRepository claims, UserRepository users,
                 InformationRequestRepository informationRequests,
                 ClaimHistoryRepository history, DemoActorService actors, Clock clock) {
        this.claims = claims;
        this.users = users;
        this.informationRequests = informationRequests;
        this.history = history;
        this.actors = actors;
        this.clock = clock;
    }

    @Transactional
    public ClaimDto submit(UUID actorId, CreateClaimRequest request) {
        User actor = actors.requireUser(actorId);
        if (actor.getRole() != Role.CLAIMANT) throw forbidden("Only claimants can submit claims");
        if (request.claimType() == ClaimType.PROPERTY && request.vehicleRegistration() != null
                && !request.vehicleRegistration().isBlank()) {
            throw new IllegalArgumentException("Vehicle registration is only valid for motor claims");
        }
        Instant now = clock.instant();
        UUID id = UUID.randomUUID();
        String claimNumber = "CLM-" + now.toString().substring(0, 10).replace("-", "")
                + "-" + id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Claim claim = claims.save(new Claim(id, claimNumber, actor, request.claimType(),
                request.incidentDate(), request.incidentDescription().trim(), request.estimatedAmount(),
                clean(request.incidentLocation()), clean(request.vehicleRegistration()), now));
        record(claim, null, ClaimStatus.SUBMITTED, "SUBMITTED", "Claim submitted", actor, now);
        return toDto(claim);
    }

    @Transactional(readOnly = true)
    public List<ClaimDto> list(UUID actorId, ClaimStatus status, ClaimType type, UUID assignedOfficerId,
                               UUID claimantId, String search) {
        User actor = actors.requireUser(actorId);
        List<Claim> source;
        if (actor.getRole() == Role.CLAIMANT) {
            source = claims.findAllByClaimantIdOrderBySubmittedAtDesc(actor.getId());
        } else {
            actors.requireStaff(actor);
            source = claims.findAllByOrderBySubmittedAtDesc();
        }
        String normalizedSearch = search == null ? null : search.trim().toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(claim -> status == null || claim.getStatus() == status)
                .filter(claim -> type == null || claim.getClaimType() == type)
                .filter(claim -> assignedOfficerId == null || (claim.getAssignedOfficer() != null
                        && assignedOfficerId.equals(claim.getAssignedOfficer().getId())))
                .filter(claim -> claimantId == null || claimantId.equals(claim.getClaimant().getId()))
                .filter(claim -> normalizedSearch == null || normalizedSearch.isBlank()
                        || claim.getClaimNumber().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClaimDto get(UUID actorId, UUID claimId) {
        User actor = actors.requireUser(actorId);
        Claim claim = requireClaim(claimId);
        actors.requireCanRead(actor, claim);
        return toDto(claim);
    }

    @Transactional
    public ClaimDto assign(UUID actorId, UUID claimId, AssignRequest request) {
        User actor = actors.requireUser(actorId);
        actors.requireStaff(actor);
        User officer = users.findById(request.officerId())
                .orElseThrow(() -> new EntityNotFoundException("Officer not found"));
        if (officer.getRole() != Role.OFFICER) throw new IllegalArgumentException("Assignee must be an officer");
        if (actor.getRole() == Role.OFFICER && !actor.getId().equals(officer.getId())) {
            throw forbidden("Officers can only pick up claims for themselves");
        }
        Claim claim = requireClaim(claimId);
        ClaimStatus previous = claim.getStatus();
        Instant now = clock.instant();
        claim.assign(officer, now);
        record(claim, previous, claim.getStatus(), "ASSIGNED", "Assigned to " + officer.getName(), actor, now);
        return toDto(claim);
    }

    @Transactional
    public ClaimDto requestInformation(UUID actorId, UUID claimId, InformationRequestBody request) {
        User actor = actors.requireUser(actorId);
        Claim claim = requireClaim(claimId);
        actors.requireAssignedOfficer(actor, claim);
        ClaimStatus previous = claim.getStatus();
        Instant now = clock.instant();
        claim.requireInformation(now);
        informationRequests.save(new ClaimInformationRequest(UUID.randomUUID(), claim,
                request.message().trim(), actor, now));
        record(claim, previous, claim.getStatus(), "INFORMATION_REQUESTED",
                request.message().trim(), actor, now);
        return toDto(claim);
    }

    @Transactional
    public ClaimDto respond(UUID actorId, UUID claimId, UUID requestId, InformationResponseBody body) {
        User actor = actors.requireUser(actorId);
        if (actor.getRole() != Role.CLAIMANT) throw forbidden("Only the claimant can respond");
        Claim claim = requireClaim(claimId);
        actors.requireCanRead(actor, claim);
        ClaimInformationRequest infoRequest = informationRequests.findByIdAndClaimId(requestId, claimId)
                .orElseThrow(() -> new EntityNotFoundException("Information request not found"));
        if (claim.getStatus() != ClaimStatus.INFORMATION_REQUIRED
                || infoRequest.getStatus() != InformationRequestStatus.OPEN) {
            throw new IllegalStateException("Claim has no open information request");
        }
        ClaimStatus previous = claim.getStatus();
        Instant now = clock.instant();
        infoRequest.respond(body.response().trim(), now);
        boolean hasOpenRequests = informationRequests.findAllByClaimIdOrderByRequestedAtDesc(claimId).stream()
                .anyMatch(item -> item.getStatus() == InformationRequestStatus.OPEN);
        if (!hasOpenRequests) claim.resumeReview(now);
        record(claim, previous, claim.getStatus(), "INFORMATION_RECEIVED",
                body.response().trim(), actor, now);
        return toDto(claim);
    }

    @Transactional
    public ClaimDto decide(UUID actorId, UUID claimId, DecisionRequest request) {
        User actor = actors.requireUser(actorId);
        Claim claim = requireClaim(claimId);
        actors.requireAssignedOfficer(actor, claim);
        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Only claims under review can be decided");
        }
        if (request.approved() == null) throw new IllegalArgumentException("Decision must be approve or reject");
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isBlank()) throw new IllegalArgumentException("A decision reason is required");
        if (request.approved()) {
            if (request.approvedAmount() == null || request.approvedAmount().signum() < 0) {
                throw new IllegalArgumentException("Approval requires a non-negative approved amount");
            }
        } else if (request.approvedAmount() != null) {
            throw new IllegalArgumentException("Rejected claims cannot have an approved amount");
        }
        ClaimStatus previous = claim.getStatus();
        Instant now = clock.instant();
        claim.decide(request.approved(), request.approvedAmount(), reason, now);
        record(claim, previous, claim.getStatus(), request.approved() ? "APPROVED" : "REJECTED",
                reason, actor, now);
        return toDto(claim);
    }

    @Transactional
    public ClaimDto settle(UUID actorId, UUID claimId, SettlementRequest request) {
        User actor = actors.requireUser(actorId);
        Claim claim = requireClaim(claimId);
        actors.requireAssignedOfficer(actor, claim);
        if (claim.getStatus() != ClaimStatus.APPROVED) throw new IllegalStateException("Only approved claims can be settled");
        if (claim.getApprovedAmount() == null || request.amount().compareTo(claim.getApprovedAmount()) > 0) {
            throw new IllegalArgumentException("Settlement amount cannot exceed the approved amount");
        }
        ClaimStatus previous = claim.getStatus();
        Instant now = clock.instant();
        claim.settle(request.amount(), now);
        record(claim, previous, claim.getStatus(), "SETTLED",
                "Settled " + request.amount().toPlainString(), actor, now);
        return toDto(claim);
    }

    @Transactional(readOnly = true)
    public List<HistoryDto> history(UUID actorId, UUID claimId) {
        User actor = actors.requireUser(actorId);
        Claim claim = requireClaim(claimId);
        actors.requireCanRead(actor, claim);
        return history.findAllByClaimIdOrderByCreatedAtAsc(claimId).stream().map(this::toHistoryDto).toList();
    }

    @Transactional(readOnly = true)
    public List<User> officers(UUID actorId) {
        User actor = actors.requireUser(actorId);
        actors.requireStaff(actor);
        return users.findByRoleOrderByName(Role.OFFICER);
    }

    private Claim requireClaim(UUID id) {
        return claims.findById(id).orElseThrow(() -> new EntityNotFoundException("Claim not found"));
    }

    private ClaimDto toDto(Claim claim) {
        List<InformationRequestDto> requests = informationRequests
                .findAllByClaimIdOrderByRequestedAtDesc(claim.getId()).stream()
                .map(item -> new InformationRequestDto(item.getId(), item.getMessage(), item.getStatus(),
                        item.getRequestedBy().getId(), item.getRequestedBy().getName(), item.getRequestedAt(),
                        item.getResponse(), item.getRespondedAt()))
                .toList();
        List<HistoryDto> events = history.findAllByClaimIdOrderByCreatedAtAsc(claim.getId()).stream()
                .map(this::toHistoryDto).toList();
        return new ClaimDto(claim.getId(), claim.getClaimNumber(), claim.getClaimant().getId(),
                claim.getClaimant().getName(),
                claim.getAssignedOfficer() == null ? null : claim.getAssignedOfficer().getId(),
                claim.getAssignedOfficer() == null ? null : claim.getAssignedOfficer().getName(),
                claim.getClaimType(), claim.getStatus(), claim.getIncidentDate(),
                claim.getIncidentDescription(), claim.getEstimatedAmount(), claim.getApprovedAmount(),
                claim.getSettledAmount(), claim.getDecisionReason(), claim.getIncidentLocation(),
                claim.getVehicleRegistration(), claim.getSubmittedAt(), claim.getUpdatedAt(),
                claim.getSettledAt(), requests, events);
    }

    private HistoryDto toHistoryDto(ClaimHistory event) {
        return new HistoryDto(event.getId(), event.getPreviousStatus(), event.getNewStatus(),
                event.getAction(), event.getRemarks(), event.getChangedBy().getId(),
                event.getChangedBy().getName(), event.getCreatedAt());
    }

    private void record(Claim claim, ClaimStatus previous, ClaimStatus next, String action,
                        String remarks, User actor, Instant now) {
        history.save(new ClaimHistory(UUID.randomUUID(), claim, previous, next, action, remarks, actor, now));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private org.springframework.web.server.ResponseStatusException forbidden(String message) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, message);
    }
}
