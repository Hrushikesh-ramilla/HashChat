package com.hashchat.messenger.call.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import com.hashchat.messenger.call.service.CallSignalingService;
import com.hashchat.messenger.infra.ws.WebSocketAuthChannelInterceptor;

@Slf4j
@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hashchat.calls.enabled", havingValue = "true")
public class CallController {

    private final CallSignalingService callSignalingService;
    private final WebSocketAuthChannelInterceptor authInterceptor;

    @MessageMapping("/call")
    public void call(@Payload CallSignalRequest request, @Header("simpSessionId") String sessionId) {
        String username = authInterceptor.getUsernameBySessionId(sessionId);
        String deviceId = authInterceptor.getDeviceIdBySessionId(sessionId);
        if (username == null || deviceId == null) {
            log.warn("Call signal dropped: missing session identity sessionId={}", sessionId);
            return;
        }
        callSignalingService.relay(username, deviceId, request);
    }
}
