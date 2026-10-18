package com.safewalk.service;

import java.util.concurrent.TimeUnit;

public interface RedisService {

    void save(String key, Object value);

    void saveWithExpiration(String key, Object value, long timeout, TimeUnit unit);

    Object get(String key);

    <T> T get(String key, Class<T> clazz);

    boolean delete(String key);

    boolean hasKey(String key);

    Long getExpire(String key, TimeUnit unit);
}
