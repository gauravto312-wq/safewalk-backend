package com.safewalk.service;

import com.safewalk.dto.event.RealTimeEvent;

public interface WebSocketService {

    void broadcastEvent(String destination, RealTimeEvent event);

    void sendToUser(String username, String destination, RealTimeEvent event);
}
