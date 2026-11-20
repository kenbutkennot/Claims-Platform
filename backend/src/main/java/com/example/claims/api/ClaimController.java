package com.example.claims.api;

import com.example.claims.application.ClaimDtos.AssignRequest;
import com.example.claims.application.ClaimDtos.ClaimDto;
import com.example.claims.application.ClaimDtos.CreateClaimRequest;
import com.example.claims.application.ClaimDtos.DecisionRequest;
import com.example.claims.application.ClaimDtos.HistoryDto;
import com.example.claims.application.ClaimDtos.InformationRequestBody;
import com.example.claims.application.ClaimDtos.InformationResponseBody;
import com.example.claims.application.ClaimDtos.SettlementRequest;
import com.example.claims.application.ClaimService;
import com.example.claims.application.ClaimDtos.UserDto;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.domain.ClaimType;
import com.example.claims.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ClaimController {
    private final ClaimService claims;

    public ClaimController(ClaimService claims) {
        this.claims = claims;
    }

    @PostMapping("/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimDto submit(@RequestHeader("X-Demo-User") UUID actorId,
                           @Valid @RequestBody CreateClaimRequest request) {
        return claims.submit(actorId, request);
    }

    @GetMapping("/claims")
    public List<ClaimDto> list(@RequestHeader("X-Demo-User") UUID actorId,
                               @RequestParam(required = false) ClaimStatus status,
                               @RequestParam(required = false) ClaimType type,
                               @RequestParam(required = false) UUID assignedOfficerId,
                               @RequestParam(required = false) UUID claimantId,
                               @RequestParam(required = false) String search) {
        return claims.list(actorId, status, type, assignedOfficerId, claimantId, search);
    }

    @GetMapping("/claims/{id}")
    public ClaimDto get(@RequestHeader("X-Demo-User") UUID actorId, @PathVariable UUID id) {
        return claims.get(actorId, id);
    }

    @PatchMapping("/claims/{id}/assign")
    public ClaimDto assign(@RequestHeader("X-Demo-User") UUID actorId, @PathVariable UUID id,
                           @Valid @RequestBody AssignRequest request) {
        return claims.assign(actorId, id, request);
    }

    @PostMapping("/claims/{id}/information-requests")
    public ClaimDto requestInformation(@RequestHeader("X-Demo-User") UUID actorId, @PathVariable UUID id,
                                       @Valid @RequestBody InformationRequestBody request) {
        return claims.requestInformation(actorId, id, request);
    }

    @PostMapping("/claims/{id}/information-requests/{requestId}/response")
    public ClaimDto respond(@RequestHeader("X-Demo-User") UUID actorId, @PathVariable UUID id,
                            @PathVariable UUID requestId, @Valid @RequestBody InformationResponseBody request) {
        return claims.respond(actorId, id, requestId, request);
    }

    @PostMapping("/claims/{id}/decision")
    public ClaimDto decide(@RequestHeader("X-Demo-User") UUID actorId, @PathVariable UUID id,
                           @RequestBody DecisionRequest request) {
        return claims.decide(actorId, id, request);
    }

    @PostMapping("/claims/{id}/settlement")
    public ClaimDto settle(@RequestHeader("X-Demo-User") UUID actorId, @PathVariable UUID id,
                           @Valid @RequestBody SettlementRequest request) {
        return claims.settle(actorId, id, request);
    }

    @GetMapping("/claims/{id}/history")
    public List<HistoryDto> history(@RequestHeader("X-Demo-User") UUID actorId, @PathVariable UUID id) {
        return claims.history(actorId, id);
    }

    @GetMapping("/demo/officers")
    public List<UserDto> officers(@RequestHeader("X-Demo-User") UUID actorId) {
        return claims.officers(actorId).stream().map(ClaimController::toUserDto).toList();
    }

    @GetMapping("/demo/assignees")
    public List<UserDto> assignees(@RequestHeader("X-Demo-User") UUID actorId) {
        return claims.assignees(actorId).stream().map(ClaimController::toUserDto).toList();
    }

    static UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
