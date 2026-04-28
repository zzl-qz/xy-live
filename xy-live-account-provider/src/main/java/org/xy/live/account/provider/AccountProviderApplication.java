package org.xy.live.account.provider;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.xy.live.account.provider.service.AccountTokenService;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
public class AccountProviderApplication{

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(AccountProviderApplication.class);
        // 设置应用类型为非 Web 应用（无 Servlet 容器）
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }
}