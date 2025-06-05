package com.hashchat.messenger.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.hashchat.messenger.user.domain.User;
import com.hashchat.messenger.user.repository.UserRepository;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserIdentityService {

    private final UserRepository userRepository;

    public User require(String identity) {
        return resolve(identity)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + safe(identity)));
    }

    public Optional<User> resolve(String identity) {
        if (identity == null || identity.isBlank()) {
            return Optional.empty();
        }

        String value = identity.trim();

        Optional<User> byUsername = userRepository.findByUsername(value);
        if (byUsername.isPresent()) {
            return byUsername;
        }

        Optional<User> byPhone = userRepository.findByPhone(value);
        if (byPhone.isPresent()) {
            return byPhone;
        }

        Optional<User> byEmail = userRepository.findByEmail(value);
        if (byEmail.isPresent()) {
            return byEmail;
        }

        if (value.matches("^\\d+$")) {
            try {
                return userRepository.findById(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private String safe(String identity) {
        if (identity == null) {
            return "<null>";
        }

        String value = identity.trim();
        if (value.length() <= 64) {
            return value;
        }

        return value.substring(0, 32) + "..." + value.substring(value.length() - 12);
    }
}