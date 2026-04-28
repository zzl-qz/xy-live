package org.xy.live.gitf.provider.consumer;


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
import org.springframework.context.annotation.Configuration;
import org.xy.live.common.interfaces.constant.AclHookConstant;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.gift.dto.ReceiveRedPacketHandlerMq;
import org.xy.live.gitf.provider.service.IRedPacketConfigService;
import static org.xy.live.common.interfaces.topic.GiftProviderTopicNames.RECEIVE_RED_PACKET_HANDLER;

/**
 * 用户抢红包处理消费者
 */
@Configuration
public class ReceiveRedPacketHandlerConsumer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveRedPacketHandlerConsumer.class);

    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;

    @Resource
    private IRedPacketConfigService redPacketConfigService;

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
                rocketMQConsumerProperties.getGroupName()  +  "_" + ReceiveRedPacketHandlerConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(10); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(RECEIVE_RED_PACKET_HANDLER, "*"); // 订阅缓存删除主题


//        DefaultMQPushConsumer mqPushConsumer = new DefaultMQPushConsumer();
//        //老版本中会开启，新版本的mq不需要使用到
//        mqPushConsumer.setVipChannelEnabled(false);
//        mqPushConsumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv());
//        mqPushConsumer.setConsumerGroup(rocketMQConsumerProperties.getGroupName() + "_" + ReceiveRedPacketHandlerConsumer.class.getSimpleName());
//        //一次从broker中拉取10条消息到本地内存当中进行消费
//        mqPushConsumer.setConsumeMessageBatchMaxSize(10);
//        mqPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
//        //监听发送过来的业务消息topic
//        mqPushConsumer.subscribe(RECEIVE_RED_PACKET_HANDLER, "*");



        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            // 遍历每一条消息
            for (MessageExt msg : msgs) {
                // 将消息中的body获取出来并进行对象转换
                ReceiveRedPacketHandlerMq imMsgBody = JSON.parseObject(new String(msg.getBody()), ReceiveRedPacketHandlerMq.class);
                String code = imMsgBody.getCode();
                Integer price = imMsgBody.getPrice();
                Long userId = imMsgBody.getUserId();
                // 处理抢红包时的参数记录
                redPacketConfigService.receiveRedPacketHandler(code, price, userId);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }

}
