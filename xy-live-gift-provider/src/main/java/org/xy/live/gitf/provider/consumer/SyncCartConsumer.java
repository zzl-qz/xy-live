package org.xy.live.gitf.provider.consumer;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.xy.live.common.interfaces.constant.AclHookConstant;
import org.xy.live.common.interfaces.topic.GiftProviderTopicNames;
import org.xy.live.common.interfaces.topic.ShopCarProviderTopicNames;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.gitf.provider.bo.CartChangeEventBO;
import org.xy.live.gitf.provider.service.CartItemService;

/**
 * 购物车消息消费
 */
@Configuration
public class SyncCartConsumer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncCartConsumer.class);

    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;

    @Resource
    private CartItemService cartItemService;

    @Override
    public void afterPropertiesSet() throws Exception {

        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(AclHookConstant.ACCESS_KEY, AclHookConstant.SECRET_KEY));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName()  +  "_" + SyncCartConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(10); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(ShopCarProviderTopicNames.SYNC_MSG_TOPIC, "*"); // 订阅缓存删除主题


//        DefaultMQPushConsumer mqPushConsumer = new DefaultMQPushConsumer();
//        //老版本中会开启，新版本的mq不需要使用到
//        mqPushConsumer.setVipChannelEnabled(false);
//        mqPushConsumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv());
//        mqPushConsumer.setConsumerGroup(rocketMQConsumerProperties.getGroupName() + "_" + SyncCartConsumer.class.getSimpleName());
//        mqPushConsumer.setConsumeMessageBatchMaxSize(10);
//        mqPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
//        //监听购物车同步的行为
//        mqPushConsumer.subscribe(ShopCarProviderTopicNames.SYNC_MSG_TOPIC, "*");


        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                CartChangeEventBO cartChangeEventBO = JSON.parseObject(msg.getBody(), CartChangeEventBO.class);
                // 进行同步
                cartItemService.syncRedisToMysqlHandle(cartChangeEventBO);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }
}
