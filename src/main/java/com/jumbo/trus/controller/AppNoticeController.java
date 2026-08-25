package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.appnotice.CurrentAppNoticeDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.appnotice.AppNoticeService;
import com.jumbo.trus.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app-notice")
@RequiredArgsConstructor
public class AppNoticeController {

    private final AppNoticeService appNoticeService;
    private final AuthService authService;

    @RoleRequired("READER")
    @GetMapping("/current")
    public CurrentAppNoticeDTO getCurrent(
            @RequestHeader(name = "x-app-version", required = false) String appVersion
    ) {
        UserEntity user = authService.getCurrentUserEntity();
        return appNoticeService.getCurrent(user, appVersion);
    }

    @RoleRequired("READER")
    @PostMapping("/{noticeId}/shown")
    public void markShown(@PathVariable Long noticeId) {
        appNoticeService.markShown(noticeId, authService.getCurrentUserEntity());
    }
}
