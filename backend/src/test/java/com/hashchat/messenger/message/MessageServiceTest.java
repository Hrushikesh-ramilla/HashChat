package com.hashchat.messenger.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.hashchat.messenger.TestFixtures;
import com.hashchat.messenger.chat.domain.Message;
import com.hashchat.messenger.chat.domain.Chat;
import com.hashchat.messenger.chat.domain.ChatParticipant;
import com.hashchat.messenger.chat.domain.GroupRole;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.chat.repository.ChatRepository;
import com.hashchat.messenger.common.exception.MessageException;
import com.hashchat.messenger.crypto.device.CurrentDeviceService;
import com.hashchat.messenger.crypto.device.UserDevice;
import com.hashchat.messenger.crypto.device.UserDeviceRepository;
import com.hashchat.messenger.crypto.dto.EncryptedMessageEnvelopeInput;
import com.hashchat.messenger.crypto.dto.EncryptedSendMessageRequestV2;
import com.hashchat.messenger.infra.presence.UnreadService;
import com.hashchat.messenger.message.dto.DeviceMessageEventResponse;
import com.hashchat.messenger.message.dto.ReactionEvent;
import com.hashchat.messenger.message.repository.MessageEnvelopeRepository;
import com.hashchat.messenger.message.repository.MessageEventRepository;
import com.hashchat.messenger.message.repository.MessageReactionRepository;
import com.hashchat.messenger.message.repository.MessageReceiptRepository;
import com.hashchat.messenger.message.repository.MessageRepository;
import com.hashchat.messenger.message.service.MessageService;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.repository.UserRepository;
import com.hashchat.messenger.user.service.UserIdentityService;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService")
class MessageServiceTest {

    @Mock MessageRepository messageRepository;
    @Mock MessageEnvelopeRepository messageEnvelopeRepository;
    @Mock MessageEventRepository messageEventRepository;
    @Mock MessageReceiptRepository messageReceiptRepository;
    @Mock MessageReactionRepository messageReactionRepository;
    @Mock ChatParticipantRepository participantRepository;
    @Mock ChatRepository chatRepository;
    @Mock UserRepository userRepository;
    @Mock UserIdentityService userIdentityService;
    @Mock UserDeviceRepository userDeviceRepository;
    @Mock CurrentDeviceService currentDeviceService;
    @Mock UnreadService unreadService;
    @Mock com.hashchat.messenger.infra.presence.OnlineService onlineService;
    @Mock com.hashchat.messenger.push.service.PushNotificationService pushNotificationService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock ObjectMapper objectMapper;
    @Spy MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks MessageService messageService;

    User alice;
    UserDevice aliceDevice;

    @BeforeEach
    void setUp() {
        alice = TestFixtures.user(1L, "alice");
        aliceDevice = TestFixtures.device(10L, 1L, "device-alice-1");
    }

    @Test
    void returnsExistingMessageForSameClientMessageIdRetry() {
        Message existing = TestFixtures.sentMessage(100L, 5L, 1L, "device-alice-1");
        existing.setClientMessageId("client-100");

        when(userIdentityService.require("alice")).thenReturn(alice);
        when(currentDeviceService.requireCurrentDevice()).thenReturn(aliceDevice);
        when(participantRepository.existsByChatIdAndUserId(5L, 1L)).thenReturn(true);
        when(messageRepository.findBySenderIdAndSenderDeviceIdAndClientMessageId(1L, "device-alice-1", "client-100"))
                .thenReturn(Optional.of(existing));
        when(messageEnvelopeRepository.findByMessageIdAndTargetDeviceId(100L, "device-alice-1"))
                .thenReturn(Optional.empty());

        DeviceMessageEventResponse response = messageService.sendEncryptedMessageV2(
                "alice",
                sendRequest(5L, "client-100", "device-alice-1")
        );

        assertThat(response.messageId()).isEqualTo(100L);
        assertThat(response.clientMessageId()).isEqualTo("client-100");
        verify(messageRepository, never()).save(any());
        verify(messageEnvelopeRepository, never()).save(any());
        verify(unreadService, never()).increment(anyLong(), anyLong());
    }

