package org.xy.live.framework.mq.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@ConfigurationProperties(prefix = "xy.rmq.producer")
@Configuration
public class RocketMQProducerProperties {

    private String nameSrv;
    private String groupName;
    private String applicationName;
    private Integer sendMsgTimeout;
    private Integer retryTimes;
    private String accessKey;
    private String secretKey;

    public String getNameSrv() {
        return nameSrv;
    }

    public void setNameSrv(String nameSrv) {
        this.nameSrv = nameSrv;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Integer getSendMsgTimeout() {
        return sendMsgTimeout;
    }

    public void setSendMsgTimeout(Integer sendMsgTimeout) {
        this.sendMsgTimeout = sendMsgTimeout;
    }

    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String toString() {
        return "RocketMQProducerProperties{" +
                "nameSrv='" + nameSrv + '\'' +
                ", groupName='" + groupName + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", sendMsgTimeout=" + sendMsgTimeout +
                ", retryTimes=" + retryTimes +
                '}';
    }
}
