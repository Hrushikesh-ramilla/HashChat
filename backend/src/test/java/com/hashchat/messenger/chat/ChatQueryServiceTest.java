package com.hashchat.messenger.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashchat.messenger.TestFixtures;
import com.hashchat.messenger.chat.access.ChatAccessService;
import com.hashchat.messenger.chat.access.ChatQueryService;
import com.hashchat.messenger.chat.domain.Chat;
import com.hashchat.messenger.chat.domain.ChatParticipant;
import com.hashchat.messenger.chat.dto.ChatResponse;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.chat.repository.ChatRepository;
import com.hashchat.messenger.infra.presence.OnlineService;
import com.hashchat.messenger.infra.presence.UnreadService;
import com.hashchat.messenger.message.repository.MessageRepository;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatQueryService")
class ChatQueryServiceTest {

    @Mock UserRepository userRepository;
    @Mock ChatRepository chatRepository;
    @Mock ChatParticipantRepository participantRepository;
    @Mock MessageRepository messageRepository;
    @Mock UnreadService unreadService;
    @Mock OnlineService onlineService;
    @Mock ChatAccessService chatAccessService;

    @InjectMocks ChatQueryService chatQueryService;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        alice = TestFixtures.user(1L, "alice");
        bob = TestFixtures.user(2L, "bob");
    }

    @Nested
    @DisplayName("getMyChats")
    class GetMyChats {

        @Test
        @DisplayName("returns an empty list when there are no chats")
        void returnsEmptyList() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
            when(chatRepository.findChatIdsByUserIdOrderByActivity(1L, 100, 0)).thenReturn(List.of());

            assertThat(chatQueryService.getMyChats("alice")).isEmpty();
        }

        @Test
        @DisplayName("returns a DIRECT chat with peer data")
        void returnsDirectChatWithOtherUser() {
            Chat chat = TestFixtures.directChat(5L);
            ChatParticipant pAlice = TestFixtures.participant(5L, 1L);
            ChatParticipant pBob = TestFixtures.participant(5L, 2L);

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
            when(chatRepository.findChatIdsByUserIdOrderByActivity(1L, 100, 0)).thenReturn(List.of(5L));
            when(chatRepository.findByIdIn(List.of(5L))).thenReturn(List.of(chat));
            when(participantRepository.findByChatIdIn(List.of(5L))).thenReturn(List.of(pAlice, pBob));
            when(userRepository.findAllById(anySet())).thenReturn(List.of(bob));
            when(messageRepository.findLatestByChatIds(List.of(5L))).thenReturn(List.of());
            when(unreadService.getMany(1L, List.of(5L))).thenReturn(java.util.Map.of(5L, 3L));
            when(onlineService.isOnlineMany(Set.of("bob"))).thenReturn(java.util.Map.of("bob", true));
            when(onlineService.getLastSeenMany(Set.of("bob"))).thenReturn(java.util.Map.of());

            List<ChatResponse> result = chatQueryService.getMyChats("alice");

            assertThat(result).hasSize(1);
            ChatResponse r = result.get(0);
            assertThat(r.chatId()).isEqualTo(5L);
            assertThat(r.type()).isEqualTo("DIRECT");
            assertThat(r.otherUsername()).isEqualTo("bob");
            assertThat(r.unreadCount()).isEqualTo(3L);
            assertThat(r.online()).isTrue();
        }

        @Test
        @DisplayName("returns a GROUP chat with the group name")
        void returnsGroupChatWithName() {
            Chat group = TestFixtures.groupChat(7L, "Project X");
            ChatParticipant pAlice = TestFixtures.participant(7L, 1L);
            ChatParticipant pBob = TestFixtures.participant(7L, 2L);

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
            when(chatRepository.findChatIdsByUserIdOrderByActivity(1L, 100, 0)).thenReturn(List.of(7L));
            when(chatRepository.findByIdIn(List.of(7L))).thenReturn(List.of(group));
            when(participantRepository.findByChatIdIn(List.of(7L))).thenReturn(List.of(pAlice, pBob));
            when(userRepository.findAllById(anySet())).thenReturn(List.of(bob));
            when(messageRepository.findLatestByChatIds(List.of(7L))).thenReturn(List.of());
            when(unreadService.getMany(1L, List.of(7L))).thenReturn(java.util.Map.of(7L, 0L));

            List<ChatResponse> result = chatQueryService.getMyChats("alice");

            assertThat(result).hasSize(1);
            ChatResponse r = result.get(0);
            assertThat(r.type()).isEqualTo("GROUP");
            assertThat(r.name()).isEqualTo("Project X");
            assertThat(r.otherUsername()).isNull();
        }
    }
}
