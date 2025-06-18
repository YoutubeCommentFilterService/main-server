package com.hanhome.youtube_comments.oauth.dto;

import java.util.concurrent.TimeUnit;

public record CustomTokenRecord(String token, TimeUnit timeUnit, long ttl) {}
