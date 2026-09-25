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
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.xy.live.common.interfaces.topic.BankProviderTopicNames;
import org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.im.dto.ImMsgBody;
import org.xy.live.msg.provider.consumer.handler.MessageHandler;

import java.nio.charset.StandardCharsets;

// 记录每个用户连接的im服务器地址，然后根据im服务器的连接地址去做具体机器的调用
// 基于mq广播思路去做，可能会有消息风暴发生，100台im机器，99%的mq消息都是无效的，
// 加入一个叫路由层的设计，router中转的设计，router就是一个dubbo的rpc层
// A--》B:
// im-core-server(消息处理) -> msg-provider(持久化) -> im-core-server(这里需要借助router知道传播到哪台机器上) -> 通知到b


/**
 * IM 服务消息消费者
 */
/**
 * IM 服务消息消费者
 */
@Component
public class ImMsgConsumer implements InitializingBean { // 实现DisposableBean接口

    private static final Logger LOGGER = LoggerFactory.getLogger(ImMsgConsumer.class);


    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;

    @Resource
    private MessageHandler singleMessageHandler;

    @Override
    public void afterPropertiesSet() throws Exception {


        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(rocketMQConsumerProperties.getAccessKey(), rocketMQConsumerProperties.getSecretKey()));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName()  + "_" + ImMsgConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(3); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(ImCoreServerProviderTopicNames.XY_LIVE_IM_BIZ_MSG_TOPIC, "*"); // 订阅缓存删除主题


        // 4. 消息监听器（增加完整的异常处理）
        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) ->

        {
            // 标记是否消费成功
            boolean consumeSuccess = true;
            for (MessageExt msg : msgs) {
                try {
                    // 打印消息基本信息，便于排查
                    LOGGER.info("接收MQ消息，msgId: {}, topic: {}, tags: {}",
                            msg.getMsgId(), msg.getTopic(), msg.getTags());

                    // 解析消息体
                    String msgBodyStr = new String(msg.getBody(), StandardCharsets.UTF_8);
                    ImMsgBody imMsgBody = JSON.parseObject(msgBodyStr, ImMsgBody.class);
                    LOGGER.info("解析后的IM消息体: {}", imMsgBody);

                    // 处理消息（持久化+业务处理）
                    singleMessageHandler.msgPersistence(imMsgBody);
                    singleMessageHandler.onMsgReceive(imMsgBody);

                } catch (Exception e) {
                    // 捕获所有异常，记录详细日志
                    LOGGER.error("消费MQ消息失败，msgId: {}, 异常信息: {}", msg.getMsgId(), e.getMessage(), e);
                    // 标记消费失败，让Broker重发
                    consumeSuccess = false;
                    // 单条消息失败，跳过继续处理下一条
                    continue;
                }
            }

            // 根据消费结果返回状态
            return consumeSuccess ? ConsumeConcurrentlyStatus.CONSUME_SUCCESS
                    : ConsumeConcurrentlyStatus.RECONSUME_LATER;
        });

        // 5. 启动消费者（增加启动异常处理）
        try {
            consumer.start();
            LOGGER.info("MQ消费者启动成功, namesrv: {}, consumerGroup: {}",
                    rocketMQConsumerProperties.getNameSrv(),
                    consumer.getConsumerGroup());
        } catch (MQClientException e) {
            LOGGER.error("MQ消费者启动失败", e);
            // 启动失败抛出异常，阻止Spring容器初始化
            throw new RuntimeException("MQ消费者启动失败", e);
        }
    }
}
