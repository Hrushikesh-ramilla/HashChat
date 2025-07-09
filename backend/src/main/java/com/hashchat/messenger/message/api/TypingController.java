package com.hashchat.messenger.message.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.hashchat.messenger.infra.ws.WebSocketAuthChannelInterceptor;
import com.hashchat.messenger.message.application.TypingService;
import com.hashchat.messenger.message.dto.TypingEvent;
import com.hashchat.messenger.message.dto.TypingRequest;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TypingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketAuthChannelInterceptor authInterceptor;
    private final TypingService typingService;

    @MessageMapping("/typing")
    public void typing(@Payload TypingRequest request, @Header("simpSessionId") String sessionId) {
        String username = authInterceptor.getUsernameBySessionId(sessionId);

        if (username == null) {
            log.warn("No username for typing event, sessionId: {}", sessionId);
            return;
        }

        if (request == null || request.chatId() == null) {
            log.warn("Invalid typing event from {}, missing chatId", username);
            return;
        }

        if (!typingService.isTypingAllowed(username, request.chatId())) {
            return;
        }

        log.debug("Typing event from {} in chat {}: {}", username, request.chatId(), request.typing());

        messagingTemplate.convertAndSend(
                "/topic/chats/" + request.chatId() + "/typing",
                new TypingEvent(username, request.typing())
        );
    }
}
