package org.xy.live.web.starter.context;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.xy.live.web.starter.error.XyErrorException;
import org.xy.live.web.starter.limit.RequestLimit;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 对于重复请求，要有专门的拦截器去处理，进行相同用户下的限流
 */
public class RequestLimitInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLimitInterceptor.class);

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 查看有无 @RequestLimit 注解标识该方法
            boolean hasLimit = handlerMethod.getMethod().isAnnotationPresent(RequestLimit.class);
            // 如果加了限流注解就进行判断
            if (hasLimit) {
                RequestLimit requestLimit = handlerMethod.getMethod().getAnnotation(RequestLimit.class);
                Long userId = XyRequestContext.getUserId();
                // 没有userId标识是网关通过的白名单请求，放行
                if (userId == null) {
                    return true;
                }
                //(userId + url + requestValue) base64 -> String(key)
                String cacheKey = applicationName + ":" + userId + ":" + request.getRequestURI();
                int limit = requestLimit.limit();// 限制访问数量上限
                int second = requestLimit.second();// 时间窗口
                // 下面这一部分的思路是这样的，就是我设置到redis的时候，过期时间就是second，也就是限定的时间内，
                // 然后value就是这段时间内限定的最大次数，那么只要我去get的时候发现这个数据还在就说明还没过这个时间，
                // 如果获取不到就说明超过这个时间窗口，那么就是重新开始，并且将这个数据设置到redis
                Integer reqTime = (Integer) Optional.ofNullable(redisTemplate.opsForValue().get(cacheKey)).orElse(0);
                if (reqTime == 0) {
                    redisTemplate.opsForValue().set(cacheKey, 1, second, TimeUnit.SECONDS);
                    return true;
                } else if (reqTime < limit) {
                    // 次数加1
                    redisTemplate.opsForValue().increment(cacheKey, 1);
                    return true;
                }
                // 超过限流数量上限
                // 直接抛出全局异常，让异常捕获器处理
                LOGGER.error("[RequestLimitInterceptor] userId is {}, req too much", userId);
                throw new XyErrorException(-10001, requestLimit.msg());
            }
        }
        return true;
    }
}