package com.hashchat.messenger.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hashchat.messenger.chat.domain.Message;
import com.hashchat.messenger.common.TransactionUtils;
import com.hashchat.messenger.common.exception.MessageException;
import com.hashchat.messenger.crypto.device.UserDevice;
import com.hashchat.messenger.crypto.dto.EncryptedEditMessageRequestV2;
import com.hashchat.messenger.message.access.MessageAccessService;
import com.hashchat.messenger.message.domain.MessageEnvelope;
import com.hashchat.messenger.message.dto.DeviceMessageEventResponse;
import com.hashchat.messenger.message.repository.MessageEnvelopeRepository;
import com.hashchat.messenger.message.repository.MessageRepository;
import com.hashchat.messenger.user.domain.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageEditService {

    private final MessageRepository messageRepository;
    private final MessageEnvelopeRepository messageEnvelopeRepository;
    private final MessageAccessService messageAccessService;
    private final MessageSendService messageSendService;
    private final MessageFanoutService messageFanoutService;

    @Transactional
    public DeviceMessageEventResponse editEncryptedMessageV2(String username, Long messageId, EncryptedEditMessageRequestV2 request) {
        User sender = messageAccessService.requireUser(username);
        UserDevice currentDevice = messageAccessService.requireCurrentDevice();
        Message message = messageRepository.findByIdForUpdate(messageId)
                .orElseThrow(() -> new MessageException("Message not found"));

        messageAccessService.requireParticipant(message.getChatId(), sender.getId());

        if (!Objects.equals(message.getSenderId(), sender.getId())) {
            throw new MessageException("You can only edit your own messages");
        }
        if (message.getDeletedAt() != null) {
            throw new MessageException("Deleted messages cannot be edited");
        }
        if (request.senderDeviceId() == null || !request.senderDeviceId().equals(currentDevice.getDeviceId())) {
            throw new MessageException("senderDeviceId must match current X-Device-Id");
        }

        Map<String, UserDevice> targetDevices = messageSendService.validateEnvelopeTargets(
                message.getChatId(),
                request.envelopes(),
                currentDevice
        );

        message.setVersion(message.getVersion() + 1);
        message.setEditedAt(LocalDateTime.now());
        messageRepository.save(message);
        messageFanoutService.incrementCounter("messages_edited_total");

        messageEnvelopeRepository.deleteByMessageId(messageId);
        messageEnvelopeRepository.flush();

        Map<String, MessageEnvelope> byDevice = messageSendService.persistEnvelopes(message, sender, request.envelopes(), targetDevices);
        messageFanoutService.saveMessageEvent(message, sender.getId(), "EDIT", Map.of("version", message.getVersion()));

        final Message msgEditFinal = message;
        final Map<String, MessageEnvelope> byDeviceEditFinal = byDevice;
        TransactionUtils.afterCommit(() -> {
            try {
                messageFanoutService.fanoutEditedEvent(msgEditFinal, byDeviceEditFinal);
                messageFanoutService.notifyChatListUpdated(msgEditFinal.getChatId(), "message_edited");
            } catch (Exception e) {
                log.error("afterCommit edit fanout failed for message {}", msgEditFinal.getId(), e);
            }
        });

        return messageFanoutService.toDeviceEvent("MESSAGE_EDITED", message, byDevice.get(currentDevice.getDeviceId()), sender.getId());
    }
}
