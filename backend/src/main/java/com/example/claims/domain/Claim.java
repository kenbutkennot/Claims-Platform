package com.example.claims.domain;

import com.example.claims.infrastructure.ExactDecimalConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claim", uniqueConstraints = @UniqueConstraint(columnNames = "claim_number"),
        indexes = {@Index(name = "idx_claim_status", columnList = "status"),
                @Index(name = "idx_claim_claimant", columnList = "claimant_id"),
                @Index(name = "idx_claim_assignee", columnList = "assigned_officer_id")})
public class Claim {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 32)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claimant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_claim_claimant"))
    private User claimant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id", foreignKey = @ForeignKey(name = "fk_claim_officer"))
    private User assignedOfficer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ClaimType claimType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ClaimStatus status;

    @Column(nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false, length = 2000)
    private String incidentDescription;

    @Convert(converter = ExactDecimalConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private BigDecimal estimatedAmount;

    @Convert(converter = ExactDecimalConverter.class)
    @Column(columnDefinition = "TEXT")
    private BigDecimal approvedAmount;

    @Convert(converter = ExactDecimalConverter.class)
    @Column(columnDefinition = "TEXT")
    private BigDecimal settledAmount;

    @Column(length = 1200)
    private String decisionReason;

    @Column(length = 240)
    private String incidentLocation;

    @Column(length = 40)
    private String vehicleRegistration;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant settledAt;

    protected Claim() {}

    public Claim(UUID id, String claimNumber, User claimant, ClaimType claimType, LocalDate incidentDate,
                 String incidentDescription, BigDecimal estimatedAmount, String incidentLocation,
                 String vehicleRegistration, Instant now) {
        this.id = id;
        this.claimNumber = claimNumber;
        this.claimant = claimant;
        this.claimType = claimType;
        this.status = ClaimStatus.SUBMITTED;
        this.incidentDate = incidentDate;
        this.incidentDescription = incidentDescription;
        this.estimatedAmount = estimatedAmount;
        this.incidentLocation = incidentLocation;
        this.vehicleRegistration = vehicleRegistration;
        this.submittedAt = now;
        this.updatedAt = now;
    }

    public void assign(User officer, Instant now) {
        this.assignedOfficer = officer;
        if (status == ClaimStatus.SUBMITTED) status = ClaimStatus.UNDER_REVIEW;
        this.updatedAt = now;
    }

    public void requireInformation(Instant now) {
        transition(ClaimStatus.INFORMATION_REQUIRED, now);
    }

    public void resumeReview(Instant now) {
        transition(ClaimStatus.UNDER_REVIEW, now);
    }

    public void decide(boolean approved, BigDecimal amount, String reason, Instant now) {
        this.approvedAmount = approved ? amount : null;
        this.decisionReason = reason;
        transition(approved ? ClaimStatus.APPROVED : ClaimStatus.REJECTED, now);
    }

    public void settle(BigDecimal amount, Instant now) {
        this.settledAmount = amount;
        this.settledAt = now;
        transition(ClaimStatus.SETTLED, now);
    }

    public void transition(ClaimStatus next, Instant now) {
        boolean allowed = switch (status) {
            case SUBMITTED -> next == ClaimStatus.UNDER_REVIEW;
            case UNDER_REVIEW -> next == ClaimStatus.INFORMATION_REQUIRED
                    || next == ClaimStatus.APPROVED || next == ClaimStatus.REJECTED;
            case INFORMATION_REQUIRED -> next == ClaimStatus.UNDER_REVIEW;
            case APPROVED -> next == ClaimStatus.SETTLED;
            case REJECTED, SETTLED -> false;
        };
        if (!allowed) throw new IllegalStateException("Cannot move claim from " + status + " to " + next);
        status = next;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getClaimNumber() { return claimNumber; }
    public User getClaimant() { return claimant; }
    public User getAssignedOfficer() { return assignedOfficer; }
    public ClaimType getClaimType() { return claimType; }
    public ClaimStatus getStatus() { return status; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public String getIncidentDescription() { return incidentDescription; }
    public BigDecimal getEstimatedAmount() { return estimatedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public String getDecisionReason() { return decisionReason; }
    public String getIncidentLocation() { return incidentLocation; }
    public String getVehicleRegistration() { return vehicleRegistration; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSettledAt() { return settledAt; }
}
