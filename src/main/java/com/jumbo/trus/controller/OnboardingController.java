package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.service.auth.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService service;
    public record ProgressRequest(OnboardingService.Step completedStep) {}

    // Explicit multi-segment route cannot collide with legacy PUT /user/{userId}.
    @PostMapping("/start")
    @RoleRequired("VIEWER")
    public OnboardingService.State start() { return service.advance(null); }

    @GetMapping
    @RoleRequired("READER")
    public OnboardingService.State state() { return service.state(); }

    @PutMapping
    @RoleRequired("VIEWER")
    public OnboardingService.State progress(@RequestBody ProgressRequest request) {
        return service.advance(request.completedStep());
    }

    @GetMapping("/profiles")
    @RoleRequired("READER")
    public OnboardingService.Profiles profiles() { return service.profiles(); }

    @PostMapping("/profile")
    @RoleRequired("VIEWER")
    public PlayerDTO pair(@RequestBody OnboardingService.PairRequest request) { return service.pair(request); }
}
