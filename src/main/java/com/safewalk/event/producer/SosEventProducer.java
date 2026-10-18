package com.safewalk.event.producer;

import com.safewalk.event.SosCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SosEventProducer {

    private static final Logger log = LoggerFactory.getLogger(SosEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String sosEventsTopic;

    @Autowired
    public SosEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${safewalk.kafka.topics.sos-events:sos.events}") String sosEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.sosEventsTopic = sosEventsTopic;
    }

    public void publishSosCreatedEvent(SosCreatedEvent event) {
        if (event == null || event.getEmergencyId() == null) {
            log.warn("Cannot publish null event or event without emergencyId");
            return;
        }

        String key = event.getEmergencyId().toString();
        log.info("Publishing SOS_CREATED event [eventId={}, emergencyId={}] to Kafka topic '{}'",
                event.getEventId(), event.getEmergencyId(), sosEventsTopic);

        try {
            kafkaTemplate.send(sosEventsTopic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka error publishing SOS_CREATED event [eventId={}]: {}",
                            event.getEventId(), ex.getMessage());
                } else {
                    log.info("Successfully published SOS_CREATED event [eventId={}] to topic '{}' (partition={}, offset={})",
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Unexpected error invoking KafkaTemplate.send for eventId {}: {}",
                    event.getEventId(), e.getMessage());
        }
    }
}
