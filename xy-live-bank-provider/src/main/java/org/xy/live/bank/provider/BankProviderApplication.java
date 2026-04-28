package org.xy.live.bank.provider;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.xy.live.bank.interfaces.dto.PayProductDTO;
import org.xy.live.bank.provider.service.PayProductService;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
@EnableDiscoveryClient
@EnableDubbo
public class BankProviderApplication {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        SpringApplication springApplication = new SpringApplication(BankProviderApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
        countDownLatch.await();
    }

    //implements CommandLineRunner
//    @Resource
//    private PayProductService payProductService;
//    @Override
//    public void run(String... args) throws Exception {
//        List<PayProductDTO> products = payProductService.products(0);
//        for (PayProductDTO product : products) {
//            System.out.println("product数据:" + product);
//        }
//    }
}