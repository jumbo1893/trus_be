package com.jumbo.trus.service.header;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Component
@RequestScope
@RequiredArgsConstructor
public class OperationContext {

    private final HeaderManager headerManager;

    public UUID getOperationId() {
        String uuid = headerManager.getOperationId();
        if (uuid == null || uuid.isEmpty()) {
            return generateUUID();
        }
        return UUID.fromString(uuid);
    }

    private UUID generateUUID () {
        return UUID.randomUUID();
    }

}