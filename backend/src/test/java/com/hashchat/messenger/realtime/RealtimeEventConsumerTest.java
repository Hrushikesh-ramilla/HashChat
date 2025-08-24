package com.hashchat.messenger.realtime;

import org.junit.jupiter.api.Test;
import com.hashchat.messenger.outbox.DomainEvent;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeEventConsumerTest {

    @Test
    void delegatesToDomainEventProcessor() {
        DomainEventProcessor processor = mock(DomainEventProcessor.class);
        RealtimeEventConsumer consumer = new RealtimeEventConsumer(processor);
        DomainEvent event = new DomainEvent(
                "evt-1",
                "message",
                "10",
                "MESSAGE_CREATED",
                1,
                1,
                "{}",
                Instant.parse("2025-07-12T00:00:00Z"),
                "corr-1",
                "idem-1"
        );

        consumer.handleDomainEvent(event);

        verify(processor).process(event);
    }
}
