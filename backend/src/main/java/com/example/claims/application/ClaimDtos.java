package com.example.claims.application;

import com.example.claims.domain.ClaimStatus;
import com.example.claims.domain.ClaimType;
import com.example.claims.domain.InformationRequestStatus;
import com.example.claims.domain.Role;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ClaimDtos {
    private ClaimDtos() {}

    public record CreateClaimRequest(
            @NotNull ClaimType claimType,
            @NotNull @PastOrPresent LocalDate incidentDate,
            @NotBlank @Size(max = 2000) String incidentDescription,
            @NotNull @DecimalMin("0.00") BigDecimal estimatedAmount,
            @Size(max = 240) String incidentLocation,
            @Size(max = 40) String vehicleRegistration) {}

    public record AssignRequest(@NotNull UUID officerId) {}
    public record InformationRequestBody(@NotBlank @Size(max = 1200) String message) {}
    public record InformationResponseBody(@NotNull @Size(max = 2000) String response) {}
    public record DecisionRequest(Boolean approved, BigDecimal approvedAmount, String reason) {}
    public record SettlementRequest(@NotNull @DecimalMin("0.00") BigDecimal amount) {}

    public record UserDto(UUID id, String name, String email, Role role) {}
    public record InformationRequestDto(UUID id, String message, InformationRequestStatus status,
                                        UUID requestedBy, String requestedByName, Instant requestedAt,
                                        String response, Instant respondedAt) {}
    public record HistoryDto(UUID id, ClaimStatus previousStatus, ClaimStatus newStatus,
                             String action, String remarks, UUID changedBy,
                             String changedByName, Instant createdAt) {}
    public record ClaimDto(UUID id, String claimNumber, UUID claimantId, String claimantName,
                           UUID assignedOfficerId, String assignedOfficerName, ClaimType claimType,
                           ClaimStatus status, LocalDate incidentDate, String incidentDescription,
                           BigDecimal estimatedAmount, BigDecimal approvedAmount, BigDecimal settledAmount,
                           String decisionReason, String incidentLocation, String vehicleRegistration,
                           Instant submittedAt, Instant updatedAt, Instant settledAt,
                           List<InformationRequestDto> informationRequests, List<HistoryDto> history) {}

    public record DashboardSummary(long totalClaims, long awaitingReview, long underReview,
                                   long informationRequired, long approvedUnsettled, long rejected,
                                   BigDecimal estimatedOutstandingExposure,
                                   BigDecimal approvedUnsettledAmount) {}
    public record OfficerWorkload(UUID officerId, String officerName, long assignedClaims,
                                  long activeClaims) {}
}
