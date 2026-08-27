package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.membership.MembershipDTO;
import com.jumbo.trus.dto.membership.MembershipGrantRequest;
import jakarta.validation.Valid;
import com.jumbo.trus.service.ai.AiQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final AiQuestionService questionService;

    @RoleRequired("READER")
    @GetMapping
    public MembershipDTO getMembership() {
        return questionService.getMembership();
    }

    @RoleRequired("NONE")
    @PutMapping("/users/{userId}/grant")
    public MembershipDTO grantMembership(
            @PathVariable Long userId,
            @Valid @RequestBody MembershipGrantRequest request
    ) {
        return questionService.grantMembership(userId, request);
    }

    @RoleRequired("NONE")
    @DeleteMapping("/users/{userId}/grant")
    public MembershipDTO clearMembershipGrant(@PathVariable Long userId) {
        return questionService.clearMembershipGrant(userId);
    }
}
