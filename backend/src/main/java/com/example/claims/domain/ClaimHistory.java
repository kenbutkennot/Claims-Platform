package com.example.claims.domain;

import jakarta.persistence.Column;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_history", indexes = @Index(name = "idx_history_claim", columnList = "claim_id"))
public class ClaimHistory {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false, foreignKey = @ForeignKey(name = "fk_history_claim"))
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private ClaimStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private ClaimStatus newStatus;

    @Column(nullable = false, length = 48)
    private String action;

    @Column(length = 1200)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false, foreignKey = @ForeignKey(name = "fk_history_actor"))
    private User changedBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected ClaimHistory() {}

    public ClaimHistory(UUID id, Claim claim, ClaimStatus previousStatus, ClaimStatus newStatus,
                        String action, String remarks, User changedBy, Instant createdAt) {
        this.id = id;
        this.claim = claim;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.action = action;
        this.remarks = remarks;
        this.changedBy = changedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Claim getClaim() { return claim; }
    public ClaimStatus getPreviousStatus() { return previousStatus; }
    public ClaimStatus getNewStatus() { return newStatus; }
    public String getAction() { return action; }
    public String getRemarks() { return remarks; }
    public User getChangedBy() { return changedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
