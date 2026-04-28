package org.xy.live.xxljob.starter.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(XxlJobProperties.class)
@ConditionalOnProperty(prefix = "xxl.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(XxlJobAutoConfiguration.class);

    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor(XxlJobProperties props) {
        LOGGER.info("已经启动XxlJob配置类");
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(props.getAdmin().getAddresses());
        executor.setAppname(props.getExecutor().getAppname());
        executor.setIp(props.getExecutor().getIp());
        executor.setPort(props.getExecutor().getPort());
        executor.setLogPath(props.getExecutor().getLogPath());
        executor.setLogRetentionDays(props.getExecutor().getLogRetentionDays());
        executor.setAccessToken(props.getAccessToken());
        return executor;
    }
}