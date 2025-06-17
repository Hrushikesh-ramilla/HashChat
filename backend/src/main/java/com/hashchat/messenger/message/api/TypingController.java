package com.hashchat.messenger.message.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.infra.ws.WebSocketAuthChannelInterceptor;
import com.hashchat.messenger.message.dto.TypingEvent;
import com.hashchat.messenger.message.dto.TypingRequest;
import com.hashchat.messenger.user.service.UserIdentityService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TypingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketAuthChannelInterceptor authInterceptor;
    private final UserIdentityService userIdentityService;
    private final ChatParticipantRepository participantRepository;

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

        var user = userIdentityService.resolve(username).orElse(null);
        if (user == null || !participantRepository.existsByChatIdAndUserId(request.chatId(), user.getId())) {
            log.warn("Typing event denied for user={} chatId={}", username, request.chatId());
            return;
        }

        log.debug("Typing event from {} in chat {}: {}", username, request.chatId(), request.typing());

        messagingTemplate.convertAndSend(
                "/topic/chats/" + request.chatId() + "/typing",
                new TypingEvent(username, request.typing())
        );
    }
}
