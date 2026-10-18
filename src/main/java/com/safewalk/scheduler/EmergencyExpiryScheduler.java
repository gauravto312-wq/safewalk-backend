package com.safewalk.scheduler;

import com.safewalk.service.EmergencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmergencyExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmergencyExpiryScheduler.class);

    private final EmergencyService emergencyService;

    public EmergencyExpiryScheduler(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    /**
     * Startup reconciliation: Runs when the Spring application is ready.
     * Evaluates all active/accepted emergencies and expires any created > 5 hours ago.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartupReconciliation() {
        log.info("SafeWalk startup: Performing emergency expiry reconciliation for overdue active emergencies...");
        try {
            int expiredCount = emergencyService.expireOverdueEmergencies();
            log.info("SafeWalk startup emergency expiry reconciliation completed. Expired {} overdue emergencies.", expiredCount);
        } catch (Exception e) {
            log.error("SafeWalk startup emergency expiry reconciliation encountered an error: {}", e.getMessage(), e);
        }
    }

    /**
     * Periodic scan: Runs every 60 seconds (with initial delay 60 seconds).
     * Scans for and automatically expires any emergencies reaching their 5-hour lifetime.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void scheduleEmergencyExpiryCheck() {
        try {
            int expiredCount = emergencyService.expireOverdueEmergencies();
            if (expiredCount > 0) {
                log.info("Periodic emergency expiry check expired {} overdue emergencies.", expiredCount);
            }
        } catch (Exception e) {
            log.error("Periodic emergency expiry check encountered an error: {}", e.getMessage(), e);
        }
    }
}
