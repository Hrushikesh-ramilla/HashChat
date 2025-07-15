package com.hashchat.messenger.message.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.hashchat.messenger.crypto.dto.EncryptedEditMessageRequestV2;
import com.hashchat.messenger.crypto.dto.EncryptedSendMessageRequestV2;
import com.hashchat.messenger.message.dto.DeviceMessageEventResponse;
import com.hashchat.messenger.message.dto.MessageTimelineItemResponse;
import com.hashchat.messenger.message.dto.ReactionEvent;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSendService messageSendService;
    private final MessageEditService messageEditService;
    private final MessageDeleteService messageDeleteService;
    private final MessageReceiptService messageReceiptService;
    private final MessageReactionService messageReactionService;
    private final MessageTimelineService messageTimelineService;

    public DeviceMessageEventResponse sendEncryptedMessageV2(String username, EncryptedSendMessageRequestV2 request) {
        return messageSendService.sendEncryptedMessageV2(username, request);
    }

    public DeviceMessageEventResponse editEncryptedMessageV2(String username, Long messageId, EncryptedEditMessageRequestV2 request) {
        return messageEditService.editEncryptedMessageV2(username, messageId, request);
    }

    public List<MessageTimelineItemResponse> getChatTimeline(String username, Long chatId, Long beforeMessageId, int limit) {
        return messageTimelineService.getChatTimeline(username, chatId, beforeMessageId, limit);
    }

    public void deleteMessage(String username, Long messageId) {
        messageDeleteService.deleteMessage(username, messageId);
    }

    public void markChatAsRead(String username, Long chatId) {
        messageReceiptService.markChatAsRead(username, chatId);
    }

    public void markChatAsDelivered(String username, Long chatId) {
        messageReceiptService.markChatAsDelivered(username, chatId);
    }

    public void updateMessageStatus(String username, Long messageId, String status) {
        messageReceiptService.updateMessageStatus(username, messageId, status);
    }

    public ReactionEvent toggleReaction(String username, Long messageId, String emoji) {
        return messageReactionService.toggleReaction(username, messageId, emoji);
    }
}
