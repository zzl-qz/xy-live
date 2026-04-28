package org.xy.live.gitf.provider.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;
import org.xy.live.common.interfaces.topic.GiftProviderTopicNames;
import org.xy.live.common.interfaces.topic.SkuStockProviderTopicNames;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.constants.SkuOrderInfoEnum;
import org.xy.live.gift.constants.SyncStockToDBTypeEnum;
import org.xy.live.gift.dto.SkuOrderInfoReqDTO;
import org.xy.live.gift.dto.SkuOrderInfoRespDTO;
import org.xy.live.gitf.provider.bo.RollBackStockBO;
import org.xy.live.gitf.provider.bo.SendMsgToSyncDBStockBO;
import org.xy.live.gitf.provider.dao.mapper.ISkuStockInfoMapper;
import org.xy.live.gitf.provider.dao.po.SkuStockInfoPO;
import org.xy.live.gitf.provider.service.ISkuOrderInfoService;
import org.xy.live.gitf.provider.service.ISkuStockInfoService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SkuStockInfoServiceImpl implements ISkuStockInfoService {


    @Resource
    private ISkuStockInfoMapper skuStockInfoMapper;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    //@Resource
    //private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GiftProviderCacheKeyBuilder cacheKeyBuilder;

    @Resource
    private ISkuOrderInfoService skuOrderInfoService;

    @Resource
    private MQProducer mqProducer;

    private static final Logger LOGGER = LoggerFactory.getLogger(SkuStockInfoServiceImpl.class);


    private final int maxCnt = 3; // 最大尝试次数


    // 库存扣减相关
    private static final DefaultRedisScript<Long> DECR_STOCK_SCRIPT;
    static {
        DECR_STOCK_SCRIPT = new DefaultRedisScript<>();
        DECR_STOCK_SCRIPT.setLocation(new ClassPathResource("DECR_STOCK_LUA_SCRIPT.lua"));
        DECR_STOCK_SCRIPT.setResultType(Long.class);
    }


    /**
     * 执行扣减库存的lua脚本，具体做的事情就是判断这个商品key是否存在redis中，
     * 如果存在的话就判断是否充足，如果充足就进行扣减操作，并返回扣减之后的数量
     */
    private static final String DECR_STOCK_LUA_SCRIPT =
            "-- 修正点1：去掉多余括号，简化exists判断；修正点2：补全key不存在的返回值\n" +
                    "local key = KEYS[1]\n" +
                    "local deductNum = tonumber(ARGV[1])\n" +
                    "\n" +
                    "-- 1. 检查key是否存在，不存在返回-2（明确标识“库存key不存在”）\n" +
                    "if redis.call('exists', key) ~= 1 then\n" +
                    "    return -2\n" +
                    "end\n" +
                    "\n" +
                    "-- 2. 获取当前库存并转换为数字（防止库存是字符串格式）\n" +
                    "local currentStock = tonumber(redis.call('get', key))\n" +
                    "if not currentStock then  -- 库存不是数字（异常情况），返回-3\n" +
                    "    return -3\n" +
                    "end\n" +
                    "\n" +
                    "-- 3. 库存充足则扣减，否则返回-1\n" +
                    "if currentStock >= deductNum then\n" +
                    "    return redis.call('decrby', key, deductNum)  -- 扣减并返回新库存\n" +
                    "else\n" +
                    "    return -1  -- 库存不足\n" +
                    "end";

//    private static final String DECR_STOCK_LUA_SCRIPT =
//            "if (redis.call('exists', KEYS[1])) == 1 then\n" +
//                    "    local currentStock = redis.call('get', KEYS[1])\n" +
//                    "    if (tonumber(currentStock) >= tonumber(ARGV[1])) then\n" +
//                    "        return redis.call('decrby', KEYS[1], tonumber(ARGV[1]))\n" +
//                    "    else\n" +
//                    "        return -1\n" +
//                    "    end\n" +
//                    "    return -1\n" +
//                    "end";

    /**
     * 进行增加库存，如果对应的key存在的话就进行增加操作，否则返回-1
     */
    private static final String INCR_STOCK_LUA_SCRIPT =
            "if redis.call('exists', KEYS[1]) == 1 then\n" +
                    "    local incrNum = tonumber(ARGV[1])\n" +
                    "    if incrNum == nil or incrNum <= 0 then\n" +
                    "        return -3  -- 参数不合法\n" +
                    "    end\n" +
                    "    return redis.call('incrby', KEYS[1], incrNum)\n" +
                    "else\n" +
                    "    return -4  -- 库存key不存在\n" +
                    "end";

    @Override
    public boolean updateStockNumByTypeAndSkuId(Long skuId, Integer num, Integer type, String msgId) {
        // 已经更新过了不需要继续处理
        String cacheKey = cacheKeyBuilder.buildSyncDBStock(msgId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            return true;
        }

        // 查看数据是否存在
        SkuStockInfoPO stockInfoPO = this.queryBySkuId(skuId);
        if (stockInfoPO == null) {
            LOGGER.info("[updateStockNumByTypeAndSkuId] skuId is {} 不存在", skuId);
            return false;
        }

        // 执行更新操作
        Integer version = stockInfoPO.getVersion();
        LOGGER.info("[updateStockNumByTypeAndSkuId] 当前拿到的版本号 version is {}", version);
        boolean res = false;
        if (type.equals(SyncStockToDBTypeEnum.DECR.getCode())) {
            res = skuStockInfoMapper.decrStockNum(skuId, num, version) > 0;
        } else if (type.equals(SyncStockToDBTypeEnum.INCR.getCode())) {
            res = skuStockInfoMapper.incrStockNum(skuId, num, version) > 0;
        }

        // 表示已经已经处理完成
        if (res) {
            redisTemplate.opsForValue().set(cacheKey, "handel success", 24, TimeUnit.HOURS);
        }
        LOGGER.info("[updateStockNumByTypeAndSkuId] res is {}", res);
        return res;
    }

    @Override
    public boolean updateStockNum(Long skuId, Integer stockNum) {
        LambdaUpdateWrapper<SkuStockInfoPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SkuStockInfoPO::getSkuId, skuId);
        SkuStockInfoPO skuStockInfoPO = new SkuStockInfoPO();
        skuStockInfoPO.setStockNum(stockNum);
        return skuStockInfoMapper.update(skuStockInfoPO, updateWrapper) > 0;
    }

    // 直接操作的mysql
    @Override
    public boolean decrStockNumBySkuId(Long skuId, Integer num) {
        return skuStockInfoMapper.decrStockNumBySkuId(skuId, num);
    }

    /**
     * 通过lua脚本，只操作redis，然后通过定时任务的方式去同步到数据库
     * 事务消息保证消息一定能在redis扣减完成之后
     * @param skuId
     * @param num
     * @return
     */
    public boolean decrStockNumBySkuIdByLua(Long skuId, Integer num) {
        String key = cacheKeyBuilder.buildSkuStock(skuId);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        LOGGER.info("[decrStockNumBySkuIdByLua] 传入参数为 script is {}, key is {}, num is {}", DECR_STOCK_SCRIPT, stringRedisSerializer.serialize(key), num);
        Long res = redisTemplate.execute(DECR_STOCK_SCRIPT, Collections.singletonList(key), String.valueOf(num));
        LOGGER.info("[decrStockNumBySkuIdByLua] 库存扣减之后还剩下{}", res);

        // 异步mysql扣减
        String msgId = UUID.randomUUID().toString();
        this.SendMsgToSyncDBStock(skuId, num, SyncStockToDBTypeEnum.DECR.getCode(), 1, msgId);

        return res != null && res >= 0;
    }

    public boolean incrStockNumBySkuIdByLua(Long skuId, Integer num) {
        String key = cacheKeyBuilder.buildSkuStock(skuId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCR_STOCK_LUA_SCRIPT, Long.class);
        Long res = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(num));
        LOGGER.info("[incrStockNumBySkuIdByLua] 传入参数为 script is {}, key is {}, num is {}", script, key, num);
        LOGGER.info("[incrStockNumBySkuIdByLua] 库存新增之后还有{}", res);

        // 异步mysql增加
        String msgId = UUID.randomUUID().toString();
        this.SendMsgToSyncDBStock(skuId, num, SyncStockToDBTypeEnum.INCR.getCode(), 1, msgId);

        return res != null && res >= 0;
    }


    @Override
    public SkuStockInfoPO queryBySkuId(Long skuId) {
        LambdaQueryWrapper<SkuStockInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SkuStockInfoPO::getSkuId, skuId);
        queryWrapper.eq(SkuStockInfoPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        queryWrapper.last("limit 1");
        return skuStockInfoMapper.selectOne(queryWrapper);
    }

    @Override
    public List<SkuStockInfoPO> queryBySkuIds(List<Long> skuIdList) {
        LambdaQueryWrapper<SkuStockInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SkuStockInfoPO::getSkuId, skuIdList);
        queryWrapper.eq(SkuStockInfoPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        return skuStockInfoMapper.selectList(queryWrapper);
    }

    @Override
    public void stockRollBackHandler(RollBackStockBO rollBackStockBO) {
        LOGGER.info("[stockRollBackHandler] 开始进行库存回滚接口检查");
        // 检验状态
        SkuOrderInfoRespDTO respDTO = skuOrderInfoService.queryByOrderId(rollBackStockBO.getOrderId());
        if (respDTO == null) {
            LOGGER.warn("[stockRollBackHandler] respDTO is null");
            return;
        }

        // 支付状态是已支付就不处理
        Integer status = respDTO.getStatus();
        if (status.equals(SkuOrderInfoEnum.HAS_PAY.getCode())) {
            LOGGER.info("[stockRollBackHandler] 用户订单已经支付完成");
            return;
        }

        // 如果是支付中，判断是否已经达到最大重试次数，如果没有的话，就继续重试，否则人工接入
        // todo 这里发送人工介入消息其实并没有真的实现，而是留为拓展
        if (status.equals(SkuOrderInfoEnum.PAYING.getCode())) {
            LOGGER.info("[stockRollBackHandler] 用户支付中，等待用户支付");
            Integer count = rollBackStockBO.getCount();
            if (count >= maxCnt) {
                LOGGER.info("[stockRollBackHandler] 用户重试次数过多，现在开始强制取消订单  orderId is {}", rollBackStockBO.getOrderId());
                // 修改订单状态
                SkuOrderInfoReqDTO skuOrderInfoReqDTO = new SkuOrderInfoReqDTO();
                skuOrderInfoReqDTO.setStatus(SkuOrderInfoEnum.CANCEL.getCode());
                skuOrderInfoReqDTO.setId(rollBackStockBO.getOrderId());
                // 修改订单状态为已取消状态
                boolean success = skuOrderInfoService.updateOrderStatus(skuOrderInfoReqDTO, SkuOrderInfoEnum.PAYING.getCode());
                // 状态修改失败
                if (!success) {
                    LOGGER.info("[stockRollBackHandler] 修改订单为取消状态失败，orderId is {}", rollBackStockBO.getOrderId());
                    return;
                }
                // todo 发送信息，人工介入

            }
            rollBackStockBO.setCount(count + 1);
            sendRollBackMsg(rollBackStockBO);
            return;
        }


        // 回滚库存
        String skuIdCountList = respDTO.getSkuIdList();
        if (StringUtils.isEmpty(skuIdCountList)) {
            LOGGER.warn("订单 {} 商品列表为空，无需回滚库存", rollBackStockBO.getOrderId());
            return;
        }

        Arrays.stream(skuIdCountList.split(","))
                .forEach(skuIdCount -> {
                    String[] skuCountArr = skuIdCount.split(":");
                    if (skuCountArr.length != 2) {
                        LOGGER.error("订单 {} 商品格式错误：{}，跳过该商品回滚", rollBackStockBO.getOrderId(), skuIdCount);
                        return;
                    }

                    try {
                        Long skuId = Long.valueOf(skuCountArr[0]);
                        int count = Integer.parseInt(skuCountArr[1]);
                        redisTemplate.opsForValue().increment(cacheKeyBuilder.buildSkuStock(skuId), count);
                        LOGGER.info("订单 {} 商品 {} 库存回滚成功，数量：{}", rollBackStockBO.getOrderId(), skuId, count);
                    } catch (Exception e) {
                        LOGGER.error("订单 {} 商品数据解析失败：{}", rollBackStockBO.getOrderId(), skuIdCount, e);
                    }
                });

    }


    // 发送二次延迟消息
    private void sendRollBackMsg(RollBackStockBO rollBackStockBO) {
        LOGGER.info("[sendRollBackMsg] 用户未能及时支付，现在进行第{}次重试，userId is {}, orderId is {}",
                rollBackStockBO.getCount(), rollBackStockBO.getUserId(), rollBackStockBO.getOrderId());
        Message message = new Message();
        message.setTopic(GiftProviderTopicNames.ROLL_BACK_STOCK);
        message.setDelayTimeLevel(8);
        message.setBody(JSON.toJSONBytes(rollBackStockBO));
        try {
            mqProducer.send(message);
        } catch (Exception e) {
            LOGGER.error("[sendRollBackMsg] err， errMsg is {}", e.getMessage());
        }
    }


    // 发送库存同步mq消息
    public boolean SendMsgToSyncDBStock(long skuId, int num, int type, int cnt, String msgId) {
        // 超过最大重试次数
        if (cnt > maxCnt) {
            LOGGER.error("[SendMsgToSyncDBStock] 超过最大重试次数，拒绝重发 skuId is {}，num is {}, " +
                    "type is {}, cnt is {}, msgId is {}", skuId, num, type, cnt, msgId);
            // todo 发送信息给到运维人员
            return false;
        }

        // 封装消息BO对象
        SendMsgToSyncDBStockBO syncDBStockBO = new SendMsgToSyncDBStockBO();
        syncDBStockBO.setSkuId(skuId);
        syncDBStockBO.setNum(num);
        syncDBStockBO.setType(type);
        syncDBStockBO.setCnt(cnt);
        syncDBStockBO.setMsgId(msgId);
        LOGGER.info("[SendMsgToSyncDBStock] skuId is {}, num is {}, type is {}, " +
                "cnt is {}, MsgId is {}", skuId, num, type, cnt, msgId);

        // 发送mq消息
        Message message = new Message();
        message.setBody(JSON.toJSONBytes(syncDBStockBO));
        message.setTopic(SkuStockProviderTopicNames.SEND_MSG_TO_SYNC_DB_STOCK_TOPIC);
        if (cnt > 1) message.setDelayTimeLevel(2); // 不是第一次的话就延时一会，可能是暂时不可用，只要最终一致性就行
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("[SendMsgToSyncDBStock] sendMsg success, sendResult is {}", sendResult);
            return true;
        } catch (Exception e) {
            LOGGER.info("[SendMsgToSyncDBStock] sendMsg error, errMsg is {}", e.getMessage());
            return false;
        }
    }

}
