package org.xy.live.msg.provider.consumer.handler.impl;

import com.alibaba.fastjson.JSON;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.xy.live.framework.redis.starter.key.MsgProviderCacheKeyBuilder;
import org.xy.live.im.constants.AppIdEnum;
import org.xy.live.im.dto.ImMsgBody;
import org.xy.live.im.router.constants.ImMsgBizCodeEnum;
import org.xy.live.im.router.interfaces.ImRouterRpc;
import org.xy.live.living.interfaces.dto.LivingRoomReqDTO;
import org.xy.live.living.interfaces.rpc.ILivingRoomRpc;
import org.xy.live.msg.dto.MessageDTO;
import org.xy.live.msg.provider.consumer.handler.MessageHandler;
import org.xy.live.msg.provider.dao.mapper.DanmakuMapper;
import org.xy.live.msg.provider.dao.po.DanmakuPO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.xy.live.common.interfaces.topic.MsgProviderTopicNames.XY_LIVE_DANMU_MSG_PER_TOPIC;

/**
 *
 */
@Component
public class SingleMessageHandlerImpl implements MessageHandler {


    private final Logger LOGGER = LoggerFactory.getLogger(SingleMessageHandlerImpl.class);

     @DubboReference
    private ImRouterRpc routerRpc;
     @DubboReference
    private ILivingRoomRpc livingRoomRpc;

    @Resource
    private MQProducer mqProducer;

    @Resource
    private DanmakuMapper danmakuMapper;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private MsgProviderCacheKeyBuilder cacheKeyBuilder;

    /**
     * 处理im服务投递过来的消息内容
     * @param imMsgBody
     */
    @Override
    public void onMsgReceive(ImMsgBody imMsgBody) {
        // 获取业务码
        int bizCode = imMsgBody.getBizCode();

        // 直播间的聊天消息，一个人发送，n个人接收
        if (bizCode == ImMsgBizCodeEnum.LIVING_ROOM_IM_CHAT_MSG_BIZ.getCode()) {
            // 将消息解析出来
            MessageDTO messageDTO = JSON.parseObject(imMsgBody.getData(), MessageDTO.class);
            Integer roomId = messageDTO.getRoomId();
            LivingRoomReqDTO reqDTO = new LivingRoomReqDTO();
            reqDTO.setRoomId(roomId);
            reqDTO.setAppId(imMsgBody.getAppId());

            //根据roomId去调用rpc方法查询直播间在线userId(自己不用发，所以只筛选出和自己的id不相等的)
            List<Long> userIdList = livingRoomRpc.queryUserIdsByRoomId(reqDTO).stream().filter(x -> !x.equals(imMsgBody.getUserId())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(userIdList)) {
                LOGGER.warn("[SingleMessageHandlerImpl] 要转发的userIdList为空");
                return;
            }

            LOGGER.info("[SingleMessageHandlerImpl] 要转发的userIdList is {}", userIdList);

            // 因为有多个人，所以需要创建出多个ImMsgBody
            List<ImMsgBody> respMsgBodies = new ArrayList<>();
            userIdList.forEach(userId -> {
                ImMsgBody respMsgBody = new ImMsgBody();
                respMsgBody.setMsgId(imMsgBody.getMsgId());
                respMsgBody.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
                respMsgBody.setBizCode(ImMsgBizCodeEnum.LIVING_ROOM_IM_CHAT_MSG_BIZ.getCode());
                respMsgBody.setData(JSON.toJSONString(messageDTO));
                //设置发送目标对象的id
                respMsgBody.setUserId(userId);
                respMsgBodies.add(respMsgBody);
            });

            //将消息推送给router进行转发给im服务器
            routerRpc.batchSendMsg(respMsgBodies);
        }
    }


    @Override
    public void sendMsgPersistence(ImMsgBody imMsgBody) {
        // setnx一下避免重复持久化
        String msgId = imMsgBody.getMsgId();
        String cacheKey = cacheKeyBuilder.buildMsgUnionMidKey(msgId);
        LOGGER.info("[sendMsgPersistence] 拿到的msgId 是 {}", msgId);
        if (msgId == null || msgId.isBlank()) {
            LOGGER.warn("[sendMsgPersistence]，消息msgId为null，直接跳过");
            return;
        }
        Boolean status = redisTemplate.opsForValue().setIfAbsent(cacheKey, -1, 1, TimeUnit.DAYS);
        if (!status) {
            LOGGER.warn("[sendMsgPersistence]，消息：{} 已经持久化，直接跳过", msgId);
            return;
        }

        Message message = new Message();
        message.setTopic(XY_LIVE_DANMU_MSG_PER_TOPIC);
        message.setBody(JSON.toJSONString(imMsgBody).getBytes());
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("[sendMsgPersistence] success, {}", sendResult);
        } catch (Exception e) {
            LOGGER.error("[sendMsgPersistence] error is {}", e.getMessage());
        }
    }


    /**
     * 消息持久化
     * @param imMsgBody
     */
    @Override
    @Transactional
    public boolean msgPersistence(ImMsgBody imMsgBody) {
        try {
            MessageDTO messageDTO = JSON.parseObject(imMsgBody.getData(), MessageDTO.class);
            DanmakuPO danmakuPO = new DanmakuPO();
            danmakuPO.setRoomId(Long.valueOf(messageDTO.getRoomId()));
            danmakuPO.setUserId(imMsgBody.getUserId());
            danmakuPO.setContent(messageDTO.getContent());
            danmakuPO.setSendTime(LocalDateTime.now());
            danmakuMapper.insert(danmakuPO);
            return true;
        } catch (Exception e) {
            LOGGER.error("[msgPersistence] 接口执行失败，失败原因是{}", e.getMessage());
            return false;
        }
    }
}
