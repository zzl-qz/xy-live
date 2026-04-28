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

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class RocketMQProducerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQProducerConfig.class);

    @Resource
    private RocketMQProducerProperties rocketMQProducerProperties;

    // ACL 密钥（建议移到配置文件，不要硬编码）
    private final String accessKey = "";
    private final String secretKey = "";
    private final String nameSrvAddr = "";

    @Bean
    public MQProducer mqProducer() {
        // 1. 异步发送线程池（保留原有逻辑）
        ThreadPoolExecutor asyncThreadPool = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() * 2,
                100,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2000)
        );

        try {
            // 2. 构建 ACL 鉴权钩子
            AclClientRPCHook rpcHook = new AclClientRPCHook(new SessionCredentials(accessKey, secretKey));

            // 3. 核心修复：使用无 namespace 的构造方法（仅传组名 + RPCHook）
            // 绝对不要把 NameServer 地址传入 namespace 参数！
            DefaultMQProducer defaultMQProducer = new DefaultMQProducer(
                    rocketMQProducerProperties.getGroupName(), // 仅合法的组名
                    rpcHook
            );

            // 4. 正确配置 NameServer 地址（单独设置，不是构造方法参数）
            defaultMQProducer.setNamesrvAddr(rocketMQProducerProperties.getNameSrv() != null
                    ? rocketMQProducerProperties.getNameSrv()
                    : nameSrvAddr);

            // 5. 其他配置（保留原有逻辑）
            defaultMQProducer.setRetryTimesWhenSendFailed(rocketMQProducerProperties.getRetryTimes());
            defaultMQProducer.setRetryTimesWhenSendAsyncFailed(rocketMQProducerProperties.getRetryTimes());
            defaultMQProducer.setRetryAnotherBrokerWhenNotStoreOK(true);
            defaultMQProducer.setAsyncSenderExecutor(asyncThreadPool);
            defaultMQProducer.setSendMsgTimeout(10000);

            // 腾讯云 TDmq 适配配置
            defaultMQProducer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
            defaultMQProducer.setVipChannelEnabled(false);

            // 6. 启动生产者
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