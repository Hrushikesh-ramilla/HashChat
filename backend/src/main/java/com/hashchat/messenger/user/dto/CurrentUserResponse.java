package com.hashchat.messenger.user.dto;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        String publicKey
) {}
