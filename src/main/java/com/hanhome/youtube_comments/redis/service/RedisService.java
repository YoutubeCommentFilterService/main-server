package com.hanhome.youtube_comments.redis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

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

    public <T> T get(String key, Class<T> clazz) {
        Object obj = redisTemplate.opsForValue().get(key);
        return objectMapper.convertValue(obj, clazz);
    }

    public void remove(String key) {
        redisTemplate.delete(key);
    }

    public void searchNRemove(String pattern, boolean isPrefix) {
        pattern = isPrefix ? pattern + "*" : "*" + pattern;
        ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).count(100).build();
        Cursor<byte[]> cursor =  redisTemplate.getConnectionFactory().getConnection().scan(scanOptions);

        while(cursor.hasNext()) {
            String key = new String(cursor.next(), StandardCharsets.UTF_8);
            remove(key);
        }
    }
}
