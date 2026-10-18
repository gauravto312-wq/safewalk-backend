package com.safewalk.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safewalk.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String key, Object value) {
        log.debug("Saving key '{}' to Redis without expiration", key);
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void saveWithExpiration(String key, Object value, long timeout, TimeUnit unit) {
        log.debug("Saving key '{}' to Redis with TTL: {} {}", key, timeout, unit);
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public Object get(String key) {
        log.debug("Fetching value for key '{}' from Redis", key);
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        log.debug("Fetching value for key '{}' as type {}", key, clazz.getSimpleName());
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, clazz);
    }

    @Override
    public boolean delete(String key) {
        log.debug("Deleting key '{}' from Redis", key);
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean hasKey(String key) {
        log.debug("Checking existence of key '{}' in Redis", key);
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public Long getExpire(String key, TimeUnit unit) {
        log.debug("Checking TTL for key '{}' in Redis", key);
        return redisTemplate.getExpire(key, unit);
    }
}
