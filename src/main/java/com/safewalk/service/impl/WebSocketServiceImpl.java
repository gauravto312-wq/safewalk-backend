package com.safewalk.service.impl;

import com.safewalk.dto.event.RealTimeEvent;
import com.safewalk.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServiceImpl.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketServiceImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcastEvent(String destination, RealTimeEvent event) {
        if (event == null || destination == null) {
            log.warn("Cannot broadcast null event or null destination");
            return;
        }
        log.info("Broadcasting real-time event [type={}, eventId={}] to destination '{}'",
                event.getEventType(), event.getEventId(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    @Override
    public void sendToUser(String username, String destination, RealTimeEvent event) {
        if (username == null || destination == null || event == null) {
            log.warn("Cannot send real-time event to null user, destination, or event");
            return;
        }
        log.info("Sending user-targeted real-time event [type={}, eventId={}] to user '{}' at destination '{}'",
                event.getEventType(), event.getEventId(), username, destination);
        messagingTemplate.convertAndSendToUser(username, destination, event);
    }
}
