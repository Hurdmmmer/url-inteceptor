package com.tdh.urlInterceptor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tdh.urlInterceptor.configuration.Rule;

import java.io.IOException;

public interface RedisService {
    Long isLimit(String key, long expireTime, int limitTimes);

    Rule get(String key) throws IOException;

    void add(String key, Rule value) throws JsonProcessingException;
}
