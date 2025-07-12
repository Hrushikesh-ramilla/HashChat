package com.hashchat.messenger.crypto.dto;

import java.util.List;

public record PreKeyBundleResponse(
        String username,
        List<DeviceBundleDto> devices
) {
}
