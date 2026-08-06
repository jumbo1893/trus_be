package com.jumbo.trus.entity.outbox;

public enum OutboxEventStatus {

    NEW,
    PROCESSING,
    DONE,
    RETRY,
    FAILED
}