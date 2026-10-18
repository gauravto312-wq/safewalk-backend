package com.safewalk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safewalk.service.impl.RedisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ObjectMapper objectMapper;
    private RedisServiceImpl redisService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        redisService = new RedisServiceImpl(redisTemplate, objectMapper);
    }

    @Test
    void save_CallsValueOperationsSet() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisService.save("test:key", "testValue");

        verify(valueOperations).set("test:key", "testValue");
    }

    @Test
    void saveWithExpiration_CallsValueOperationsSetWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisService.saveWithExpiration("test:key", "testValue", 60, TimeUnit.SECONDS);

        verify(valueOperations).set("test:key", "testValue", 60, TimeUnit.SECONDS);
    }

    @Test
    void get_ReturnsValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("test:key")).thenReturn("testValue");

        Object result = redisService.get("test:key");

        assertNotNull(result);
        assertEquals("testValue", result);
    }

    @Test
    void delete_ReturnsTrueOnSuccess() {
        when(redisTemplate.delete("test:key")).thenReturn(true);

        boolean deleted = redisService.delete("test:key");

        assertTrue(deleted);
        verify(redisTemplate).delete("test:key");
    }

    @Test
    void hasKey_ReturnsTrueIfExists() {
        when(redisTemplate.hasKey("test:key")).thenReturn(true);

        boolean exists = redisService.hasKey("test:key");

        assertTrue(exists);
        verify(redisTemplate).hasKey("test:key");
    }

    @Test
    void getExpire_ReturnsTtl() {
        when(redisTemplate.getExpire("test:key", TimeUnit.SECONDS)).thenReturn(45L);

        Long ttl = redisService.getExpire("test:key", TimeUnit.SECONDS);

        assertEquals(45L, ttl);
        verify(redisTemplate).getExpire("test:key", TimeUnit.SECONDS);
    }
}
