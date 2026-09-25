package org.xy.live.user.provider.config;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import org.xy.live.user.constants.CacheAsyncDeleteCode;
import org.xy.live.user.dto.UserCacheAsyncDeleteDT0;

import java.util.List;

import static org.xy.live.user.constants.UserProviderTopicNames.CACHE_ASYNC_DELETE_TOPIC;

/**
 * RocketMQ的消费者bean配置类（适配RocketMQ 4.9.x，无方法找不到问题）
 */
@Configuration
public class RocketMQConsumerConfig implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQConsumerConfig.class);

    @Resource
    private RocketMQConsumerProperties consumerProperties;
    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserProviderCacheKeyBuilder userProviderCacheKeyBuilder;


    @Override
    public void afterPropertiesSet() throws Exception {
        initConsumer();
    }

    public void initConsumer() {
        try {
            AclClientRPCHook rpcHook = new AclClientRPCHook(
                    new SessionCredentials(consumerProperties.getAccessKey(), consumerProperties.getSecretKey()));


            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                    consumerProperties.getGroupName()  + "_" + RocketMQConsumerConfig.class.getSimpleName(),
                    rpcHook,
                    new AllocateMessageQueueAveragely()
            );

            // 3. 基础配置（所有配置都集中在同一个consumer实例）
            // consumer.setVipChannelEnabled(false);
            consumer.setNamesrvAddr(consumerProperties.getNameSrv()); // NameServer地址
            consumer.setConsumeMessageBatchMaxSize(1); // 批量消费最大条数
            consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
            consumer.subscribe(CACHE_ASYNC_DELETE_TOPIC, "*"); // 订阅缓存删除主题

            // 4. 消息监听逻辑（处理Redis缓存删除）
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {


                    // LOGGER.info("[test MSG], {}", msgs.get(0).toString());
                    // return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

                    // 防御性编程：避免空指针
                    if (msgs == null || msgs.isEmpty()) {
                        LOGGER.warn("接收到空的消息列表，直接返回消费成功");
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    }

                    try {
                        // 解析消息体
                        MessageExt msg = msgs.get(0);
                        String json = new String(msg.getBody());

                        UserCacheAsyncDeleteDT0 deleteDTO = null;
                        try {
                            deleteDTO = JSON.parseObject(json, UserCacheAsyncDeleteDT0.class);
                        } catch (Exception e) {
                            LOGGER.error("消息解析失败，疑似捣乱信息，直接忽略，msgId is {}, msg is {}", msg.getMsgId(), json);
                            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                        }

                        // 根据删除类型清理对应Redis缓存
                        if (deleteDTO.getCode() == CacheAsyncDeleteCode.USER_INFO_DELETE.getCode()) {
                            Long userId = JSON.parseObject(deleteDTO.getJson()).getLong("userId");
                            String cacheKey = userProviderCacheKeyBuilder.buildUserInfoKey(userId);
                            redisTemplate.delete(cacheKey);
                            LOGGER.info("成功删除用户信息缓存，userId: {}, 缓存key: {}", userId, cacheKey);
                        } else if (deleteDTO.getCode() == CacheAsyncDeleteCode.USER_TAG_DELETE.getCode()) {
                            Long userId = JSON.parseObject(deleteDTO.getJson()).getLong("userId");
                            String cacheKey = userProviderCacheKeyBuilder.buildUserTagKey(userId);
                            redisTemplate.delete(cacheKey);
                            LOGGER.info("成功删除用户标签缓存，userId: {}, 缓存key: {}", userId, cacheKey);
                        }

                        // 消费成功
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    } catch (Exception e) {
                        LOGGER.error("处理缓存删除消息失败，消息体：{}", new String(msgs.get(0).getBody()), e);
                        // 消费失败，返回重试（可根据业务调整为直接成功）
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
            });

            // 5. 启动消费者
            consumer.start();
            LOGGER.info("RocketMQ消费者启动成功！nameSrv: {}, 消费组: {}, 订阅主题: {}",
                    consumerProperties.getNameSrv(),
                    consumerProperties.getGroupName(),
                    CACHE_ASYNC_DELETE_TOPIC);

        } catch (MQClientException e) {
            LOGGER.error("RocketMQ消费者初始化失败", e);
            throw new RuntimeException("RocketMQ消费者启动失败", e);
        }
    }
}
