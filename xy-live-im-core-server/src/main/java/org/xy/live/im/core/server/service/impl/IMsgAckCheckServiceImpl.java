package org.xy.live.im.core.server.service.impl;

import com.alibaba.fastjson2.JSON;
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
import org.springframework.stereotype.Service;
import org.xy.live.framework.redis.starter.key.ImCoreServerProviderCacheKeyBuilder;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.handler.impl.AckImMsgHandler;
import org.xy.live.im.core.server.service.IMsgAckCheckService;
import org.xy.live.im.dto.ImMsgBody;

import java.util.concurrent.TimeUnit;

import static org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames.XY_LIVE_IM_ACK_MSG_TOPIC;

@Service
public class IMsgAckCheckServiceImpl implements IMsgAckCheckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IMsgAckCheckServiceImpl.class);
    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ImCoreServerProviderCacheKeyBuilder cacheKeyBuilder;


    @Resource
    private MQProducer mqProducer;


    @Override
    public void doMsgAck(ImMsgBody imMsgBody) {
        redisTemplate.opsForHash().delete(cacheKeyBuilder
                .buildImAckMapKey(imMsgBody.getUserId(), imMsgBody.getAppId()), imMsgBody.getMsgId());
    }

    @Override
    public void recordMsgAck(ImMsgBody imMsgBody, int times) {
        LOGGER.info("[recordMsgAck] 传递过来的 imMsgBody is {}", imMsgBody);
        String cacheKey = cacheKeyBuilder.buildImAckMapKey(imMsgBody.getUserId(), imMsgBody.getAppId());
        LOGGER.info("[recordMsgAck] cacheKey is {}", cacheKey);
        redisTemplate.opsForHash().put(cacheKey, imMsgBody.getMsgId(), times);
        redisTemplate.expire(cacheKey, 30, TimeUnit.SECONDS);
    }

    @Override
    public void sendDelayMsg(ImMsgBody imMsgBody) {
        String msgStr = JSON.toJSONString(imMsgBody);
        Message message = new Message();
        message.setTopic(XY_LIVE_IM_ACK_MSG_TOPIC);
        message.setBody(msgStr.getBytes());
        // 设置等级为2，也就是大概5秒
        message.setDelayTimeLevel(2);
        try {
            SendResult result = mqProducer.send(message);
            LOGGER.info("result = {}", result);
        } catch (Exception e) {
            LOGGER.error("ack 校验失败");
        }
    }

    @Override
    public int getMsgAckTimes(String msgId, Long userId, int appId) {
        Object value = redisTemplate.opsForHash().get(cacheKeyBuilder.buildImAckMapKey(userId, appId), msgId);
        return value == null ? -1 : (int) value;
    }
}