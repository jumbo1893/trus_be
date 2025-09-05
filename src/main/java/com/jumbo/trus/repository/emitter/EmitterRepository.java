package com.jumbo.trus.repository.emitter;


import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

public interface EmitterRepository {

    void addOrReplaceEmitter(String memberId, SseEmitter emitter);

    void remove(String memberId);

    Optional<SseEmitter> get(String memberId);

}

