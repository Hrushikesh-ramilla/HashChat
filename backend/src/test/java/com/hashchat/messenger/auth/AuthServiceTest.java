package com.hashchat.messenger.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import com.hashchat.messenger.auth.service.AuthService;
import com.hashchat.messenger.auth.service.DeviceRegistrationTokenService;
import com.hashchat.messenger.auth.service.PhoneVerificationService;
import com.hashchat.messenger.auth.service.RefreshTokenService;
import com.hashchat.messenger.auth.service.SetupTokenService;
import com.hashchat.messenger.infra.security.JwtService;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.domain.UserStatus;
import com.hashchat.messenger.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock PhoneVerificationService verificationService;
    @Mock UserRepository userRepository;
    @Mock RefreshTokenService refreshTokenService;
    @Mock DeviceRegistrationTokenService deviceRegTokenService;
    @Mock JwtService jwtService;
    @Mock SetupTokenService setupTokenService;
    @Mock PasswordEncoder passwordEncoder;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                verificationService,
                userRepository,
                refreshTokenService,
                deviceRegTokenService,
                jwtService,
                setupTokenService,
                passwordEncoder
        );
    }

    @Test
    void completeSetupDoesNotConsumeTokenWhenUsernameIsTaken() {
        User user = phoneUser();
        when(setupTokenService.getPhone("setup-token")).thenReturn("+79001234567");
        when(userRepository.findByPhone("+79001234567")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.completeSetup("setup-token", "Alice", "Alice", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(setupTokenService, never()).consumePhone("setup-token");
        verify(userRepository, never()).save(user);
        verifyNoInteractions(jwtService, refreshTokenService, deviceRegTokenService);
    }

    @Test
    void completeSetupConsumesTokenAfterValidationAndSavesProfile() {
        User user = phoneUser();
        when(setupTokenService.getPhone("setup-token")).thenReturn("+79001234567");
        when(userRepository.findByPhone("+79001234567")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(setupTokenService.consumePhone("setup-token")).thenReturn("+79001234567");
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateToken("alice")).thenReturn("jwt");
        when(refreshTokenService.issue("alice")).thenReturn("refresh");
        when(deviceRegTokenService.issue("alice")).thenReturn("device-token");

        var response = service.completeSetup(
                "setup-token",
                " Alice ",
                " Alice ",
                " Smith ",
                " avatar.png "
        );

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getAvatarUrl()).isEqualTo("avatar.png");
        assertThat(response.status()).isEqualTo("ok");
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.deviceRegistrationToken()).isEqualTo("device-token");
        verify(setupTokenService).consumePhone("setup-token");
        verify(userRepository).save(user);
    }

    @Test
    void completeSetupRejectsRaceWhenTokenWasConsumedAfterValidation() {
        User user = phoneUser();
        when(setupTokenService.getPhone("setup-token")).thenReturn("+79001234567");
        when(userRepository.findByPhone("+79001234567")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(setupTokenService.consumePhone("setup-token")).thenReturn(null);

        assertThatThrownBy(() -> service.completeSetup("setup-token", "alice", "Alice", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(userRepository, never()).save(user);
        verifyNoInteractions(jwtService, refreshTokenService, deviceRegTokenService);
    }

    private static User phoneUser() {
        User user = new User();
        user.setId(1L);
        user.setPhone("+79001234567");
        user.setUsername("pending_1");
        user.setEmail("phone-79001234567@local.invalid");
        user.setPasswordHash("PHONE_AUTH_ONLY");
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
