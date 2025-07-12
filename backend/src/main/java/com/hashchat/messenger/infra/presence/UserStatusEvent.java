package com.hashchat.messenger.infra.presence;

public record UserStatusEvent(
        String username,
        String status,
        long timestamp
) {
}
