package org.xy.live.framework.mq.starter.producer;

import jakarta.annotation.Resource;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xy.live.framework.mq.starter.properties.RocketMQProducerProperties;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class RocketMQProducerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQProducerConfig.class);

    @Resource
    private RocketMQProducerProperties rocketMQProducerProperties;

    private final String nameSrvAddr = "";

    @Bean
    public MQProducer mqProducer() {
        ThreadPoolExecutor asyncThreadPool = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() * 2,
                100,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2000)
        );

        try {
            AclClientRPCHook rpcHook = new AclClientRPCHook(new SessionCredentials(
                    rocketMQProducerProperties.getAccessKey(),
                    rocketMQProducerProperties.getSecretKey()));

            DefaultMQProducer defaultMQProducer = new DefaultMQProducer(
                    rocketMQProducerProperties.getGroupName(),
                    rpcHook
            );

            defaultMQProducer.setNamesrvAddr(rocketMQProducerProperties.getNameSrv() != null
                    ? rocketMQProducerProperties.getNameSrv()
                    : nameSrvAddr);
            defaultMQProducer.setRetryTimesWhenSendFailed(rocketMQProducerProperties.getRetryTimes());
            defaultMQProducer.setRetryTimesWhenSendAsyncFailed(rocketMQProducerProperties.getRetryTimes());
            defaultMQProducer.setRetryAnotherBrokerWhenNotStoreOK(true);
            defaultMQProducer.setAsyncSenderExecutor(asyncThreadPool);
            defaultMQProducer.setSendMsgTimeout(rocketMQProducerProperties.getSendMsgTimeout());
            defaultMQProducer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
            defaultMQProducer.setVipChannelEnabled(false);
            defaultMQProducer.start();

            LOGGER.info("=============== mq生产者启动成功,namesrv is {} ==================", defaultMQProducer.getNamesrvAddr());
            return defaultMQProducer;
        } catch (MQClientException e) {
            LOGGER.error("MQ生产者启动失败，组名：{}，NameServer：{}",
                    rocketMQProducerProperties.getGroupName(),
                    rocketMQProducerProperties.getNameSrv(),
                    e);
            throw new RuntimeException("RocketMQ 生产者初始化失败", e);
        }
    }
}
