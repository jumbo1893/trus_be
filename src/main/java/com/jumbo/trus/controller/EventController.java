package com.jumbo.trus.controller;

import com.jumbo.trus.dto.EventDTO;
import com.jumbo.trus.service.emitter.EmitterService;
import com.jumbo.trus.service.emitter.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    public static final String MEMBER_ID_HEADER = "MemberId";

    private final EmitterService emitterService;
    private final NotificationService notificationService;

    @GetMapping
    public SseEmitter subscribeToEvents(@RequestHeader(name = MEMBER_ID_HEADER) String memberId) {
        return emitterService.createEmitter(memberId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void publishEvent(@RequestHeader(name = MEMBER_ID_HEADER) String memberId, @RequestBody EventDTO event) {
        notificationService.sendNotification(memberId, event);
    }
}
