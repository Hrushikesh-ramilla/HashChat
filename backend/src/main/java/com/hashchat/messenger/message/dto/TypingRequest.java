package com.hashchat.messenger.message.dto;

import jakarta.validation.constraints.NotNull;

public record TypingRequest(
        @NotNull(message = "Chat ID is required")
        Long chatId,
        boolean typing
) {
}
