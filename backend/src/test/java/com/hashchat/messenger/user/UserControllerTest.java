package com.hashchat.messenger.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import com.hashchat.messenger.TestFixtures;
import com.hashchat.messenger.chat.repository.ChatParticipantRepository;
import com.hashchat.messenger.infra.security.JwtService;
import com.hashchat.messenger.user.api.UserController;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.dto.CurrentUserResponse;
import com.hashchat.messenger.user.dto.UpdateProfileRequest;
import com.hashchat.messenger.user.dto.UserProfileResponse;
import com.hashchat.messenger.user.repository.UserRepository;
import com.hashchat.messenger.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;
    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock ChatParticipantRepository participantRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock Authentication authentication;

    @InjectMocks UserController userController;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = TestFixtures.user(1L, "alice");
        alice.setFirstName("Alice");
        alice.setLastName("Smith");
        alice.setAvatarUrl("alice.png");

        bob = TestFixtures.user(2L, "bob");
        bob.setFirstName("Bob");
        bob.setLastName("Brown");
        bob.setAvatarUrl("bob.png");
    }

    @Test
    void searchReturnsPublicUserMaps() {
        when(userRepository.findByUsernameContainingIgnoreCase("ali"))
                .thenReturn(List.of(alice));

        List<Map<String, Object>> response = userController.search("ali");

        assertThat(response).hasSize(1);
        assertThat(response.get(0))
                .containsEntry("id", 1L)
                .containsEntry("username", "alice")
                .containsEntry("firstName", "Alice")
                .containsEntry("lastName", "Smith")
                .containsEntry("avatarUrl", "alice.png");
    }

    @Test
    void meDelegatesToUserService() {
        CurrentUserResponse expected = new CurrentUserResponse(
                1L,
                "alice",
                "alice@test.com",
                "Alice",
                "Smith",
                "alice.png",
                "legacy-key"
        );

        when(authentication.getName()).thenReturn("alice");
        when(userService.getCurrentUser("alice")).thenReturn(expected);

        assertThat(userController.me(authentication)).isSameAs(expected);
    }

    @Test
    void profileDelegatesToUserService() {
        UserProfileResponse expected = new UserProfileResponse(
                1L,
                "alice",
                "alice@test.com",
                "Alice",
                "Smith",
                "alice.png"
        );

        when(authentication.getName()).thenReturn("alice");
        when(userService.getProfile("alice")).thenReturn(expected);

        assertThat(userController.profile(authentication)).isSameAs(expected);
    }

    @Test
    void updateProfileReturnsNewTokenAndNotifiesSharedChats() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("alice_new");

        UserProfileResponse updated = new UserProfileResponse(
                1L,
                "alice_new",
                "alice@test.com",
                "Alice",
                "Smith",
                "new-avatar.png"
        );

        when(authentication.getName()).thenReturn("alice");
        when(userService.updateProfile("alice", request)).thenReturn(updated);
        when(jwtService.generateToken("alice_new")).thenReturn("jwt-new");

        when(participantRepository.findByUserId(1L))
                .thenReturn(List.of(
                        TestFixtures.participant(100L, 1L),
                        TestFixtures.participant(200L, 1L)
                ));

        when(participantRepository.findByChatId(100L))
                .thenReturn(List.of(
                        TestFixtures.participant(100L, 1L),
                        TestFixtures.participant(100L, 2L)
                ));

        when(participantRepository.findByChatId(200L))
                .thenReturn(List.of(
                        TestFixtures.participant(200L, 2L)
                ));

        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

        Map<String, Object> response = userController.updateProfile(authentication, request);

        assertThat(response)
                .containsEntry("id", 1L)
                .containsEntry("username", "alice_new")
                .containsEntry("email", "alice@test.com")
                .containsEntry("firstName", "Alice")
                .containsEntry("lastName", "Smith")
                .containsEntry("avatarUrl", "new-avatar.png")
                .containsEntry("token", "jwt-new");

        verify(messagingTemplate).convertAndSend(eq("/topic/users/alice/chats"), anyMap());
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/users/bob/chats"), anyMap());
    }
}