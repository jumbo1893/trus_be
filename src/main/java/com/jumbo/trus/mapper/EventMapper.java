package com.jumbo.trus.mapper;

import com.jumbo.trus.dto.EventDTO;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@AllArgsConstructor
public class EventMapper {

    public SseEmitter.SseEventBuilder toSseEventBuilder(EventDTO event) {
        return SseEmitter.event()
                .id(RandomStringUtils.randomAlphanumeric(12))
                .name(event.getType())
                .data(event.getBody());
    }

}
