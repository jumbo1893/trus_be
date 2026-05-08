package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.dto.notification.push.DeviceTokenDTO;
import com.jumbo.trus.dto.notification.push.EnabledPushNotificationDTO;
import com.jumbo.trus.service.notification.push.PushService;
import com.jumbo.trus.service.notification.settings.EnabledPushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/push")
public class PushController {

    private final PushService pushService;
    private final EnabledPushNotificationService enabledPushService;

    @RoleRequired("READER")
    @PostMapping("/token/add")
    public DeviceTokenDTO addToken(@RequestBody DeviceTokenDTO deviceTokenDTO) {
        return pushService.addNewToken(deviceTokenDTO);
    }

    @RoleRequired("READER")
    @GetMapping("/enabled/get-all")
    public List<EnabledPushNotificationDTO> getAllByUser() {
        return enabledPushService.getAllByUser();
    }

    @RoleRequired("READER")
    @PutMapping("enabled/{notificationId}")
    public EnabledPushNotificationDTO editNotificationPermit(@PathVariable Long notificationId, @RequestBody EnabledPushNotificationDTO enabledPushNotificationDTO) throws NotFoundException {
        return enabledPushService.editNotificationPermit(notificationId, enabledPushNotificationDTO);
    }

    @RoleRequired("READER")
    @PutMapping("enabled/set")
    public StringAndString editNotificationsPermit(@RequestBody List<EnabledPushNotificationDTO> enabledPushNotificationList) throws NotFoundException {
        return enabledPushService.editNotificationsPermit(enabledPushNotificationList);
    }

    @PostMapping("push/init")
    public void editNotificationsPermit() {
        pushService.initAllTokenUsers();
    }

    @RoleRequired("READER")
    @PostMapping("/token/test")
    public void sendTestPush(@RequestBody DeviceTokenDTO deviceTokenDTO) throws Exception {
        pushService.sendTestPushToCurrentDevice(deviceTokenDTO);
    }

}
