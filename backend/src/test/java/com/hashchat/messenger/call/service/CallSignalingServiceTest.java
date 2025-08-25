package com.hashchat.messenger.call.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashchat.messenger.TestFixtures;
import com.hashchat.messenger.call.api.CallEvent;
import com.hashchat.messenger.call.api.CallSignalRequest;
import com.hashchat.messenger.call.api.IceCandidatePayload;
import com.hashchat.messenger.chat.domain.Chat;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.chat.repository.ChatRepository;
import com.hashchat.messenger.crypto.device.UserDevice;
import com.hashchat.messenger.crypto.device.UserDeviceRepository;
import com.hashchat.messenger.realtime.StompEventPublisher;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.service.UserIdentityService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallSignalingServiceTest {

    @Mock UserIdentityService userIdentityService;
    @Mock ChatRepository chatRepository;
    @Mock ChatParticipantRepository participantRepository;
    @Mock UserDeviceRepository userDeviceRepository;
    @Mock StompEventPublisher stompEventPublisher;

    @InjectMocks CallSignalingService callSignalingService;

    private User alice;
    private Chat direct;

    @BeforeEach
    void setUp() {
        alice = TestFixtures.user(1L, "alice");
        direct = TestFixtures.directChat(100L);
        direct.setDirectStatus("ACCEPTED");
    }

    @Test
    void relaysOfferToOtherParticipantDevicesAndSkipsSender() {
        UserDevice aliceDevice = TestFixtures.device(10L, 1L, "alice-phone");
        UserDevice bobDevice = TestFixtures.device(20L, 2L, "bob-laptop");
        UserDevice aliceTablet = TestFixtures.device(11L, 1L, "alice-tablet");

        when(userIdentityService.resolve("alice")).thenReturn(Optional.of(alice));
        when(participantRepository.existsByChatIdAndUserId(100L, 1L)).thenReturn(true);
        when(chatRepository.findById(100L)).thenReturn(Optional.of(direct));
        when(participantRepository.findUserIdsByChatId(100L)).thenReturn(List.of(1L, 2L));
        when(userDeviceRepository.findByUserIdInAndActiveTrue(List.of(1L, 2L)))
                .thenReturn(List.of(aliceDevice, bobDevice, aliceTablet));

        callSignalingService.relay("alice", "alice-phone", new CallSignalRequest(
                100L, "OFFER", "v=0", null, true, null
        ));

        ArgumentCaptor<CallEvent> captor = ArgumentCaptor.forClass(CallEvent.class);
        verify(stompEventPublisher).publishToDevice(eq("bob-laptop"), eq("/calls"), captor.capture());
        verify(stompEventPublisher).publishToDevice(eq("alice-tablet"), eq("/calls"), any(CallEvent.class));
        verify(stompEventPublisher, never()).publishToDevice(eq("alice-phone"), anyString(), any());

        CallEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo("offer");
        assertThat(event.chatId()).isEqualTo(100L);
        assertThat(event.fromUsername()).isEqualTo("alice");
        assertThat(event.fromDeviceId()).isEqualTo("alice-phone");
        assertThat(event.sdp()).isEqualTo("v=0");
        assertThat(event.video()).isTrue();
    }

    @Test
    void relaysIceCandidatePayload() {
        when(userIdentityService.resolve("alice")).thenReturn(Optional.of(alice));
        when(participantRepository.existsByChatIdAndUserId(100L, 1L)).thenReturn(true);
        when(chatRepository.findById(100L)).thenReturn(Optional.of(direct));
        when(participantRepository.findUserIdsByChatId(100L)).thenReturn(List.of(1L, 2L));
        when(userDeviceRepository.findByUserIdInAndActiveTrue(List.of(1L, 2L)))
                .thenReturn(List.of(TestFixtures.device(20L, 2L, "bob-laptop")));

        IceCandidatePayload candidate = new IceCandidatePayload("candidate:1", "0", 0, "frag");
        callSignalingService.relay("alice", "alice-phone", new CallSignalRequest(
                100L, "ice", null, candidate, null, null
        ));

        ArgumentCaptor<CallEvent> captor = ArgumentCaptor.forClass(CallEvent.class);
        verify(stompEventPublisher).publishToDevice(eq("bob-laptop"), eq("/calls"), captor.capture());
        assertThat(captor.getValue().candidate()).isEqualTo(candidate);
    }

    @Test
    void dropsSignalWhenChatIsNotAcceptedDirect() {
        Chat pending = TestFixtures.directChat(100L);
        pending.setDirectStatus("PENDING");
        when(userIdentityService.resolve("alice")).thenReturn(Optional.of(alice));
        when(participantRepository.existsByChatIdAndUserId(100L, 1L)).thenReturn(true);
        when(chatRepository.findById(100L)).thenReturn(Optional.of(pending));

        callSignalingService.relay("alice", "alice-phone", new CallSignalRequest(100L, "offer", "v=0", null, null, null));

        verify(stompEventPublisher, never()).publishToDevice(anyString(), anyString(), any());
    }

    @Test
    void dropsSignalForGroupChats() {
        when(userIdentityService.resolve("alice")).thenReturn(Optional.of(alice));
        when(participantRepository.existsByChatIdAndUserId(100L, 1L)).thenReturn(true);
        when(chatRepository.findById(100L)).thenReturn(Optional.of(TestFixtures.groupChat(100L, "team")));

        callSignalingService.relay("alice", "alice-phone", new CallSignalRequest(100L, "offer", "v=0", null, null, null));

        verify(stompEventPublisher, never()).publishToDevice(anyString(), anyString(), any());
    }

    @Test
    void dropsSignalWhenUserIsNotParticipant() {
        when(userIdentityService.resolve("alice")).thenReturn(Optional.of(alice));
        when(participantRepository.existsByChatIdAndUserId(100L, 1L)).thenReturn(false);

        callSignalingService.relay("alice", "alice-phone", new CallSignalRequest(100L, "offer", "v=0", null, null, null));

        verify(chatRepository, never()).findById(any());
        verify(stompEventPublisher, never()).publishToDevice(anyString(), anyString(), any());
    }

    @Test
    void dropsUnknownSignalType() {
        callSignalingService.relay("alice", "alice-phone", new CallSignalRequest(100L, "screen-share", null, null, null, null));

        verify(userIdentityService, never()).resolve(anyString());
        verify(stompEventPublisher, never()).publishToDevice(anyString(), anyString(), any());
    }
}
