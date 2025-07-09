package com.hashchat.messenger.push.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.hashchat.messenger.crypto.device.CurrentDeviceService;
import com.hashchat.messenger.push.dto.PushSubscriptionRequest;
import com.hashchat.messenger.push.service.PushNotificationService;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.service.UserIdentityService;

@Tag(name = "Push Notifications", description = "Web Push subscription management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushNotificationService pushNotificationService;
    private final UserIdentityService userIdentityService;
    private final CurrentDeviceService currentDeviceService;

    @Operation(summary = "Subscribe to push notifications")
    @PostMapping("/subscribe")
    public void subscribe(@Valid @RequestBody PushSubscriptionRequest request, Authentication auth) {
        User user = userIdentityService.require(auth.getName());
        String deviceId = currentDeviceService.requireCurrentDevice().getDeviceId();
        pushNotificationService.subscribe(
                user.getId(),
                deviceId,
                request.endpoint(),
                request.p256dh(),
                request.auth()
        );
    }

    @Operation(summary = "Unsubscribe from push notifications")
    @DeleteMapping("/unsubscribe")
    public void unsubscribe(Authentication auth) {
        User user = userIdentityService.require(auth.getName());
        String deviceId = currentDeviceService.requireCurrentDevice().getDeviceId();
        pushNotificationService.unsubscribe(user.getId(), deviceId);
    }

    @Operation(summary = "Get VAPID public key")
    @GetMapping("/vapid-public-key")
    public String getVapidPublicKey() {
        return pushNotificationService.getVapidPublicKey();
    }
}
