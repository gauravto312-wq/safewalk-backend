package com.safewalk.controller;

import com.safewalk.dto.response.ApiResponse;
import com.safewalk.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/redis")
public class RedisHealthController {

    private final RedisService redisService;

    @Autowired
    public RedisHealthController(RedisService redisService) {
        this.redisService = redisService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkRedisHealth() {
        String testKey = "health:test:" + UUID.randomUUID().toString().substring(0, 8);
        String testValue = "SafeWalk Redis Active Verification";

        // 1. Write with 60s TTL
        redisService.saveWithExpiration(testKey, testValue, 60, TimeUnit.SECONDS);

        // 2. Read
        Object retrievedValue = redisService.get(testKey);

        // 3. Check TTL
        Long ttlSeconds = redisService.getExpire(testKey, TimeUnit.SECONDS);

        // 4. Check existence
        boolean existsBeforeDelete = redisService.hasKey(testKey);

        // 5. Delete
        boolean deleted = redisService.delete(testKey);

        // 6. Check existence after deletion
        boolean existsAfterDelete = redisService.hasKey(testKey);

        Map<String, Object> redisDetails = new HashMap<>();
        redisDetails.put("status", "UP");
        redisDetails.put("service", "Redis Connection & Utility Engine");
        redisDetails.put("writeReadSuccess", testValue.equals(retrievedValue));
        redisDetails.put("retrievedValue", retrievedValue);
        redisDetails.put("ttlVerifiedSeconds", ttlSeconds);
        redisDetails.put("existsBeforeDelete", existsBeforeDelete);
        redisDetails.put("deletedSuccessfully", deleted);
        redisDetails.put("existsAfterDelete", existsAfterDelete);

        return ResponseEntity.ok(ApiResponse.success("Redis verification completed successfully", redisDetails));
    }
}
