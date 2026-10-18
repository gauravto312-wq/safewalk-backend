package com.safewalk.scheduler;

import com.safewalk.service.EmergencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Continuous Responder Discovery Scheduler
 * Periodically scans for active emergencies created within the last 5 hours,
 * detects newly eligible nearby responders (e.g., users who entered the 2 km radius
 * or refreshed their location coordinates), and safely dispatches FCM notifications
 * with persistent deduplication.
 */
@Component
public class EmergencyDiscoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmergencyDiscoveryScheduler.class);

    private final EmergencyService emergencyService;

    public EmergencyDiscoveryScheduler(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    /**
     * Periodic discovery scan:
     * Defaults to running every 15 seconds (configurable via safewalk.discovery.interval-ms).
     * Uses fixedDelay so the next execution begins only after the previous run finishes.
     */
    @Scheduled(fixedDelayString = "${safewalk.discovery.interval-ms:15000}", initialDelay = 15000)
    public void scheduleContinuousDiscovery() {
        try {
            int dispatchedCount = emergencyService.discoverAndDispatchNewResponders();
            if (dispatchedCount > 0) {
                log.info("Continuous responder discovery cycle completed: dispatched alerts to {} new responder(s).", dispatchedCount);
            }
        } catch (Exception e) {
            log.error("Continuous responder discovery cycle encountered an error: {}", e.getMessage(), e);
        }
    }
}
