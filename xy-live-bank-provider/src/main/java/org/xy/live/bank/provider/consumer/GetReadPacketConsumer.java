package org.xy.live.bank.provider.consumer;

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
import org.xy.live.common.interfaces.enums.TradeTypeEnum;
import org.xy.live.bank.provider.service.XyCurrencyAccountService;
import org.xy.live.common.interfaces.dto.RedPacketMq;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;

import static org.xy.live.common.interfaces.topic.GiftProviderTopicNames.ADD_RED_PACKET_PRICE;


/**
 * 消费处理用户抢到红包之后投递的信息
 */
@Component
public class GetReadPacketConsumer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetReadPacketConsumer.class);
    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;


    @Resource
    private XyCurrencyAccountService xyCurrencyAccountService;

    /**
     * 每次有消息过来都会调用这个方法进行消费
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(rocketMQConsumerProperties.getAccessKey(), rocketMQConsumerProperties.getSecretKey()));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName()  +  "_" + GetReadPacketConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(10); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(ADD_RED_PACKET_PRICE, "*"); // 订阅缓存删除主题


        // DefaultMQPushConsumer mqPushConsumer = new DefaultMQPushConsumer();
        //老版本中会开启，新版本的mq不需要使用到
        // mqPushConsumer.setVipChannelEnabled(false);
        // mqPushConsumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv());
        // mqPushConsumer.setConsumerGroup(rocketMQConsumerProperties.getGroupName() + "_" + GetReadPacketConsumer.class.getSimpleName());
        // mqPushConsumer.setConsumeMessageBatchMaxSize(10);
        // mqPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        //监听im发送过来的业务消息topic
        // mqPushConsumer.subscribe(ADD_RED_PACKET_PRICE, "*");


        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            // 遍历每一条消息
            for (MessageExt msg : msgs) {
                // 将消息中的body获取出来并进行对象转换
                 RedPacketMq imMsgBody = JSON.parseObject(new String(msg.getBody()), RedPacketMq.class);
                 LOGGER.info("imMsgBody, {}", imMsgBody);
                // 对用户的金额进行一下增长操作
                 xyCurrencyAccountService.incr(imMsgBody.getUserId(), imMsgBody.getPrice(), TradeTypeEnum.GET_RED_PACKET, "");
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }
}
