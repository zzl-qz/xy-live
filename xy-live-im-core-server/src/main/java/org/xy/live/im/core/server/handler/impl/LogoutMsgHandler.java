package org.xy.live.im.core.server.handler.impl;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames;
import org.xy.live.framework.redis.starter.key.ImCoreServerProviderCacheKeyBuilder;
import org.xy.live.im.constants.ImConstants;
import org.xy.live.im.constants.ImMsgCodeEnum;
import org.xy.live.im.core.server.common.ChannelHandlerContextCache;
import org.xy.live.im.core.server.common.ImContextUtils;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.handler.SimpleHandler;
import org.xy.live.im.core.server.interfaces.constants.ImCoreServerConstants;
import org.xy.live.im.core.server.interfaces.dto.ImOfflineDTO;
import org.xy.live.im.dto.ImMsgBody;

import java.util.concurrent.TimeUnit;

import static org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames.IM_OFFLINE_TOPIC;

/**
 * 登出消息处理器
 */
@Component
public class LogoutMsgHandler implements SimpleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutMsgHandler.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ImCoreServerProviderCacheKeyBuilder cacheKeyBuilder;

    @Resource
    private MQProducer mqProducer;

    @Override
    public void handler(ChannelHandlerContext ctx, ImMsg imMsg) {
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appId = ImContextUtils.getAppId(ctx);
        if (userId == null || appId == null) {
            LOGGER.error(" attr error, imMsg {}", imMsg);
            ctx.close();
            throw new RuntimeException("attr error");
        }
        logoutHandler(ctx, userId, appId);
    }


    /**
     * 删除一些缓存信息
     *
     * @param userId
     * @param appId
     */
    public void handlerLogout(Long userId, Integer appId) {
        // 理想情况下：客户端断线的时候发送短线消息包
        ChannelHandlerContextCache.remove(userId);
        // 删除供Router取出的存在Redis的IM服务器的ip+端口地址
        stringRedisTemplate.delete(ImCoreServerConstants.IM_BIND_IP_KEY + appId + ":" + userId);
        // 删除盖用户心跳包存活缓存
        stringRedisTemplate.opsForZSet().remove(cacheKeyBuilder.buildImLoginTokenKey(userId, appId), userId);
    }


    // logoutHandler()方法removeUserId前加上 sendLogoutMQ(ctx, userId, appId);
    // logoutHandler()方法加上 ImContextUtils.removeRoomId(ctx);

    /**
     * 将IM消息回写给客户端并进行一些属性的移除
     *
     * @param ctx
     * @param userId
     * @param appId
     */
    public void logoutHandler(ChannelHandlerContext ctx, Long userId, Integer appId) {
        //将IM消息回写给客户端
        ImMsgBody respBody = new ImMsgBody();
        respBody.setUserId(userId);
        respBody.setAppId(appId);
        respBody.setData("true");
        ctx.writeAndFlush(ImMsg.build(ImMsgCodeEnum.IM_LOGOUT_MSG.getCode(), JSON.toJSONString(respBody)));
        LOGGER.info("[LogoutMsgHandler] logout success, userId is {}, appId is {}", userId, appId);
        //理想情况下：客户端断线的时候发送断线消息包
        handlerLogout(userId, appId);
        // 异步解绑roomId与userId的关系
        sendLogoutMQ(ctx, userId, appId);
        ImContextUtils.removeUserId(ctx);
        ImContextUtils.removeAppId(ctx);
        ctx.close();
    }


    /**
     * ws协议登出时，发送消息取消userId与roomId的关联
     */
    private void sendLogoutMQ(ChannelHandlerContext ctx, Long userId, Integer appId) {
        ImOfflineDTO imOfflineDTO = new ImOfflineDTO();
        imOfflineDTO.setUserId(userId);
        imOfflineDTO.setAppId(appId);
        imOfflineDTO.setRoomId(ImContextUtils.getRoomId(ctx));
        imOfflineDTO.setLogoutTime(System.currentTimeMillis());
        Message message = new Message();
        message.setTopic(IM_OFFLINE_TOPIC);
        message.setBody(JSON.toJSONString(imOfflineDTO).getBytes());
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("[sendLogoutMQ] send result is {}", sendResult);
        } catch (Exception e) {
            LOGGER.error("[sendLogoutMQ] send loginMQ error, error is ", e);
        }
    }

}