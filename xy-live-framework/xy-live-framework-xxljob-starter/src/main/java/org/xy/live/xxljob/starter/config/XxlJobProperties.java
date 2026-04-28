package org.xy.live.xxljob.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {
    private String accessToken;
    private Admin admin = new Admin();
    private Executor executor = new Executor();

    @Data
    public static class Admin {
        private String addresses;
    }

    @Data
    public static class Executor {
        private String appname;
        private String ip;
        private Integer port = 9999;
        private String logPath = "./logs/xxl-job";
        private Integer logRetentionDays = 7;
    }
}