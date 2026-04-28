package org.xy.live.user.provider;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.xy.live.user.provider.service.IUserPhoneService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 用户服务中台启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableDubbo
public class UserProviderApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(UserProviderApplication.class);
        // 设置应用类型为非 Web 应用（无 Servlet 容器）
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }


    @Resource
    private IUserPhoneService userPhoneService;

    @Override
    public void run(String... args) throws Exception {
        // initData();
    }

    void initData() {
        List<String> phones = generatePhoneNumbers(10000);
        for (String phone : phones) {
             userPhoneService.login(phone);
        }

    }



    // 常见手机号段前缀
    private static final String[] PREFIXES = {
            // 中国移动
            "134", "135", "136", "137", "138", "139", "147", "150", "151", "152",
            "157", "158", "159", "178", "182", "183", "184", "187", "188", "198",
            // 中国联通
            "130", "131", "132", "145", "155", "156", "166", "175", "176", "185", "186",
            // 中国电信
            "133", "149", "153", "173", "177", "180", "181", "189", "199",
            // 虚拟运营商
            "170", "171"
    };




    private static final Random random = new Random();

    /**
     * 生成指定数量的手机号
     */
    public static List<String> generatePhoneNumbers(int count) {
        List<String> phoneNumbers = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String prefix = PREFIXES[random.nextInt(PREFIXES.length)];
            String suffix = generateRandomDigits(8);
            phoneNumbers.add(prefix + suffix);
        }

        return phoneNumbers;
    }

    /**
     * 生成指定长度的随机数字字符串
     */
    private static String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }


}
