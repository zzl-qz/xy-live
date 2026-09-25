package org.xy.live.living.provider.comsumer;

import com.alibaba.fastjson.JSON;
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
import org.springframework.stereotype.Component;
import org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.im.core.server.interfaces.dto.ImOfflineDTO;
import org.xy.live.living.provider.service.ILivingRoomService;


/**
 * 用户下线消息消费者
 */
@Component
public class LivingRoomOfflineConsumer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(LivingRoomOfflineConsumer.class);
    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;
    @Resource
    private ILivingRoomService livingRoomService;

    @Override
    public void afterPropertiesSet() throws Exception {

        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(rocketMQConsumerProperties.getAccessKey(), rocketMQConsumerProperties.getSecretKey()));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName() + "_" + LivingRoomOfflineConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(10); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(ImCoreServerProviderTopicNames.IM_OFFLINE_TOPIC, "*"); // 订阅缓存删除主题



//        DefaultMQPushConsumer mqPushConsumer = new DefaultMQPushConsumer();
//        //老版本中会开启，新版本的mq不需要使用到
//        mqPushConsumer.setVipChannelEnabled(false);
//        mqPushConsumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv());
//        mqPushConsumer.setConsumerGroup(rocketMQConsumerProperties.getGroupName() + "_" + LivingRoomOfflineConsumer.class.getSimpleName());
//        //一次从broker中拉取10条消息到本地内存当中进行消费
//        mqPushConsumer.setConsumeMessageBatchMaxSize(10);
//        mqPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
//        //监听im发送过来的业务消息topic
//        mqPushConsumer.subscribe(ImCoreServerProviderTopicNames.IM_OFFLINE_TOPIC, "*");


        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                livingRoomService.userOfflineHandler(JSON.parseObject(new String(msg.getBody()), ImOfflineDTO.class));
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }
}
