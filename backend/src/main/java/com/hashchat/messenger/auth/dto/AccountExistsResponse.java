package com.hashchat.messenger.auth.dto;

public record AccountExistsResponse(boolean exists, String phone) {
}
