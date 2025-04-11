package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.NotificationDTO;
import com.jumbo.trus.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    NotificationService notificationService;

    @RoleRequired("READER")
    @GetMapping("/get-all")
    public List<NotificationDTO> getNotifications(@RequestParam(required = false, defaultValue = "20") int limit, @RequestParam(required = false, defaultValue = "0") int page) {
        return notificationService.getAll(limit, page);
    }

}
