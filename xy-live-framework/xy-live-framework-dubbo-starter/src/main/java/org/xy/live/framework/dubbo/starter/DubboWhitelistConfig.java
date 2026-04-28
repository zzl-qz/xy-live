package org.xy.live.framework.dubbo.starter;


import org.apache.dubbo.config.ApplicationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DubboWhitelistConfig {

    // 方式1: 通过代码配置（推荐）
    @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setSerializeCheckStatus("DISABLE"); // 关闭严格检查（不推荐）
        return config;
    }

}
