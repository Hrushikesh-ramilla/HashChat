package com.hashchat.messenger.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hashchat.messenger.chat.domain.Chat;
import com.hashchat.messenger.chat.domain.ChatParticipant;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.chat.repository.ChatRepository;
import com.hashchat.messenger.common.exception.ChatException;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SavedMessagesService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ChatOutboxService chatOutboxService;

    @Transactional
    public Long createOrGetSavedMessagesChat(String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ChatException("Current user not found"));

        Optional<Long> existing = participantRepository.findSavedChatId(user.getId());
        if (existing.isPresent()) {
            Long chatId = existing.get();
            chatOutboxService.chatListUpdated(chatId, "saved_chat_exists");
            return chatId;
        }

        Chat chat = new Chat();
        chat.setType("SAVED");
        chat.setName("SAVED");
        chat.setCreatedAt(LocalDateTime.now());
        chat = chatRepository.save(chat);

        participantRepository.save(new ChatParticipant(chat.getId(), user.getId()));

        Long chatId = chat.getId();
        chatOutboxService.chatListUpdated(chatId, "saved_chat_created");
        return chatId;
    }
}
