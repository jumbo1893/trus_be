package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.ai.*;
import com.jumbo.trus.service.ai.AiQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiQuestionService questionService;

    @RoleRequired("READER")
    @PostMapping("/questions")
    public AiQuestionResponse ask(@Valid @RequestBody AiAskRequest request) {
        return questionService.ask(request);
    }

    @RoleRequired("READER")
    @GetMapping("/questions")
    public List<AiQuestionResponse> history(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return questionService.getHistory(limit);
    }

    @RoleRequired("READER")
    @GetMapping("/usage")
    public AiUsageDTO usage() {
        return questionService.getUsage();
    }

    @RoleRequired("NONE")
    @GetMapping("/access")
    public List<AiAccessDTO> allAccess() {
        return questionService.getAllAccess();
    }

    @RoleRequired("NONE")
    @PutMapping("/access/{userId}")
    public AiAccessDTO updateAccess(
            @PathVariable Long userId,
            @Valid @RequestBody AiAccessUpdateRequest request
    ) {
        return questionService.updateAccess(userId, request);
    }
}
