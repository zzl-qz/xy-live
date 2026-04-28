package org.xy.live.framework.redis.starter.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
public class MyRedissonConfig {
    /**
     * 对 Redisson 的使用都是通过 RedissonClient 对象
     * @return
     * @throws IOException
     */
    @Bean(destroyMethod="shutdown") // 服务停止后调用 shutdown 方法。
    public RedissonClient redisson() throws IOException {
        // 1. 创建配置
        Config config = new Config();

        // 2. 配置单节点模式
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setPassword("123456")
                .setDatabase(3)
                .setConnectionPoolSize(50)
                .setConnectionMinimumIdleSize(10)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(10000);

        return Redisson.create(config);
    }
}