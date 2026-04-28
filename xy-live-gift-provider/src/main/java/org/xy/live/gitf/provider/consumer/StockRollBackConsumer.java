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
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.xy.live.common.interfaces.constant.AclHookConstant;
import org.xy.live.common.interfaces.topic.GiftProviderTopicNames;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.gift.dto.ReceiveRedPacketHandlerMq;
import org.xy.live.gitf.provider.bo.RollBackStockBO;
import org.xy.live.gitf.provider.service.ISkuInfoService;
import org.xy.live.gitf.provider.service.ISkuStockInfoService;

/**
 * 库存回滚消费者
 */
@Configuration
public class StockRollBackConsumer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(StockRollBackConsumer.class);

    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;

    @Resource
    private ISkuStockInfoService skuStockInfoService;

    @Override
    public void afterPropertiesSet() throws Exception {

        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(AclHookConstant.ACCESS_KEY, AclHookConstant.SECRET_KEY));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName()   +  "_" + StockRollBackConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(10); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(GiftProviderTopicNames.ROLL_BACK_STOCK, "*"); // 订阅缓存删除主题


//        DefaultMQPushConsumer mqPushConsumer = new DefaultMQPushConsumer();
//        //老版本中会开启，新版本的mq不需要使用到
//        mqPushConsumer.setVipChannelEnabled(false);
//        mqPushConsumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv());
//        mqPushConsumer.setConsumerGroup(rocketMQConsumerProperties.getGroupName() + "_" + ReceiveRedPacketHandlerConsumer.class.getSimpleName());
//        //一次从broker中拉取10条消息到本地内存当中进行消费
//        mqPushConsumer.setConsumeMessageBatchMaxSize(1);
//        mqPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
//        //监听发送过来的库存消息topic
//        mqPushConsumer.subscribe(GiftProviderTopicNames.ROLL_BACK_STOCK, "*");



        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            try {
                // 进行库存回滚
                RollBackStockBO rollBackStockBO = JSON.parseObject(new String(msgs.get(0).getBody()), RollBackStockBO.class);
                LOGGER.info("[StockRollBackConsumer] 开始订单支付状态检查并确定是否要进行库存回滚，rollBackStockBO is {}", rollBackStockBO);
                skuStockInfoService.stockRollBackHandler(rollBackStockBO);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }

}
