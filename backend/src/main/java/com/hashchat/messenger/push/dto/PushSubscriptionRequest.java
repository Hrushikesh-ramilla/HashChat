package com.hashchat.messenger.push.dto;

public record PushSubscriptionRequest(
        String endpoint,
        String p256dh,
        String auth
) {}
