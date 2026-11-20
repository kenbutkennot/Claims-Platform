package com.example.claims.api;

import com.example.claims.application.ClaimDtos.DashboardSummary;
import com.example.claims.application.ClaimDtos.OfficerWorkload;
import com.example.claims.application.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(@RequestHeader("X-Demo-User") UUID actorId) {
        return dashboard.summary(actorId);
    }

    @GetMapping("/workload")
    public List<OfficerWorkload> workload(@RequestHeader("X-Demo-User") UUID actorId) {
        return dashboard.workload(actorId);
    }
}
