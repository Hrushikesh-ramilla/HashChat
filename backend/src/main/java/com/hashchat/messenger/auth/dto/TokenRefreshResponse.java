package com.hashchat.messenger.auth.dto;

public record TokenRefreshResponse(String token, String refreshToken, String deviceRegistrationToken) {}
