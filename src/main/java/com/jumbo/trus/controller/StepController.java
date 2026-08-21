package com.jumbo.trus.controller;

import com.jumbo.trus.dto.step.StepDailyDTO;
import com.jumbo.trus.dto.step.StepConsentDTO;
import com.jumbo.trus.dto.step.StepBackgroundSyncRequestDTO;
import com.jumbo.trus.dto.step.StepLeaderboardResponseDTO;
import com.jumbo.trus.dto.step.StepPeriod;
import com.jumbo.trus.dto.step.StepSyncRequestDTO;
import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.service.StepService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/step")
@RequiredArgsConstructor
public class StepController {

    private final StepService stepService;

    @RoleRequired("READER")
    @PutMapping("/sync")
    public List<StepDailyDTO> sync(@RequestBody @Valid StepSyncRequestDTO request) {
        return stepService.sync(request);
    }

    @PostMapping("/background-sync")
    public List<StepDailyDTO> backgroundSync(
            @RequestBody @Valid StepBackgroundSyncRequestDTO request) {
        return stepService.backgroundSync(request);
    }

    @RoleRequired("READER")
    @GetMapping("/consent")
    public StepConsentDTO getConsent() {
        return stepService.getConsent();
    }

    @RoleRequired("READER")
    @PutMapping("/consent")
    public StepConsentDTO setConsent(@RequestBody @Valid StepConsentDTO request) {
        return stepService.setConsent(request);
    }

    @RoleRequired("READER")
    @GetMapping("/leaderboard")
    public StepLeaderboardResponseDTO getLeaderboard(
            @RequestParam(defaultValue = "TODAY") StepPeriod period) {
        return stepService.getLeaderboard(period);
    }

    @GetMapping("/me")
    public List<StepDailyDTO> getMySteps(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return stepService.getMySteps(from, to);
    }
}
