package com.hanhome.youtube_comments.batch;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;

@Component
@StepScope
public class ChannelIdItemReader implements ItemReader<String> {
    @Value("${data.keys.update-profile-job}")
    private String HASH_KEY;

    private final RedisTemplate<String, String> redisTemplate;
    private Cursor<Map.Entry<String, String>> cursor;
    private Iterator<Map.Entry<String, String>> iterator;

    public ChannelIdItemReader(
            @Qualifier("googleAccessTokenStringRedisTemplate") RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @org.springframework.batch.core.annotation.BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        if (cursor == null || cursor.isClosed()) {
            HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
            cursor = hashOperations.scan(HASH_KEY, ScanOptions.scanOptions().match("*").count(1).build());
            iterator = cursor.stream().iterator();
        }
    }

    @Override
    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException{
        if (iterator != null && iterator.hasNext()) {
            return iterator.next().getValue();
        } else {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            return null;
        }
    }

    @org.springframework.batch.core.annotation.AfterStep
    public void afterStep(StepExecution stepExecution) {
        if (cursor != null && !cursor.isClosed()) {
            HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
            cursor = hashOperations.scan(HASH_KEY, ScanOptions.scanOptions().match("*").count(1).build());
            iterator = cursor.stream().iterator();
        }
    }
}
