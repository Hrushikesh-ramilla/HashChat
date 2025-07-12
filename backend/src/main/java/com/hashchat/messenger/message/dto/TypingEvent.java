package com.hashchat.messenger.message.dto;

public record TypingEvent(
        String username,
        boolean typing
) {
}
