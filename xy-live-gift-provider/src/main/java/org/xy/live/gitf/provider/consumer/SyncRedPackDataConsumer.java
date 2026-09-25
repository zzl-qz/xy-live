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
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.gitf.provider.bo.CartChangeEventBO;
import org.xy.live.gitf.provider.bo.SyncRedPackDataBO;
import org.xy.live.gitf.provider.service.CartItemService;
import org.xy.live.gitf.provider.service.IRedPacketConfigService;

import static org.xy.live.common.interfaces.topic.GiftProviderTopicNames.SYNC_RED_PACK_DATA_TOPIC;
import static org.xy.live.common.interfaces.topic.ShopCarProviderTopicNames.CLEAR_CAR_MSG_TOPIC;

/**
 * 发送同步红包雨活动数据消费对象
 */
@Configuration
public class SyncRedPackDataConsumer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncRedPackDataConsumer.class);

    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;

    @Resource
    private IRedPacketConfigService redPacketConfigService;


    @Override
    public void afterPropertiesSet() throws Exception {

        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(rocketMQConsumerProperties.getAccessKey(), rocketMQConsumerProperties.getSecretKey()));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName() +  "_" + SyncRedPackDataConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(10); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(SYNC_RED_PACK_DATA_TOPIC, "*"); // 订阅缓存删除主题


//        DefaultMQPushConsumer mqPushConsumer = new DefaultMQPushConsumer();
//        //老版本中会开启，新版本的mq不需要使用到
//        mqPushConsumer.setVipChannelEnabled(false);
//        mqPushConsumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv());
//        mqPushConsumer.setConsumerGroup(rocketMQConsumerProperties.getGroupName() + "_" + ClearCartConsumer.class.getSimpleName());
//        mqPushConsumer.setConsumeMessageBatchMaxSize(10);
//        mqPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
//        //监听购物车清空的行为
//        mqPushConsumer.subscribe(CLEAR_CAR_MSG_TOPIC, "*");


        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                SyncRedPackDataBO syncRedPackDataBO = JSON.parseObject(msg.getBody(), SyncRedPackDataBO.class);
                if (syncRedPackDataBO.getCode() == null) {
                    LOGGER.warn("[SyncRedPackDataConsumer] msg is {}，缺少关键信息 code", msg);
                    continue;
                }
                // 调用同步接口
                redPacketConfigService.SyncRedPackDataHandler(syncRedPackDataBO.getCode(), syncRedPackDataBO.getCnt());
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }
}
