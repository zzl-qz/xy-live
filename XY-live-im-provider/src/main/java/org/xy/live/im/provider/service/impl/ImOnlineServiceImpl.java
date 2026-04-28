package org.xy.live.im.provider.service.impl;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xy.live.im.core.server.interfaces.constants.ImCoreServerConstants;
import org.xy.live.im.provider.service.ImOnlineService;

@Service
public class ImOnlineServiceImpl implements ImOnlineService {

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 判断用户是否在线
     *
     * @param userId
     * @param appId
     * @return
     */
    @Override
    public boolean isOnline(long userId, int appId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ImCoreServerConstants.IM_BIND_IP_KEY + appId + ":" + userId));
    }
}
