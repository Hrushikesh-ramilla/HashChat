package com.hashchat.messenger.crypto.dto;

public record DeviceRegistrationResponse(
        String deviceId,
        Long serverDeviceInternalId
) {
}
