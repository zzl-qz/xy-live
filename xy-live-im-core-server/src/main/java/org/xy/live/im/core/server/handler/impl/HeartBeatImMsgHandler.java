package org.xy.live.im.core.server.handler.impl;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.xy.live.framework.redis.starter.key.ImCoreServerProviderCacheKeyBuilder;
import org.xy.live.im.constants.ImConstants;
import org.xy.live.im.constants.ImMsgCodeEnum;
import org.xy.live.im.core.server.common.ImContextUtils;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.handler.SimpleHandler;
import org.xy.live.im.core.server.interfaces.constants.ImCoreServerConstants;
import org.xy.live.im.dto.ImMsgBody;

import java.util.concurrent.TimeUnit;

/**
 * 心跳消息处理器
 */
@Component
public class HeartBeatImMsgHandler implements SimpleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatImMsgHandler.class);

    @Resource
    private ImCoreServerProviderCacheKeyBuilder imCoreServerProviderCacheKeyBuilder;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void handler(ChannelHandlerContext ctx, ImMsg imMsg) {
        // 基本校验
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appId = ImContextUtils.getAppId(ctx);
        if (userId == null || appId == null) {
            ctx.close();
            LOGGER.error(" ctx error, imMsg {}", imMsg);
            throw new RuntimeException("token error");
        }
        // 存储心跳包记录到redis中
        String redisKey = imCoreServerProviderCacheKeyBuilder.buildImLoginTokenKey(userId, appId);
        this.recordOnlineTime(userId, redisKey); // 将用户心跳包记录保存到redis中
        this.removeExpireRecord(redisKey); // 用计算出来的这个key中的过期心跳包记录删除掉
        redisTemplate.expire(redisKey, 2L, TimeUnit.HOURS); // 为每一个key设置过期时间（每次都会刷新时间，只有当这个key的所有用户都断开连接才有可能过期）
        // 将以供Router服务取出进行转发的im服务器的ip+端口地址进行延时
        // todo
        redisTemplate.expire(ImCoreServerConstants.IM_BIND_IP_KEY + appId + ":" + userId, 2, TimeUnit.HOURS);
        // 回写给客户端
        ImMsgBody respBody = new ImMsgBody();
        respBody.setUserId(userId);
        respBody.setAppId(appId);
        respBody.setData("true");
        LOGGER.info("[HeartBeatImMsgHandler] heartbeat msg, userId is {}, appId is {}", userId, appId);
        ctx.writeAndFlush(ImMsg.build(ImMsgCodeEnum.IM_HEARTBEAT_MSG.getCode(), JSON.toJSONString(respBody)));
    }

    /**
     * 清理掉过期不在线的用户留下的心跳记录（两次心跳时间更友好）
     * 为什么不直接设置TTL让他自动过期？
     * 因为我们build redisKey的时候，是对userId%10000进行构建的，一个用户心跳记录只是zset中的一个键值对，而不是整个zset对象
     * （说人话就是一个key对应多个用户）
     */
    private void removeExpireRecord(String redisKey) {
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, System.currentTimeMillis() - 2 * ImConstants.DEFAULT_HEART_BEAT_GAP * 1000);
    }

    /**
     * 记录用户最近一次心跳时间到Redis上
     */
    private void recordOnlineTime(Long userId, String redisKey) {
        redisTemplate.opsForZSet().add(redisKey, userId, System.currentTimeMillis());
    }

}