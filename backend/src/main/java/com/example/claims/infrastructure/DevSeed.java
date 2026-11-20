package com.example.claims.infrastructure;

import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimHistory;
import com.example.claims.domain.ClaimInformationRequest;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.domain.ClaimType;
import com.example.claims.domain.Role;
import com.example.claims.domain.User;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Configuration
@Profile("dev")
public class DevSeed {
    public static final UUID CLAIMANT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    public static final UUID OFFICER_ONE_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    public static final UUID OFFICER_TWO_ID = UUID.fromString("20000000-0000-4000-8000-000000000002");
    public static final UUID MANAGER_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");

    @Bean
    ApplicationRunner seedDemoData(UserRepository users, ClaimRepository claims,
                                   InformationRequestRepository requests, ClaimHistoryRepository history) {
        return args -> {
            if (users.count() > 0) return;
            Instant now = Instant.now();
            User claimant = new User(CLAIMANT_ID, "Maya Tan", "maya.tan@example.test", Role.CLAIMANT, now);
            User officerOne = new User(OFFICER_ONE_ID, "Rafiq Lim", "rafiq.lim@example.test", Role.OFFICER, now);
            User officerTwo = new User(OFFICER_TWO_ID, "Anika Rao", "anika.rao@example.test", Role.OFFICER, now);
            User manager = new User(MANAGER_ID, "Darren Koh", "darren.koh@example.test", Role.MANAGER, now);
            users.save(claimant);
            users.save(officerOne);
            users.save(officerTwo);
            users.save(manager);

            Claim submitted = create(claimant, "CLM-2026-0001", ClaimType.MOTOR,
                    "A delivery van reversed into my parked vehicle at the market.", "Sengkang, Singapore",
                    "SGX 4821K", "1840.00", now.minusSeconds(7200));
            claims.save(submitted);
            addHistory(history, submitted, null, ClaimStatus.SUBMITTED, "SUBMITTED",
                    "Claim submitted", claimant, submitted.getSubmittedAt());

            Claim review = create(claimant, "CLM-2026-0002", ClaimType.PROPERTY,
                    "Water entered the kitchen after a pipe joint failed.", "Petaling Jaya, Malaysia",
                    null, "3260.50", now.minusSeconds(172800));
            review.assign(officerOne, now.minusSeconds(170000));
            claims.save(review);
            addHistory(history, review, null, ClaimStatus.SUBMITTED, "SUBMITTED",
                    "Claim submitted", claimant, review.getSubmittedAt());
            addHistory(history, review, ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, "ASSIGNED",
                    "Assigned to " + officerOne.getName(), officerOne, now.minusSeconds(170000));

            Claim needsInfo = create(claimant, "CLM-2026-0003", ClaimType.MOTOR,
                    "A motorbike struck the rear bumper at a junction.", "Tampines, Singapore",
                    "SLA 9032T", "2100.00", now.minusSeconds(345600));
            needsInfo.assign(officerTwo, now.minusSeconds(340000));
            needsInfo.requireInformation(now.minusSeconds(330000));
            claims.save(needsInfo);
            ClaimInformationRequest infoRequest = requests.save(new ClaimInformationRequest(
                    UUID.fromString("40000000-0000-4000-8000-000000000001"), needsInfo,
                    "Please provide a copy of the police report or incident reference.",
                    officerTwo, now.minusSeconds(330000)));
            addHistory(history, needsInfo, null, ClaimStatus.SUBMITTED, "SUBMITTED",
                    "Claim submitted", claimant, needsInfo.getSubmittedAt());
            addHistory(history, needsInfo, ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, "ASSIGNED",
                    "Assigned to " + officerTwo.getName(), officerTwo, now.minusSeconds(340000));
            addHistory(history, needsInfo, ClaimStatus.UNDER_REVIEW, ClaimStatus.INFORMATION_REQUIRED,
                    "INFORMATION_REQUESTED", infoRequest.getMessage(), officerTwo, now.minusSeconds(330000));

            Claim approved = create(claimant, "CLM-2026-0004", ClaimType.PROPERTY,
                    "A storm damaged two roof tiles and caused a ceiling leak.", "Kallang, Singapore",
                    null, "4860.00", now.minusSeconds(604800));
            approved.assign(officerOne, now.minusSeconds(600000));
            approved.decide(true, new BigDecimal("3525.75"), "Repair estimate reviewed and approved.",
                    now.minusSeconds(500000));
            claims.save(approved);
            addHistory(history, approved, null, ClaimStatus.SUBMITTED, "SUBMITTED",
                    "Claim submitted", claimant, approved.getSubmittedAt());
            addHistory(history, approved, ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, "ASSIGNED",
                    "Assigned to " + officerOne.getName(), officerOne, now.minusSeconds(600000));
            addHistory(history, approved, ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED, "APPROVED",
                    approved.getDecisionReason(), officerOne, now.minusSeconds(500000));

            Claim rejected = create(claimant, "CLM-2026-0005", ClaimType.MOTOR,
                    "A side mirror was found cracked after parking.", "Jurong East, Singapore",
                    "SNE 3168M", "680.00", now.minusSeconds(864000));
            rejected.assign(officerTwo, now.minusSeconds(820000));
            rejected.decide(false, null, "The damage was present before the reported incident date.",
                    now.minusSeconds(800000));
            claims.save(rejected);
            addHistory(history, rejected, null, ClaimStatus.SUBMITTED, "SUBMITTED",
                    "Claim submitted", claimant, rejected.getSubmittedAt());
            addHistory(history, rejected, ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, "ASSIGNED",
                    "Assigned to " + officerTwo.getName(), officerTwo, now.minusSeconds(820000));
            addHistory(history, rejected, ClaimStatus.UNDER_REVIEW, ClaimStatus.REJECTED, "REJECTED",
                    rejected.getDecisionReason(), officerTwo, now.minusSeconds(800000));

            Claim settled = create(claimant, "CLM-2026-0006", ClaimType.PROPERTY,
                    "A kitchen appliance leak damaged the cabinet base.", "Woodlands, Singapore",
                    null, "1250.00", now.minusSeconds(1209600));
            settled.assign(officerOne, now.minusSeconds(1200000));
            settled.decide(true, new BigDecimal("1125.00"), "Assessment confirmed the repair scope.",
                    now.minusSeconds(1100000));
            settled.settle(new BigDecimal("1125.00"), now.minusSeconds(1000000));
            claims.save(settled);
            addHistory(history, settled, null, ClaimStatus.SUBMITTED, "SUBMITTED",
                    "Claim submitted", claimant, settled.getSubmittedAt());
            addHistory(history, settled, ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, "ASSIGNED",
                    "Assigned to " + officerOne.getName(), officerOne, now.minusSeconds(1200000));
            addHistory(history, settled, ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED, "APPROVED",
                    settled.getDecisionReason(), officerOne, now.minusSeconds(1100000));
            addHistory(history, settled, ClaimStatus.APPROVED, ClaimStatus.SETTLED, "SETTLED",
                    "Settled 1125.00", officerOne, now.minusSeconds(1000000));
        };
    }

    private Claim create(User claimant, String number, ClaimType type, String description,
                         String location, String registration, String estimated, Instant submittedAt) {
        return new Claim(UUID.randomUUID(), number, claimant, type,
                LocalDate.now().minusDays(3), description, new BigDecimal(estimated), location, registration, submittedAt);
    }

    private void addHistory(ClaimHistoryRepository history, Claim claim, ClaimStatus previous,
                            ClaimStatus next, String action, String remarks, User actor, Instant at) {
        history.save(new ClaimHistory(UUID.randomUUID(), claim, previous, next, action, remarks, actor, at));
    }
}
