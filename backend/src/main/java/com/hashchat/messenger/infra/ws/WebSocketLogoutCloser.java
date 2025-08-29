package com.hashchat.messenger.infra.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.hashchat.messenger.auth.service.RefreshTokenService;
import com.hashchat.messenger.realtime.WebSocketSessionRegistry;

@Component
@RequiredArgsConstructor
public class WebSocketLogoutCloser {

    private final WebSocketSessionRegistry sessionRegistry;
    private final WebSocketNativeSessionTracker nativeSessions;
    private final RefreshTokenService refreshTokenService;

    public void closeSessionsForUsername(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        for (String sessionId : sessionRegistry.sessionIdsForUsername(username)) {
            nativeSessions.close(sessionId);
            sessionRegistry.unregister(sessionId);
        }
    }

    public void closeSessionsForDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        for (String sessionId : sessionRegistry.sessionIdsForDevice(deviceId)) {
            refreshTokenService.revokeFamily(sessionRegistry.familyId(sessionId));
            nativeSessions.close(sessionId);
            sessionRegistry.unregister(sessionId);
        }
    }
}
