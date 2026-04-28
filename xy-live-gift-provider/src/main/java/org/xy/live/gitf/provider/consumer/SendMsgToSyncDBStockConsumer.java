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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.xy.live.common.interfaces.constant.AclHookConstant;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.gitf.provider.bo.SendMsgToSyncDBStockBO;
import org.xy.live.gitf.provider.service.ISkuStockInfoService;

import static org.xy.live.common.interfaces.topic.SkuStockProviderTopicNames.SEND_MSG_TO_SYNC_DB_STOCK_TOPIC;

/**
 * @Author: Daylight
 * @Date: 2026-01-27 16:04
 */
@Configuration
public class SendMsgToSyncDBStockConsumer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendMsgToSyncDBStockConsumer.class);

    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;

    @Resource
    private ISkuStockInfoService skuStockInfoService;



    @Override
    public void afterPropertiesSet() throws Exception {

        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(AclHookConstant.ACCESS_KEY, AclHookConstant.SECRET_KEY));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName()  +  "_" + SendMsgToSyncDBStockConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(1);
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(SEND_MSG_TO_SYNC_DB_STOCK_TOPIC, "*"); // 订阅缓存删除主题


        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            // 处理一下库存同步操作
            SendMsgToSyncDBStockBO stockBO = JSON.parseObject(new String(msgs.get(0).getBody()), SendMsgToSyncDBStockBO.class);
            // todo 幂等处理
            Long skuId = stockBO.getSkuId();
            Integer num = stockBO.getNum();
            Integer type = stockBO.getType();
            Integer cnt = stockBO.getCnt();
            String msgId = stockBO.getMsgId();
            boolean success = skuStockInfoService.updateStockNumByTypeAndSkuId(skuId, num, type, msgId);
            if (!success) {
                skuStockInfoService.SendMsgToSyncDBStock(skuId, num, type, cnt + 1, msgId);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }
}
