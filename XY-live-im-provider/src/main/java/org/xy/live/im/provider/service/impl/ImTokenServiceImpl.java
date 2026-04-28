package org.xy.live.im.provider.service.impl;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xy.live.framework.redis.starter.key.ImProviderCacheKeyBuilder;
import org.xy.live.im.provider.service.ImTokenService;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ImTokenServiceImpl implements ImTokenService {

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ImProviderCacheKeyBuilder imProviderCacheKeyBuilder;


    @Override
    public String createImLoginToken(Long userId, int appId) {
        String token = UUID.randomUUID() + "%" + appId;
        redisTemplate.opsForValue().set(imProviderCacheKeyBuilder.buildImLoginTokenKey(token), userId, 5L, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public Long getUserIdByToken(String token) {
        Object userId = redisTemplate.opsForValue().get(imProviderCacheKeyBuilder.buildImLoginTokenKey(token));
        return userId == null ? null : Long.valueOf(userId.toString());
    }
}