    @Test
    void rejectsClientMessageIdConflictInsideSameSenderDeviceScope() {
        Message existing = TestFixtures.sentMessage(100L, 6L, 1L, "device-alice-1");
        existing.setClientMessageId("client-100");

        when(userIdentityService.require("alice")).thenReturn(alice);
        when(currentDeviceService.requireCurrentDevice()).thenReturn(aliceDevice);
        when(participantRepository.existsByChatIdAndUserId(5L, 1L)).thenReturn(true);
        when(messageRepository.findBySenderIdAndSenderDeviceIdAndClientMessageId(1L, "device-alice-1", "client-100"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> messageService.sendEncryptedMessageV2(
                "alice",
                sendRequest(5L, "client-100", "device-alice-1")
        ))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("clientMessageId");
    }

    @Test
    void markChatAsDeliveredWritesReceipt() {
        when(userIdentityService.require("alice")).thenReturn(alice);
        when(currentDeviceService.requireCurrentDevice()).thenReturn(aliceDevice);
        when(participantRepository.existsByChatIdAndUserId(5L, 1L)).thenReturn(true);
        when(messageRepository.findDistinctSenderIdsByChatIdAndSenderIdNotAndStatus(5L, 1L, Message.MessageStatus.SENT))
                .thenReturn(List.of(2L));
        when(messageReceiptRepository.upsertDeliveredForChat(eq(5L), eq(1L), eq("device-alice-1"), any(java.time.LocalDateTime.class)))
                .thenReturn(1);
        when(participantRepository.findDistinctUsernamesByChatId(5L)).thenReturn(List.of("alice", "bob"));
        when(userDeviceRepository.findByUserIdInAndActiveTrue(List.of(2L))).thenReturn(List.of());

        messageService.markChatAsDelivered("alice", 5L);

        verify(messageReceiptRepository).upsertDeliveredForChat(
                eq(5L), eq(1L), eq("device-alice-1"), any(java.time.LocalDateTime.class)
        );
        verify(messageRepository).recalculateAggregateStatusesForChat(5L, 1L);
    }

    @Test
    void markChatAsReadWritesReceipt() {
        when(userIdentityService.require("alice")).thenReturn(alice);
        when(currentDeviceService.requireCurrentDevice()).thenReturn(aliceDevice);
        when(participantRepository.existsByChatIdAndUserId(5L, 1L)).thenReturn(true);
        when(participantRepository.findUserIdsByChatId(5L)).thenReturn(List.of(1L, 2L));
        when(messageRepository.findDistinctSenderIdsByChatIdAndSenderIdNotAndStatusNot(5L, 1L, Message.MessageStatus.READ))
                .thenReturn(List.of(2L));
        when(messageReceiptRepository.upsertReadForChat(eq(5L), eq(1L), eq("device-alice-1"), any(java.time.LocalDateTime.class)))
                .thenReturn(1);
        when(participantRepository.findDistinctUsernamesByChatId(5L)).thenReturn(List.of("alice", "bob"));
        when(userDeviceRepository.findByUserIdInAndActiveTrue(List.of(2L))).thenReturn(List.of());

        messageService.markChatAsRead("alice", 5L);

        verify(unreadService).reset(1L, 5L);
        verify(messageReceiptRepository).upsertReadForChat(
                eq(5L), eq(1L), eq("device-alice-1"), any(java.time.LocalDateTime.class)
        );
        verify(messageRepository).recalculateAggregateStatusesForChat(5L, 1L);
    }

    @Test
    void senderCannotUpdateOwnStatus() {
        Message msg = TestFixtures.sentMessage(1L, 5L, 1L, "device-alice");

        when(userIdentityService.require("alice")).thenReturn(alice);
        when(currentDeviceService.requireCurrentDevice()).thenReturn(aliceDevice);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(msg));
        when(participantRepository.existsByChatIdAndUserId(5L, 1L)).thenReturn(true);

        messageService.updateMessageStatus("alice", 1L, "READ");

        verify(messageReceiptRepository, never()).save(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void toggleReactionAddsReaction() throws Exception {
        Message msg = TestFixtures.sentMessage(100L, 5L, 2L, "device-bob-1");

        when(userIdentityService.require("alice")).thenReturn(alice);
        when(currentDeviceService.requireCurrentDevice()).thenReturn(aliceDevice);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(msg));
        when(participantRepository.existsByChatIdAndUserId(5L, 1L)).thenReturn(true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(participantRepository.findUserIdsByChatId(5L)).thenReturn(List.of(1L, 2L));
        when(userDeviceRepository.findByUserIdInAndActiveTrue(any())).thenReturn(List.of());
        when(participantRepository.findDistinctUsernamesByChatId(5L)).thenReturn(List.of("alice", "bob"));

        ReactionEvent event = messageService.toggleReaction("alice", 100L, "👍");

        assertThat(event.type()).isEqualTo("MESSAGE_REACTION");
        assertThat(event.emoji()).isEqualTo("👍");
        assertThat(event.active()).isTrue();

        verify(messageReactionRepository).save(argThat(reaction ->
                reaction.getMessageId().equals(100L)
                        && reaction.getChatId().equals(5L)
                        && reaction.getUserId().equals(1L)
                        && reaction.getEmoji().equals("👍")
        ));
    }

    @Test
    void rejectsGroupMessageWhenWhoCanWritePolicyBlocksSender() {
        Chat group = new Chat();
        group.setId(5L);
        group.setType("GROUP");
        group.setWhoCanWrite("ADMINS");

        when(userIdentityService.require("alice")).thenReturn(alice);
        when(currentDeviceService.requireCurrentDevice()).thenReturn(aliceDevice);
        when(participantRepository.existsByChatIdAndUserId(5L, 1L)).thenReturn(true);
        when(chatRepository.findById(5L)).thenReturn(Optional.of(group));
        when(participantRepository.findByChatIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(new ChatParticipant(5L, 1L, GroupRole.MEMBER)));

        assertThatThrownBy(() -> messageService.sendEncryptedMessageV2(
                "alice",
                sendRequest(5L, "client-100", "device-alice-1")
        ))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("cannot write");
    }

    @Test
    void rejectsGroupMessageWhenSenderIsMuted() {
        Chat group = new Chat();
        group.setId(5L);
        group.setType("GROUP");
        group.setWhoCanWrite("ALL");
        ChatParticipant participant = new ChatParticipant(5L, 1L, GroupRole.MEMBER);
        participant.setMutedUntil(LocalDateTime.now().plusMinutes(5));

        when(userIdentityService.require("alice")).thenReturn(alice);
        when(currentDeviceService.requireCurrentDevice()).thenReturn(aliceDevice);
        when(participantRepository.existsByChatIdAndUserId(5L, 1L)).thenReturn(true);
        when(chatRepository.findById(5L)).thenReturn(Optional.of(group));
        when(participantRepository.findByChatIdAndUserId(5L, 1L)).thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> messageService.sendEncryptedMessageV2("alice", sendRequest(5L, "client-100", "device-alice-1")))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("muted");
    }

    @Test
    void rejectsGroupMessageWhenSenderIsBanned() {
        Chat group = new Chat();
        group.setId(5L);
        group.setType("GROUP");
        group.setWhoCanWrite("ALL");
        ChatParticipant participant = new ChatParticipant(5L, 1L, GroupRole.MEMBER);
        participant.setBannedAt(LocalDateTime.now());

        when(userIdentityService.require("alice")).thenReturn(alice);
        when(currentDeviceService.requireCurrentDevice()).thenReturn(aliceDevice);
        when(participantRepository.existsByChatIdAndUserId(5L, 1L)).thenReturn(true);
        when(chatRepository.findById(5L)).thenReturn(Optional.of(group));
        when(participantRepository.findByChatIdAndUserId(5L, 1L)).thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> messageService.sendEncryptedMessageV2("alice", sendRequest(5L, "client-100", "device-alice-1")))
                .isInstanceOf(MessageException.class)
                .hasMessageContaining("banned");
    }

    private static EncryptedSendMessageRequestV2 sendRequest(Long chatId, String clientMessageId, String senderDeviceId) {
        return new EncryptedSendMessageRequestV2(
                chatId,
                clientMessageId,
                senderDeviceId,
                List.of(new EncryptedMessageEnvelopeInput(
                        "device-alice-1", 1L, "WHISPER", "identity",
                        null, "ciphertext", "nonce", null, null, null, 1,
                        null, null
                )),
                null
        );
    }
}
