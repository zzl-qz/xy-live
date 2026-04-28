package org.xy.live.msg.provider.consumer;

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
import org.xy.live.common.interfaces.constant.AclHookConstant;
import org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.im.dto.ImMsgBody;
import org.xy.live.msg.provider.consumer.handler.MessageHandler;

import static org.xy.live.common.interfaces.topic.MsgProviderTopicNames.XY_LIVE_DANMU_MSG_PER_TOPIC;

/**
 * @Author: Daylight
 * @Date: 2025-12-20 19:28
 * @Description: 直播间弹幕消息持久化类
 */

@Component
public class MsgPersistenceConsumer implements InitializingBean {


    private static final Logger LOGGER = LoggerFactory.getLogger(MsgPersistenceConsumer.class);
    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;
    @Resource
    private MessageHandler singleMessageHandler;

    /**
     * 每次有消息过来都会调用这个方法进行消费
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(AclHookConstant.ACCESS_KEY, AclHookConstant.SECRET_KEY));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName()  + "_" + MsgPersistenceConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(10); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(XY_LIVE_DANMU_MSG_PER_TOPIC, "*"); // 订阅缓存删除主题


//
//        DefaultMQPushConsumer mqPushConsumer = new DefaultMQPushConsumer();
//        //老版本中会开启，新版本的mq不需要使用到
//        mqPushConsumer.setVipChannelEnabled(false);
//        mqPushConsumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv());
//        mqPushConsumer.setConsumerGroup(rocketMQConsumerProperties.getGroupName() + "_" + ImMsgConsumer.class.getSimpleName());
//        //一次从broker中拉取100条消息到本地内存当中进行消费
//        mqPushConsumer.setConsumeMessageBatchMaxSize(100);
//        mqPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
//        //监听弹幕消息持久化topic
//        mqPushConsumer.subscribe(XY_LIVE_DANMU_MSG_PER_TOPIC, "*");


        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            // 遍历每一条消息
            for (MessageExt msg : msgs) {
                // 将消息中的body获取出来并进行对象转换
                ImMsgBody imMsgBody = JSON.parseObject(new String(msg.getBody()), ImMsgBody.class);
                // 消息持久化吧
                singleMessageHandler.msgPersistence(imMsgBody);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }
}
