package com.hanhome.youtube_comments.oauth.dto;

import java.util.concurrent.TimeUnit;

public record CustomTokenRecord(long ttl, String token, TimeUnit timeUnit) {}
