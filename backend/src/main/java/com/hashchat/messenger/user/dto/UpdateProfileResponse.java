package com.hashchat.messenger.user.dto;

public record UpdateProfileResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String bio,
        String avatarUrl,
        String token
) {
}
