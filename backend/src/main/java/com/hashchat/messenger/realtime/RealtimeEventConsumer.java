package com.hashchat.messenger.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.hashchat.messenger.outbox.DomainEvent;
import com.hashchat.messenger.outbox.KafkaConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeEventConsumer {

    private final DomainEventProcessor domainEventProcessor;

    @KafkaListener(
            topics = {
                    KafkaConfig.MESSAGE_EVENTS_TOPIC,
                    KafkaConfig.CHAT_EVENTS_TOPIC,
                    KafkaConfig.RECEIPT_EVENTS_TOPIC,
                    KafkaConfig.USER_EVENTS_TOPIC,
                    KafkaConfig.SECURITY_EVENTS_TOPIC
            },
            groupId = "${hashchat.kafka.realtime.group-id:hashchat-realtime}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDomainEvent(DomainEvent event) {
        domainEventProcessor.process(event);
    }
}
