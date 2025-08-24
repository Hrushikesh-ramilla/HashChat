package com.hashchat.messenger.outbox;

/**
 * Domain services publish through this contract only.
 * {@link OutboxPublisher} sends to Kafka; {@link com.hashchat.messenger.realtime.RealtimeEventConsumer}
 * is the only consumer that turns events into durable device delivery.
 */
public interface EventPublisher {

    void publish(String aggregateType, String aggregateId, String eventType, Object payload);

    void publish(
            String aggregateType,
            String aggregateId,
            String eventType,
            Object payload,
            String correlationId,
            String idempotencyKey
    );
}
