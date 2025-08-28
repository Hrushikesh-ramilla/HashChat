package com.hashchat.messenger.crypto.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hashchat.messenger.crypto.device.CurrentDeviceService;
import com.hashchat.messenger.crypto.dto.DeviceBundleDto;
import com.hashchat.messenger.crypto.dto.PreKeyBundleResponse;
import com.hashchat.messenger.crypto.dto.ResolvedChatDevicesResponse;
import com.hashchat.messenger.auth.service.CredentialRateLimiter;
import com.hashchat.messenger.crypto.prekey.PreKeyService;

@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class BundleController {

    private final PreKeyService preKeyService;
    private final CurrentDeviceService currentDeviceService;
    private final CredentialRateLimiter credentialRateLimiter;

    @GetMapping("/bundle/{username}")
    public PreKeyBundleResponse getBundle(@PathVariable String username, Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        return preKeyService.getBundleByUsername(authentication.getName(), username);
    }

    @PostMapping("/resolve-chat-devices/{chatId}")
    public ResolvedChatDevicesResponse resolveChatDevices(@PathVariable Long chatId, Authentication authentication) {
        currentDeviceService.requireCurrentDevice();
        return preKeyService.resolveChatDevices(authentication.getName(), chatId);
    }

    @PostMapping("/chats/{chatId}/devices/{targetDeviceId}/reserve-prekey")
    public DeviceBundleDto reserveOneTimePreKey(
            @PathVariable Long chatId,
            @PathVariable String targetDeviceId,
            Authentication authentication
    ) {
        currentDeviceService.requireCurrentDevice();
        credentialRateLimiter.checkUserAction(authentication.getName(), "prekey");
        return preKeyService.reserveChatDeviceOneTimePreKey(authentication.getName(), chatId, targetDeviceId);
    }
}