package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.notification.push.DeviceTokenDTO;
import com.jumbo.trus.service.notification.push.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/push")
public class PushController {

    private final PushService pushService;

    @RoleRequired("READER")
    @PostMapping("/token/add")
    public DeviceTokenDTO addToken(@RequestBody DeviceTokenDTO deviceTokenDTO) {
        return pushService.addNewToken(deviceTokenDTO);
    }
}
