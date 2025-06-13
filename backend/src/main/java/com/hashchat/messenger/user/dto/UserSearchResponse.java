package com.hashchat.messenger.user.dto;

public record UserSearchResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String avatarUrl
) {
}
