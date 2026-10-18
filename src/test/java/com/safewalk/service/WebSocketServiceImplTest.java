package com.safewalk.service;

import com.safewalk.dto.event.RealTimeEvent;
import com.safewalk.service.impl.WebSocketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketServiceImpl webSocketService;

    @BeforeEach
    void setUp() {
        webSocketService = new WebSocketServiceImpl(messagingTemplate);
    }

    @Test
    void broadcastEvent_Success() {
        RealTimeEvent event = RealTimeEvent.builder()
                .eventType("TEST")
                .message("Test message")
                .build();

        webSocketService.broadcastEvent("/topic/test", event);

        verify(messagingTemplate).convertAndSend(eq("/topic/test"), eq(event));
    }

    @Test
    void broadcastEvent_NullEvent_DoesNotSend() {
        webSocketService.broadcastEvent("/topic/test", null);
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void sendToUser_Success() {
        RealTimeEvent event = RealTimeEvent.builder()
                .eventType("TEST")
                .message("Private user event")
                .build();

        webSocketService.sendToUser("+919876543210", "/queue/messages", event);

        verify(messagingTemplate).convertAndSendToUser(eq("+919876543210"), eq("/queue/messages"), eq(event));
    }

    @Test
    void sendToUser_NullUser_DoesNotSend() {
        RealTimeEvent event = RealTimeEvent.builder().build();
        webSocketService.sendToUser(null, "/queue/messages", event);

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }
}
