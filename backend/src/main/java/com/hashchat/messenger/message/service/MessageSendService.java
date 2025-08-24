package com.hashchat.messenger.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hashchat.messenger.chat.domain.Chat;
import com.hashchat.messenger.chat.domain.GroupPolicy;
import com.hashchat.messenger.chat.domain.Message;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.chat.repository.ChatRepository;
import com.hashchat.messenger.chat.service.ChatOutboxService;
import com.hashchat.messenger.common.exception.ChatException;
import com.hashchat.messenger.common.exception.MessageException;
import com.hashchat.messenger.crypto.device.UserDevice;
import com.hashchat.messenger.crypto.dto.EncryptedSendMessageRequestV2;
import com.hashchat.messenger.message.access.MessageAccessService;
import com.hashchat.messenger.message.domain.MessageEnvelope;
import com.hashchat.messenger.message.dto.DeviceMessageEventResponse;
import com.hashchat.messenger.message.repository.MessageEnvelopeRepository;
import com.hashchat.messenger.message.repository.MessageRepository;
import com.hashchat.messenger.user.domain.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSendService {

    private final MessageRepository messageRepository;
    private final MessageEnvelopeRepository messageEnvelopeRepository;
    private final ChatRepository chatRepository;
    private final ChatParticipantRepository participantRepository;
    private final MessageAccessService messageAccessService;
    private final MessageFanoutService messageFanoutService;
    private final MessageOutboxService messageOutboxService;
    private final ChatOutboxService chatOutboxService;
    private final MessageEnvelopeService messageEnvelopeService;

    @Transactional
    public DeviceMessageEventResponse sendEncryptedMessageV2(String username, EncryptedSendMessageRequestV2 request) {
        User sender = messageAccessService.requireUser(username);
        UserDevice currentDevice = messageAccessService.requireCurrentDevice();
        validateBasicSendRequest(sender, currentDevice, request);

        Optional<Message> existing = messageRepository.findBySenderIdAndSenderDeviceIdAndClientMessageId(
                sender.getId(),
                currentDevice.getDeviceId(),
                request.clientMessageId()
        );
        if (existing.isPresent()) {
            return buildIdempotentSendResponse(existing.get(), request, currentDevice, sender.getId());
        }

        Map<String, UserDevice> targetDevices = messageEnvelopeService.validateEnvelopeTargets(
                request.chatId(),
                request.envelopes(),
                currentDevice
        );

        Message message = new Message();
        message.setChatId(request.chatId());
        message.setSenderId(sender.getId());
        message.setSenderDeviceId(currentDevice.getDeviceId());
        message.setClientMessageId(request.clientMessageId());
        message.setContent("[encrypted]");
        message.setCreatedAt(LocalDateTime.now());
        message.setStatus(Message.MessageStatus.SENT);
        if (request.selfDestructSeconds() != null && request.selfDestructSeconds() > 0) {
            message.setExpiresAt(LocalDateTime.now().plusSeconds(request.selfDestructSeconds()));
        }
        try {
            message = messageRepository.save(message);
            messageRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            Message existingAfterRace = messageRepository.findBySenderIdAndSenderDeviceIdAndClientMessageId(
                            sender.getId(),
                            currentDevice.getDeviceId(),
                            request.clientMessageId()
                    )
                    .orElseThrow(() -> ex);
            return buildIdempotentSendResponse(existingAfterRace, request, currentDevice, sender.getId());
        }

        messageFanoutService.incrementCounter("messages_sent_total");

        Map<String, MessageEnvelope> byDevice = messageEnvelopeService.persistEnvelopes(message, sender, request.envelopes(), targetDevices);

        messageOutboxService.messageCreated(message, byDevice);
        messageOutboxService.chatListUpdated(message.getChatId(), "message_created");
        Chat chat = chatRepository.findById(request.chatId()).orElse(null);
        if (chat != null
                && "DIRECT".equals(chat.getType())
                && "PENDING".equalsIgnoreCase(chat.getDirectStatus())) {
            chatOutboxService.requestUpdated(message.getChatId(), "request_message");
        }

        return messageFanoutService.toDeviceEvent("MESSAGE_CREATED", message, byDevice.get(currentDevice.getDeviceId()), sender.getId());
    }

    private void validateBasicSendRequest(User sender, UserDevice currentDevice, EncryptedSendMessageRequestV2 request) {
        if (request == null || request.chatId() == null) {
            throw new IllegalArgumentException("chatId is required");
        }
        if (request.clientMessageId() == null || request.clientMessageId().isBlank()) {
            throw new IllegalArgumentException("clientMessageId is required");
        }
        if (request.senderDeviceId() == null || !request.senderDeviceId().equals(currentDevice.getDeviceId())) {
            throw new MessageException("senderDeviceId must match current X-Device-Id");
        }

        messageAccessService.requireParticipant(request.chatId(), sender.getId());

        Chat chat = chatRepository.findById(request.chatId()).orElse(null);
        if (chat != null && "DIRECT".equals(chat.getType())) {
            String st = chat.getDirectStatus();
            if ("PENDING".equalsIgnoreCase(st)) {
                Long requestedBy = chat.getDirectRequestedBy();
                if (requestedBy != null && !requestedBy.equals(sender.getId())) {
                    throw new MessageException("Chat request not accepted");
                }
                if (requestedBy != null && requestedBy.equals(sender.getId())) {
                    long senderMessages = messageRepository.countByChatIdAndSenderIdAndDeletedAtIsNull(
                            request.chatId(),
                            sender.getId()
                    );
                    if (senderMessages >= 1) {
                        throw new MessageException("Only one message is allowed until request is accepted");
                    }
                }
            }
            if ("DECLINED".equalsIgnoreCase(st)) {
                throw new MessageException("Chat request was declined");
            }
        }
        if (chat != null && "GROUP".equals(chat.getType())) {
            var participant = participantRepository.findByChatIdAndUserId(request.chatId(), sender.getId())
                    .orElseThrow(() -> new ChatException("You are not a participant of this chat"));
            if (participant.isBanned()) {
                throw new MessageException("You are banned in this group");
            }
            if (participant.isMutedNow()) {
                throw new MessageException("You are muted in this group");
            }
            GroupPolicy policy = GroupPolicy.fromString(chat.getWhoCanWrite(), GroupPolicy.ALL);
            if (policy != GroupPolicy.ALL && policy != GroupPolicy.ANYONE) {
                if (!participant.groupRole().atLeast(policy.minRole())) {
                    throw new MessageException("You cannot write to this group");
                }
            }
        }
    }

    private DeviceMessageEventResponse buildIdempotentSendResponse(
            Message existingMessage,
            EncryptedSendMessageRequestV2 request,
            UserDevice currentDevice,
            Long senderUserId
    ) {
        boolean sameMessage = Objects.equals(existingMessage.getChatId(), request.chatId())
                && Objects.equals(existingMessage.getSenderDeviceId(), currentDevice.getDeviceId());
        if (!sameMessage) {
            throw new MessageException("clientMessageId already belongs to another message");
        }

        MessageEnvelope currentEnvelope = messageEnvelopeRepository
                .findByMessageIdAndTargetDeviceId(existingMessage.getId(), currentDevice.getDeviceId())
                .orElse(null);
        return messageFanoutService.toDeviceEvent("MESSAGE_CREATED", existingMessage, currentEnvelope, senderUserId);
    }
}
