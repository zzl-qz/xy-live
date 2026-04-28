package org.xy.live.im.core.server.consumer;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.xy.live.common.interfaces.constant.AclHookConstant;
import org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames;
import org.xy.live.common.interfaces.topic.ShopCarProviderTopicNames;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.im.core.server.service.IMsgAckCheckService;
import org.xy.live.im.core.server.service.IRouterHandlerService;
import org.xy.live.im.dto.ImMsgBody;

/**
 * ack消费者
 */
@Configuration
public class ImAckConsumer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImAckConsumer.class);

    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;
    @Resource
    private IMsgAckCheckService msgAckCheckService;
    @Resource
    private IRouterHandlerService routerHandlerService;

    @Override
    public void afterPropertiesSet() throws Exception {

        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(AclHookConstant.ACCESS_KEY, AclHookConstant.SECRET_KEY));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName() +  "_" + ImAckConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(10); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(ImCoreServerProviderTopicNames.XY_LIVE_IM_ACK_MSG_TOPIC, "*"); // 订阅缓存删除主题


//        DefaultMQPushConsumer mqPushConsumer = new DefaultMQPushConsumer();
//        mqPushConsumer.setVipChannelEnabled(false);
//        //设置我们的namesrv地址
//        mqPushConsumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv());
//        //声明消费组
//        mqPushConsumer.setConsumerGroup(rocketMQConsumerProperties.getGroupName() + "_" + ImAckConsumer.class.getSimpleName());
//        //每次只拉取一条消息
//        mqPushConsumer.setConsumeMessageBatchMaxSize(1);
//        mqPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
//        mqPushConsumer.subscribe(ImCoreServerProviderTopicNames.XY_LIVE_IM_ACK_MSG_TOPIC, "*");


        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            String json = new String(msgs.get(0).getBody());
            ImMsgBody imMsgBody = JSON.parseObject(json, ImMsgBody.class);
            int retryTimes = msgAckCheckService.getMsgAckTimes(imMsgBody.getMsgId(), imMsgBody.getUserId(), imMsgBody.getAppId());
            LOGGER.info("retryTimes is {},msgId is {}", retryTimes, imMsgBody.getMsgId());
            if (retryTimes < 0) {
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
            //只支持2次重尝试
            if (retryTimes < 2) {
                msgAckCheckService.recordMsgAck(imMsgBody, retryTimes + 1);
                msgAckCheckService.sendDelayMsg(imMsgBody);
                routerHandlerService.sendMsgToClient(imMsgBody);
            } else {
                // 重新发送还是失败，直接不管了，没必要保证信息一定到
                msgAckCheckService.doMsgAck(imMsgBody);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }


}
