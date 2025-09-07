package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.log.LogDTO;
import com.jumbo.trus.service.notification.push.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/log")
public class LogController {

    private final LogService logService;

    @RoleRequired("READER")
    @PostMapping("/add")
    public LogDTO addLog(@RequestBody LogDTO logDTO) {
        return logService.addLog(logDTO);
    }
}
