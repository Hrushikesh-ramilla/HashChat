package com.hashchat.messenger.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventTopicResolverTest {

    private final EventTopicResolver resolver = new EventTopicResolver();

    @Test
    void routesMessageAndReceiptEventsToDifferentTopics() {
        assertThat(resolver.topicFor(event("message", "MESSAGE_CREATED")))
                .isEqualTo("hashchat.message.events");
        assertThat(resolver.topicFor(event("message", "MESSAGE_STATUS")))
                .isEqualTo("hashchat.receipt.events");
        assertThat(resolver.topicFor(event("message", "MESSAGE_BULK_STATUS")))
                .isEqualTo("hashchat.receipt.events");
    }

    @Test
    void routesChatAndRequestToTheSameChatTopic() {
        assertThat(resolver.topicFor(event("chat", "CHAT_CREATED")))
                .isEqualTo("hashchat.chat.events");
        assertThat(resolver.topicFor(event("request", "REQUEST_CREATED")))
                .isEqualTo("hashchat.chat.events");
    }

    @Test
    void routesUserAndSecuritySeparately() {
        assertThat(resolver.topicFor(event("user", "PROFILE_UPDATED")))
                .isEqualTo("hashchat.user.events");
        assertThat(resolver.topicFor(event("device", "DEVICE_REVOKED")))
                .isEqualTo("hashchat.security.events");
    }

    private static OutboxEvent event(String aggregate, String type) {
        return OutboxEvent.builder()
                .aggregateType(aggregate)
                .eventType(type)
                .build();
    }
}
