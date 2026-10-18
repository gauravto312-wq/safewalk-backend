package com.safewalk.event.producer;

import com.safewalk.event.SosCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SosEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private SosEventProducer sosEventProducer;

    @BeforeEach
    void setUp() {
        sosEventProducer = new SosEventProducer(kafkaTemplate, "sos.events");
    }

    @Test
    void publishSosCreatedEvent_Success() {
        SosCreatedEvent event = SosCreatedEvent.builder()
                .emergencyId(100L)
                .requesterId(1L)
                .requesterMobile("+919876543210")
                .type("GENERAL")
                .status("ACTIVE")
                .latitude(28.6139)
                .longitude(77.2090)
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("sos.events"), eq("100"), any(SosCreatedEvent.class)))
                .thenReturn(future);

        sosEventProducer.publishSosCreatedEvent(event);

        ArgumentCaptor<SosCreatedEvent> eventCaptor = ArgumentCaptor.forClass(SosCreatedEvent.class);
        verify(kafkaTemplate).send(eq("sos.events"), eq("100"), eventCaptor.capture());

        SosCreatedEvent captured = eventCaptor.getValue();
        assertEquals(100L, captured.getEmergencyId());
        assertEquals("100", captured.getEmergencyId().toString());
        assertEquals("+919876543210", captured.getRequesterMobile());
        assertEquals("SOS_CREATED", captured.getEventType());
    }

    @Test
    void publishSosCreatedEvent_NullEvent_DoesNotPublish() {
        sosEventProducer.publishSosCreatedEvent(null);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void publishSosCreatedEvent_NullEmergencyId_DoesNotPublish() {
        SosCreatedEvent event = SosCreatedEvent.builder().build();
        event.setEmergencyId(null);

        sosEventProducer.publishSosCreatedEvent(event);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void publishSosCreatedEvent_KafkaException_HandledGracefullyWithoutCrashing() {
        SosCreatedEvent event = SosCreatedEvent.builder()
                .emergencyId(100L)
                .build();

        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("Kafka broker offline"));

        assertDoesNotThrow(() -> sosEventProducer.publishSosCreatedEvent(event));
    }
}
