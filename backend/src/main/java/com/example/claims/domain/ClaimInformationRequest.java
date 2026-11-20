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
@Table(name = "claim_information_request", indexes = @Index(name = "idx_info_claim", columnList = "claim_id"))
public class ClaimInformationRequest {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false, foreignKey = @ForeignKey(name = "fk_info_claim"))
    private Claim claim;

    @Column(nullable = false, length = 1200)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InformationRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false, foreignKey = @ForeignKey(name = "fk_info_requester"))
    private User requestedBy;

    @Column(nullable = false)
    private Instant requestedAt;

    @Column(length = 2000)
    private String response;

    private Instant respondedAt;

    protected ClaimInformationRequest() {}

    public ClaimInformationRequest(UUID id, Claim claim, String message, User requestedBy, Instant requestedAt) {
        this.id = id;
        this.claim = claim;
        this.message = message;
        this.status = InformationRequestStatus.OPEN;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
    }

    public void respond(String response, Instant now) {
        if (status != InformationRequestStatus.OPEN) throw new IllegalStateException("Request is not open");
        this.response = response;
        this.respondedAt = now;
        this.status = InformationRequestStatus.RESPONDED;
    }

    public UUID getId() { return id; }
    public Claim getClaim() { return claim; }
    public String getMessage() { return message; }
    public InformationRequestStatus getStatus() { return status; }
    public User getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public String getResponse() { return response; }
    public Instant getRespondedAt() { return respondedAt; }
}
