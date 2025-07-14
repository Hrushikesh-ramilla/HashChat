package com.hashchat.messenger.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    DEAD
}
