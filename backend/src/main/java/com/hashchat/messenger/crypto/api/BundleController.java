package com.hashchat.messenger.crypto.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.hashchat.messenger.crypto.device.CurrentDeviceService;
import com.hashchat.messenger.crypto.dto.PreKeyBundleResponse;
import com.hashchat.messenger.crypto.dto.ResolvedChatDevicesResponse;
import com.hashchat.messenger.crypto.prekey.PreKeyService;

@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class BundleController {

    private final PreKeyService preKeyService;
    private final CurrentDeviceService currentDeviceService;

    @GetMapping("/bundle/{username}")
    public PreKeyBundleResponse getBundle(@PathVariable String username, Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        return preKeyService.getBundleByUsername(username);
    }

    @PostMapping("/resolve-chat-devices/{chatId}")
    public ResolvedChatDevicesResponse resolveChatDevices(@PathVariable Long chatId, Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        return preKeyService.resolveChatDevices(authentication.getName(), chatId);
    }
}