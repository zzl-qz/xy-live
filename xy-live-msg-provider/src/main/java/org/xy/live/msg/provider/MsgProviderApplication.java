package org.xy.live.msg.provider;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.xy.live.im.dto.ImMsgBody;
import org.xy.live.msg.dto.MessageDTO;
import org.xy.live.msg.provider.consumer.handler.MessageHandler;

import java.time.LocalDate;

/**
 * 验证码发送启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableDubbo
public class MsgProviderApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(MsgProviderApplication.class);
        // 设置应用类型为非 Web 应用（无 Servlet 容器）
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }
}
