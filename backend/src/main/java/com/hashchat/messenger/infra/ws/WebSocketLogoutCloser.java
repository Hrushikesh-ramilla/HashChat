package com.hashchat.messenger.infra.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.hashchat.messenger.realtime.WebSocketSessionRegistry;

@Component
@RequiredArgsConstructor
public class WebSocketLogoutCloser {

    private final WebSocketSessionRegistry sessionRegistry;
    private final WebSocketNativeSessionTracker nativeSessions;

    public void closeSessionsForUsername(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        for (String sessionId : sessionRegistry.sessionIdsForUsername(username)) {
            nativeSessions.close(sessionId);
            sessionRegistry.unregister(sessionId);
        }
    }
}
