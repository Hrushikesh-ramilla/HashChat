package com.hashchat.messenger.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hashchat.messenger.chat.domain.Message;
import com.hashchat.messenger.common.exception.MessageException;
import com.hashchat.messenger.message.access.MessageAccessService;
import com.hashchat.messenger.message.repository.MessageRepository;
import com.hashchat.messenger.user.domain.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDeleteService {

    private final MessageRepository messageRepository;
    private final MessageAccessService messageAccessService;
    private final MessageFanoutService messageFanoutService;
    private final MessageOutboxService messageOutboxService;

    @Transactional
    public void deleteMessage(String username, Long messageId) {
        User user = messageAccessService.requireUser(username);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));

        messageAccessService.requireParticipant(message.getChatId(), user.getId());

        if (!Objects.equals(message.getSenderId(), user.getId())) {
            throw new MessageException("You can only delete your own messages");
        }
        if (message.getDeletedAt() != null) {
            return;
        }

        message.setDeletedAt(LocalDateTime.now());
        messageRepository.save(message);
        messageFanoutService.incrementCounter("messages_deleted_total");
        messageFanoutService.saveMessageEvent(message, user.getId(), "DELETE", Map.of());

        messageOutboxService.messageDeleted(message);
        messageOutboxService.chatListUpdated(message.getChatId(), "message_deleted");
    }
}
