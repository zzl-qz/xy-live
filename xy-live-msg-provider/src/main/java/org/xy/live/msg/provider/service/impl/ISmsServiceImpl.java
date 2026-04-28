package org.xy.live.msg.provider.service.impl;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xy.live.framework.redis.starter.key.MsgProviderCacheKeyBuilder;
import org.xy.live.msg.dto.MsgCheckDTO;
import org.xy.live.msg.enums.MsgSendResultEnum;
import org.xy.live.msg.provider.config.ThreadPoolManager;
import org.xy.live.msg.provider.dao.mapper.SmsMapper;
import org.xy.live.msg.provider.dao.po.SmsPO;
import org.xy.live.msg.provider.service.ISmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

import static org.xy.live.msg.enums.MsgSendResultEnum.*;

@Service
public class ISmsServiceImpl implements ISmsService {

    private static Logger logger = LoggerFactory.getLogger(ISmsServiceImpl.class);

    @Resource
    private MsgProviderCacheKeyBuilder msgProviderCacheKeyBuilder;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SmsMapper smsMapper;

    @Override
    public MsgSendResultEnum sendLoginCode(String phone) {
        // 判断手机号是否为空
        if (StringUtils.isEmpty(phone)) {
            return MSG_PARAM_ERROR;
        }
        // 生成验证码（6位，需要将这个验证码缓存起来，需要设置过期时间，同一个手机号只发1次）
        String cacheKey = msgProviderCacheKeyBuilder.buildSmsLoginCodeKey(phone);
        // 发送前先判断是不是最近已经发送过了（60秒内只能发送一次）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            logger.info("{} 该手机号发送过于频繁，请稍后重试" , phone);
            return SEND_FAIL;
        }
        // int code = RandomUtils.nextInt(100000, 999999);
        int code = 123456;
        logger.info("生成的验证码是：" + code);
        redisTemplate.opsForValue().set(cacheKey, code, 60, TimeUnit.SECONDS);
        // 发送验证码(异步去发)
        ThreadPoolManager.commonAsyncPool.execute(() -> {
            boolean sendStatus = mockSendSms(phone, code);
            if (sendStatus) {
                insertOne(phone, code);
            }

        });
        return SEND_SUCCESS;
    }

    @Override
    public MsgCheckDTO checkLoginCode(String phone, Integer code) {
        // 判断手机号是否为空，验证码格式是否正确
        if (StringUtils.isEmpty(phone) || code < 100000 || code > 999999) {
            return new MsgCheckDTO(false, "验证码格式错误");
        }
        // 查询redis
        String cacheKey = msgProviderCacheKeyBuilder.buildSmsLoginCodeKey(phone);
        Integer cacheCode = (Integer) redisTemplate.opsForValue().get(cacheKey);
        if (cacheCode == null || cacheCode < 100000 || cacheCode > 999999) {
            return new MsgCheckDTO(false, "验证码错误或已经过期");
        }
        // 两个验证码是否相同
        if (cacheCode.equals(code)) {
            // 删除缓存
            redisTemplate.delete(cacheKey);
            return new MsgCheckDTO(true, "验证码校验成功");
        }
        // 登录失败
        return new MsgCheckDTO(false, "验证码校验失败");
    }

    /**
     * 插入一条消息记录到数据库中（便后续对账）
     * @param phone
     * @param code
     */
    @Override
    public void insertOne(String phone, Integer code) {
        SmsPO smsPO = new SmsPO();
        smsPO.setPhone(phone);
        smsPO.setCode(code);
        smsMapper.insert(smsPO);
    }


    /**
     * 模拟发送短信过程，感兴趣的朋友可以尝试对接一些第三方的短信平台
     *
     * @param phone
     * @param code
     */
    private boolean mockSendSms(String phone, Integer code) {
        try {
            logger.info(" ============= 创建短信发送通道中 ============= ,phone is {},code is {}", phone, code);
            Thread.sleep(1000);
            logger.info(" ============= 短信已经发送成功 ============= ");
            return true;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
