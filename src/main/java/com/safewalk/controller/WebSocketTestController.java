package com.safewalk.controller;

import com.safewalk.dto.event.RealTimeEvent;
import com.safewalk.dto.response.ApiResponse;
import com.safewalk.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/realtime")
public class WebSocketTestController {

    private final WebSocketService webSocketService;

    @Autowired
    public WebSocketTestController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<RealTimeEvent>> sendTestEvent(
            @RequestParam(required = false, defaultValue = "Real-time connection working") String message,
            @RequestBody(required = false) Map<String, Object> payload) {

        RealTimeEvent testEvent = RealTimeEvent.builder()
                .eventType("TEST")
                .message(message)
                .data(payload != null ? payload : Map.of("status", "SUCCESS"))
                .build();

        webSocketService.broadcastEvent("/topic/test", testEvent);

        return ResponseEntity.ok(ApiResponse.success("Real-time test event published successfully", testEvent));
    }
}
