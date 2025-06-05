package com.hashchat.messenger.crypto.dto;

import java.time.LocalDateTime;

public record UserDeviceResponse(
        Long id,
        String deviceId,
        String deviceName,
        boolean active,
        boolean current,
        LocalDateTime lastSeen,
        LocalDateTime createdAt
) {
}