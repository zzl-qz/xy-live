package org.xy.live.bank.provider.service.impl;

import jakarta.annotation.Resource;
import org.apache.rocketmq.client.producer.MQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xy.live.bank.interfaces.dto.AccountTradeReqDTO;
import org.xy.live.bank.interfaces.dto.AccountTradeRespDTO;
import org.xy.live.bank.interfaces.dto.XyCurrencyAccountDTO;
import org.xy.live.bank.provider.dao.mapper.XyCurrencyAccountMapper;
import org.xy.live.bank.provider.dao.po.XyCurrencyAccountPO;
import org.xy.live.bank.provider.service.XyCurrencyAccountService;
import org.xy.live.bank.provider.service.XyCurrencyTradeService;
import org.xy.live.common.interfaces.enums.TradeTypeEnum;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.BankProviderCacheKeyBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class XyCurrencyAccountServiceImpl implements XyCurrencyAccountService {

    private final Logger LOGGER = LoggerFactory.getLogger(XyCurrencyAccountServiceImpl.class);

    @Resource
    private XyCurrencyAccountMapper xyCurrencyAccountMapper;

    @Resource
    private XyCurrencyTradeService xyCurrencyTradeService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private BankProviderCacheKeyBuilder cacheKeyBuilder;

    @Resource
    private ApplicationContext applicationContext; // 当前应用上下文


    /**
     * 获取代理对象方法
     * @return
     */
    private XyCurrencyAccountServiceImpl self() {
        return applicationContext.getBean(XyCurrencyAccountServiceImpl.class);
    }

    @Override
    public boolean insertOne(Long userId) {
        try {
            XyCurrencyAccountPO xyCurrencyAccountPO = new XyCurrencyAccountPO();
            xyCurrencyAccountPO.setUserId(userId);
            xyCurrencyAccountMapper.insert(xyCurrencyAccountPO);
            return true;
        } catch (Exception e) {
            //有异常但是不抛出，只为了避免重复创建相同userId的账户
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incr(Long userId, int num, TradeTypeEnum tradeTypeEnum, String extraCol) {
        // db 操作
        xyCurrencyAccountMapper.incr(userId, num); // 扣减余额(DB层)
        xyCurrencyTradeService.insertOne(userId, num, tradeTypeEnum.getCode(), extraCol); // 流水记录

        // 删除redis余额缓存
        deleteBalanceCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decr(Long userId, int num, TradeTypeEnum tradeTypeEnum, String extraCol) {
        // db 操作
        xyCurrencyAccountMapper.decr(userId, num); // 余额扣减
        xyCurrencyTradeService.insertOne(userId, num * -1, tradeTypeEnum.getCode(), extraCol); // 流水记录

        // 删除redis余额缓存
        deleteBalanceCache(userId);
        return true;
    }

    @Override
    public XyCurrencyAccountDTO getByUserId(Long userId) {
        return ConvertBeanUtils.convert(xyCurrencyAccountMapper.selectById(userId), XyCurrencyAccountDTO.class);
    }

    @Override
    public Integer getBalance(Long userId) {
        // 查询redis
        String cacheKey = cacheKeyBuilder.buildUserBalance(userId);
        String balanceStr = stringRedisTemplate.opsForValue().get(cacheKey);
        Integer balance;
        if (balanceStr != null && !balanceStr.isBlank()) {
            balance = Integer.parseInt(balanceStr);
            if (balance != -1) {
                return balance;
            }
            return null;
        }
        // 查询数据库
        balance = xyCurrencyAccountMapper.queryBalance(userId);
        if (balance != null) {
            // 补偿操作
            stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(balance), 30, TimeUnit.MINUTES);
            return balance;
        }
        // 穿透了
        stringRedisTemplate.opsForValue().set(cacheKey, "-1", 30, TimeUnit.MINUTES);
        return null;
    }

    /**
     * 大并发请求场景，1000个直播间，500人，50W人在线，20%的人送礼，10W人在线触发送礼行为，DB扛不住
     * 1.MySQL换成写入性能相对较高的数据库
     * 2.我们能不能从业务上去进行优化，用户送礼都在直播间，大家都连接上了im服务器，router层扩容(50台)，im-core-server层(100台)，MQ削峰
     * 消费端也可以水平扩容
     * 3.我们客户端发起送礼行为的时候，同步校验（校验账户余额是否足够，余额放入到Redis中）
     * 4.拦下大部分的请求，如果余额不足，（接口还得做防止重复点击，客户端也要防重复）
     * 5.同步送礼接口，只完成简单的余额校验，发送mq，在mq的异步操作里面，完成二次余额校验，余额扣减，礼物发送
     * 6.如果余额不足，是不是可以利用im，反向通知发送方，余额充足，利用im实现礼物特效推送
     *
     * @param accountTradeReqDTO
     * @return
     */
    @Override
    public AccountTradeRespDTO consumeForSendGift(AccountTradeReqDTO accountTradeReqDTO) {
        // 1 余额判断并在Redis中扣减余额
        Long userId = accountTradeReqDTO.getUserId();
        int num = accountTradeReqDTO.getNum();
        String lockKey = "xy-live-bank-provider:balance:lock:" + userId;
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 1L, TimeUnit.SECONDS);
        // 判断余额和余额扣减操作要保证原子性
        if (Boolean.TRUE.equals(isLock)) {
            try {
                Integer balance = this.getBalance(userId);
                if (balance == null || balance < num) {
                    return AccountTradeRespDTO.buildFail(userId, "账户余额不足", 1);
                }
                // 封装的方法：包括redis余额扣减和 异步DB层处理
                self().decr(userId, num, TradeTypeEnum.SEND_GIFT_TRADE, "");
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        } else {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1000));
            } catch (InterruptedException e) {
                LOGGER.error("[consumeForSendGift] is error, {}", e.getMessage());
            }
            // 等待0.5~1秒后重试
            consumeForSendGift(accountTradeReqDTO);
        }
        return AccountTradeRespDTO.buildSuccess(userId, "扣费成功");
    }


    /**
     * 删除余额缓存
     * @param userId
     */
    private void deleteBalanceCache(Long userId) {
        String cacheKey = cacheKeyBuilder.buildUserBalance(userId);
        stringRedisTemplate.delete(cacheKey);
    }

}
