package com.tdh.urlInterceptor.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdh.urlInterceptor.configuration.Rule;
import com.tdh.urlInterceptor.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RedisServiceImpl implements RedisService {
    @Autowired
    private RedisTemplate redisTemplate;
    private DefaultRedisScript<Long> getRedisScript;

    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        getRedisScript = new DefaultRedisScript<Long>();
        getRedisScript.setResultType(Long.class);
        getRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("rateLimter.lua")));
    }

    /**
     *  调用 lua 脚本 是否限流
     * @param key       key
     * @param expireTime 限流时间 单位秒
     * @param limitTimes 限制次数
     * @return
     */
    @SuppressWarnings("unchecked")
    public Long isLimit(String key, long expireTime, int limitTimes) {
        /**
         * 执行Lua脚本
         */
        List<String> keyList = new ArrayList();
        // 设置key值为注解中的值
        keyList.add(key);
        /**
         * 调用脚本并执行
         */
        return (Long) redisTemplate.execute(getRedisScript, keyList, limitTimes, expireTime);

//        Boolean result = this.redisTemplate.hasKey(key);
//        if (result) {
//            this.redisTemplate.opsForValue().increment(key, 1);
//            this.redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
//        }
//        return result ? 0L : -1L;
    }

    public Rule get(String key) throws IOException {
        Object obj = this.redisTemplate.opsForValue().get(key);
        if (obj == null) return null;
        return objectMapper.readValue(obj.toString(), Rule.class);
    }
    @SuppressWarnings("unchecked")
    public void add(String key, Rule object) throws JsonProcessingException {
        ValueOperations valueOperations = this.redisTemplate.opsForValue();
        String json = objectMapper.writeValueAsString(object);
        valueOperations.set(key, json);
        this.redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }
}
