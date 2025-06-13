package com.hashchat.messenger.auth.dto;

public record UsernameAvailabilityResponse(String username, boolean valid, boolean available) {}
