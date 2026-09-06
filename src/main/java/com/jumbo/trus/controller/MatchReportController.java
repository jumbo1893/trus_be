package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.ai.MatchReportDTO;
import com.jumbo.trus.dto.ai.MatchReportStateDTO;
import com.jumbo.trus.service.ai.MatchReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/matches/{matchId}/reports")
@RequiredArgsConstructor
public class MatchReportController {
    private final MatchReportService service;

    @RoleRequired("READER")
    @GetMapping
    public MatchReportStateDTO get(@PathVariable Long matchId) {
        return service.getReports(matchId);
    }

    @RoleRequired("READER")
    @PostMapping
    public MatchReportDTO generate(@PathVariable Long matchId) {
        return service.generate(matchId);
    }
}
