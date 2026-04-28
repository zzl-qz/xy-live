package org.xy.live.web.starter.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.xy.live.web.starter.context.RequestLimitInterceptor;
import org.xy.live.web.starter.context.XyUserInfoInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public XyUserInfoInterceptor xyUserInfoInterceptor() {
        return new XyUserInfoInterceptor();
    }

    @Bean
    public RequestLimitInterceptor requestLimitInterceptor() {
        return new RequestLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(xyUserInfoInterceptor()).addPathPatterns("/**").excludePathPatterns("/error");
        registry.addInterceptor(requestLimitInterceptor()).addPathPatterns("/**").excludePathPatterns("/error");
    }
}