package com.hashchat.messenger.message.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.user.service.UserIdentityService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TypingService {

    private final UserIdentityService userIdentityService;
    private final ChatParticipantRepository participantRepository;

    public boolean isTypingAllowed(String username, Long chatId) {
        var user = userIdentityService.resolve(username).orElse(null);
        if (user == null || !participantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            log.warn("Typing event denied for user={} chatId={}", username, chatId);
            return false;
        }
        return true;
    }
}
