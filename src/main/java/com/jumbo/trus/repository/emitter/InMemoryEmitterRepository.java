package com.jumbo.trus.repository.emitter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class InMemoryEmitterRepository implements EmitterRepository {

    private Map<String, SseEmitter> userEmitterMap = new ConcurrentHashMap<>();

    @Override
    public void addOrReplaceEmitter(String memberId, SseEmitter emitter) {
        userEmitterMap.put(memberId, emitter);
    }

    @Override
    public void remove(String memberId) {
        if (userEmitterMap != null && userEmitterMap.containsKey(memberId)) {
            userEmitterMap.remove(memberId);
        } else {
        }
    }

    @Override
    public Optional<SseEmitter> get(String memberId) {
        return Optional.ofNullable(userEmitterMap.get(memberId));
    }
}
