package com.hashchat.messenger.message.dto;

import java.util.Map;

public record ReactionEvent(
        String type,
        Long messageId,
        Long chatId,
        Long actorUserId,
        String actorDeviceId,
        String emoji,
        boolean active,
        Map<String, Long> reactions,
        long timestamp
) {
}
