package com.jumbo.trus.controller;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.AppOpenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping("/app-open")
@RequiredArgsConstructor
public class AppOpenController {

    private final AppOpenService appOpenService;

    public record AppOpenRequest(@NotNull UUID eventId, String clientDeviceId,
                                 @NotNull String openType, Instant clientOpenedAt) {}

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@Valid @RequestBody AppOpenRequest request, HttpServletRequest httpRequest) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserEntity user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        appOpenService.record(request.eventId(), user.getId(), request.clientDeviceId(),
                request.openType(), request.clientOpenedAt(), httpRequest);
    }
}
