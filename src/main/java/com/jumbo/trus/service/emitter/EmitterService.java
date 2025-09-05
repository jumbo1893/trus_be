package com.jumbo.trus.service.emitter;

import com.jumbo.trus.repository.emitter.EmitterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class EmitterService {

    private static long EVENTS_TIMOUT = 60000;
    private final EmitterRepository repository;

    public EmitterService(
                          EmitterRepository repository) {
        this.repository = repository;
    }

    public SseEmitter createEmitter(String memberId) {
        SseEmitter emitter = new SseEmitter(EVENTS_TIMOUT);
        emitter.onCompletion(() -> repository.remove(memberId));
        emitter.onTimeout(() -> repository.remove(memberId));
        emitter.onError(e -> {
            log.error("Create SseEmitter exception", e);
            repository.remove(memberId);
        });
        repository.addOrReplaceEmitter(memberId, emitter);
        return emitter;
    }

}
