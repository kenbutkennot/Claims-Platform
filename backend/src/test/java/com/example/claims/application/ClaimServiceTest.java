package com.example.claims.application;

import com.example.claims.application.ClaimDtos.CreateClaimRequest;
import com.example.claims.application.ClaimDtos.AssignRequest;
import com.example.claims.application.ClaimDtos.InformationRequestBody;
import com.example.claims.application.ClaimDtos.InformationResponseBody;
import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimInformationRequest;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.domain.ClaimType;
import com.example.claims.domain.Role;
import com.example.claims.domain.User;
import com.example.claims.infrastructure.ClaimHistoryRepository;
import com.example.claims.infrastructure.ClaimRepository;
import com.example.claims.infrastructure.InformationRequestRepository;
import com.example.claims.infrastructure.UserRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClaimServiceTest {
    private static final UUID CLAIMANT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OFFICER_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");

    private UserRepository users;
    private ClaimRepository claims;
    private InformationRequestRepository requests;
    private ClaimHistoryRepository history;
    private ClaimService service;
    private User claimant;
    private User officer;
    private User manager;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        claims = mock(ClaimRepository.class);
        requests = mock(InformationRequestRepository.class);
        history = mock(ClaimHistoryRepository.class);
        claimant = new User(CLAIMANT_ID, "Maya Tan", "maya@example.test", Role.CLAIMANT, Instant.now());
        officer = new User(OFFICER_ID, "Rafiq Lim", "rafiq@example.test", Role.OFFICER, Instant.now());
        manager = new User(MANAGER_ID, "Darren Koh", "darren@example.test", Role.MANAGER, Instant.now());
        when(users.findById(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            if (id.equals(CLAIMANT_ID)) return Optional.of(claimant);
            if (id.equals(OFFICER_ID)) return Optional.of(officer);
            if (id.equals(MANAGER_ID)) return Optional.of(manager);
            return Optional.empty();
        });
        when(claims.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(requests.findAllByClaimIdOrderByRequestedAtDesc(any())).thenReturn(List.of());
        when(history.findAllByClaimIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        service = new ClaimService(claims, users, requests, history, new DemoActorService(users));
    }

    @Test
    void submitsValidClaimAndWritesHistory() {
        var response = service.submit(CLAIMANT_ID, new CreateClaimRequest(ClaimType.MOTOR,
                LocalDate.now().minusDays(1), "Rear bumper damage", new BigDecimal("1234.56"),
                "Singapore", "SXX 1234A"));

        assertThat(response.status()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(response.estimatedAmount()).isEqualByComparingTo("1234.56");
        assertThat(response.claimNumber()).startsWith("CLM-");
        verify(history).save(argThat(event -> event.getAction().equals("SUBMITTED")));
    }

    @Test
    void rejectsNegativeEstimatedAmountThroughBeanValidation() {
        var request = new CreateClaimRequest(ClaimType.PROPERTY, LocalDate.now(),
                "Water damage", new BigDecimal("-0.01"), null, null);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request))
                    .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("estimatedAmount"));
        }
    }

    @Test
    void assigneesIncludeOfficersAndManagersButNotClaimants() {
        List<Role> staffRoles = List.of(Role.OFFICER, Role.MANAGER);
        when(users.findByRoleInOrderByName(staffRoles)).thenReturn(List.of(manager, officer));

        List<User> assignees = service.assignees(MANAGER_ID);

        assertThat(assignees).extracting(User::getRole)
                .containsExactly(Role.MANAGER, Role.OFFICER)
                .doesNotContain(Role.CLAIMANT);
        verify(users).findByRoleInOrderByName(staffRoles);
    }

    @Test
    void managerCanAssignClaimAndClaimantCannotBeAssigned() {
        Claim claim = newClaim(ClaimStatus.SUBMITTED);
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));

        var assigned = service.assign(MANAGER_ID, claim.getId(), new AssignRequest(MANAGER_ID));

        assertThat(assigned.assignedOfficerId()).isEqualTo(MANAGER_ID);
        assertThat(assigned.status()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        verify(history).save(argThat(event -> event.getAction().equals("ASSIGNED")));

        assertThatThrownBy(() -> service.assign(MANAGER_ID, claim.getId(), new AssignRequest(CLAIMANT_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("officer or manager");
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Claim claim = newClaim(ClaimStatus.SUBMITTED);
        assertThatThrownBy(() -> claim.transition(ClaimStatus.APPROVED, Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createsInformationRequestAndRecordsClaimantResponse() {
        Claim claim = newClaim(ClaimStatus.UNDER_REVIEW);
        claim.assign(officer, Instant.now());
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        List<ClaimInformationRequest> storedRequests = new ArrayList<>();
        when(requests.save(any())).thenAnswer(invocation -> {
            ClaimInformationRequest value = invocation.getArgument(0);
            storedRequests.add(value);
            return value;
        });
        when(requests.findAllByClaimIdOrderByRequestedAtDesc(claim.getId())).thenReturn(storedRequests);
        when(requests.findByIdAndClaimId(any(UUID.class), any(UUID.class))).thenAnswer(invocation ->
                storedRequests.stream()
                        .filter(item -> item.getId().equals(invocation.getArgument(0)))
                        .findFirst());

        var requested = service.requestInformation(OFFICER_ID, claim.getId(),
                new InformationRequestBody("Please share the report."));
        ClaimInformationRequest infoRequest = storedRequests.get(0);

        assertThat(requested.status()).isEqualTo(ClaimStatus.INFORMATION_REQUIRED);
        assertThat(infoRequest).isNotNull();
        var responded = service.respond(CLAIMANT_ID, claim.getId(), infoRequest.getId(),
                new InformationResponseBody("Report reference 42."));

        assertThat(responded.status()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(infoRequest.getResponse()).isEqualTo("Report reference 42.");
        verify(history).save(argThat(event -> event.getAction().equals("INFORMATION_REQUESTED")));
        verify(history).save(argThat(event -> event.getAction().equals("INFORMATION_RECEIVED")));
    }

    @Test
    void rejectsRejectionWithoutReason() {
        Claim claim = newClaim(ClaimStatus.UNDER_REVIEW);
        claim.assign(officer, Instant.now());
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.decide(OFFICER_ID, claim.getId(),
                new ClaimDtos.DecisionRequest(false, null, " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void rejectsSettlementUnlessClaimIsApproved() {
        Claim claim = newClaim(ClaimStatus.UNDER_REVIEW);
        claim.assign(officer, Instant.now());
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.settle(OFFICER_ID, claim.getId(),
                new ClaimDtos.SettlementRequest(BigDecimal.ONE)))
                .isInstanceOf(IllegalStateException.class);
    }

    private Claim newClaim(ClaimStatus target) {
        Claim claim = new Claim(UUID.randomUUID(), "CLM-TEST-1", claimant, ClaimType.PROPERTY,
                LocalDate.now(), "Test incident", new BigDecimal("500.00"), null, null, Instant.now());
        if (target != ClaimStatus.SUBMITTED) claim.assign(officer, Instant.now());
        if (target == ClaimStatus.INFORMATION_REQUIRED) claim.requireInformation(Instant.now());
        if (target == ClaimStatus.APPROVED) claim.decide(true, new BigDecimal("400.00"), "Approved", Instant.now());
        if (target == ClaimStatus.REJECTED) claim.decide(false, null, "Rejected", Instant.now());
        if (target == ClaimStatus.SETTLED) {
            claim.decide(true, new BigDecimal("400.00"), "Approved", Instant.now());
            claim.settle(new BigDecimal("400.00"), Instant.now());
        }
        return claim;
    }

}
