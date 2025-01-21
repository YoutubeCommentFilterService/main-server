package com.hanhome.youtube_comments.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void save(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void save(String key, Object value, long ttlInSec) {
        this.save(key, value, ttlInSec, TimeUnit.SECONDS);
    }

    public void save(String key, Object value, long ttlInSec, TimeUnit timeunit) {
        redisTemplate.opsForValue().set(key, value, ttlInSec, timeunit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void remove(String key) {
        redisTemplate.delete(key);
    }
}
