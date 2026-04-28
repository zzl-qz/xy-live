package org.xy.live.account.provider.service.impl;

import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xy.live.account.provider.service.AccountTokenService;
import org.xy.live.framework.redis.starter.key.AccountProviderCacheKeyBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AccountTokenServiceImpl implements AccountTokenService {

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private AccountProviderCacheKeyBuilder cacheKeyBuilder;

    @Override
    public String createAndSaveLoginToken(Long userId) {
        // todo 双token
        String token = UUID.randomUUID().toString();
        String key = cacheKeyBuilder.buildAccountTokenKey(token);
        // 塞到redis
        redisTemplate.opsForValue().set(key, userId, 3600, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public Long getUserIdByToken(String token) {
        String key = cacheKeyBuilder.buildAccountTokenKey(token);
        Long userId = Long.valueOf(redisTemplate.opsForValue().get(key).toString());
        System.out.println("userId" + userId);
        return userId;
    }
}
