package org.xy.live.im.core.server.handler.impl;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames;
import org.xy.live.im.constants.AppIdEnum;
import org.xy.live.im.constants.ImConstants;
import org.xy.live.im.constants.ImMsgCodeEnum;
import org.xy.live.im.core.server.common.ChannelHandlerContextCache;
import org.xy.live.im.core.server.common.ImContextUtils;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.handler.SimpleHandler;
import org.xy.live.im.core.server.interfaces.constants.ImCoreServerConstants;
import org.xy.live.im.core.server.interfaces.dto.ImOnlineDTO;
import org.xy.live.im.dto.ImMsgBody;
import org.xy.live.im.interfaces.ImTokenRpc;

import java.util.concurrent.TimeUnit;

import static org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames.IM_ONLINE_TOPIC;


/**
 * 登录消息处理器
 */
@Component
public class LoginMsgHandler implements SimpleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginMsgHandler.class);

    @DubboReference
    private ImTokenRpc imTokenRpc;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MQProducer mqProducer;


    @Override
    public void handler(ChannelHandlerContext ctx, ImMsg imMsg) {

        // 防止重复请求
        if (ImContextUtils.getUserId(ctx) != null) return;

        // 先取出body
        byte[] body = imMsg.getBody();
        if (body == null || body.length == 0) {
            ctx.close();
            LOGGER.error(" body error, imMsg {}", imMsg);
            throw new RuntimeException("body error");
        }
        ImMsgBody imMsgBody = JSON.parseObject(new String(body), ImMsgBody.class);
        String token = imMsgBody.getToken();
        int appId = imMsgBody.getAppId();
        Long userId = imTokenRpc.getUserIdByToken(token);
        if (!StringUtils.hasText(token) || userId < 10000 || appId < 10000) {
            ctx.close();
            LOGGER.error(" token error, imMsg {}", imMsg);
            throw new RuntimeException("token error");
        }
        if (userId.equals(imMsgBody.getUserId())) {
            // 调用登录成功处理方法
            loginSuccessHandler(ctx, userId, appId, null);
            return;
        }
        ctx.close();
        LOGGER.error(" error, imMsg {}", imMsg);
        throw new RuntimeException("error");
    }


    /**
     * 如果用户成功登录，就处理相关记录
     */
    public void loginSuccessHandler(ChannelHandlerContext ctx, Long userId, Integer appId, Integer roomId) {
        // 按照userId保存好相关的channel信息
        ChannelHandlerContextCache.put(userId, ctx);

        // 将userId/appId/roomId保存到netty域信息中，用于正常/非正常logout的处理
        ImContextUtils.setUserId(ctx, userId);
        ImContextUtils.setAppId(ctx, appId);
        ImContextUtils.setRoomId(ctx, roomId);

        // 将im消息回写给客户端
        ImMsgBody respBody = new ImMsgBody();
        respBody.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
        respBody.setUserId(userId);
        respBody.setData("true");
        ImMsg respMsg = ImMsg.build(ImMsgCodeEnum.IM_LOGIN_MSG.getCode(), JSON.toJSONString(respBody));

        // 将当前用户绑定的im服务器的ip+端口地址保存到Redis，以供Router服务取出进行转发
        stringRedisTemplate.opsForValue().set(ImCoreServerConstants.IM_BIND_IP_KEY + appId + ":" + userId,
                ChannelHandlerContextCache.getServerIpAddress() + "%" + userId,
                2, TimeUnit.HOURS);


        LOGGER.info("[LoginMsgHandler] login success, userId is {}, appId is {}", userId, appId);
        ctx.writeAndFlush(respMsg);
        sendLoginMQ(userId, appId, roomId);
    }


    /**
     * ws协议用户初次登录的时候发送mq消息，将userId与roomId关联起来，便于直播间聊天消息的推送
     */
    private void sendLoginMQ(Long userId, Integer appId, Integer roomId) {
        ImOnlineDTO imOnlineDTO = new ImOnlineDTO();
        imOnlineDTO.setUserId(userId);
        imOnlineDTO.setAppId(appId);
        imOnlineDTO.setRoomId(roomId);
        imOnlineDTO.setLoginTime(System.currentTimeMillis());
        Message message = new Message();
        message.setTopic(IM_ONLINE_TOPIC);
        message.setBody(JSON.toJSONString(imOnlineDTO).getBytes());
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("[sendLoginMQ] send result is {}", sendResult);
        } catch (Exception e) {
            LOGGER.error("[sendLoginMQ] send loginMQ error, error is ", e);
        }
    }

}