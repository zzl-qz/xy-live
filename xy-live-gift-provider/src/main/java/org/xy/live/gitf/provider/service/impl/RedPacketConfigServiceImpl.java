package org.xy.live.gitf.provider.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.xy.live.common.interfaces.dto.RedPacketMq;
import org.xy.live.common.interfaces.dto.SendGiftMq;
import org.xy.live.common.interfaces.topic.GiftProviderTopicNames;
import org.xy.live.common.interfaces.utils.ListUtils;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.constants.RedPacketStatusEnum;
import org.xy.live.gift.dto.ReceiveRedPacketHandlerMq;
import org.xy.live.gift.dto.RedPacketConfigReqDTO;
import org.xy.live.gift.dto.RedPacketReceiveDTO;
import org.xy.live.gitf.provider.bo.SyncRedPackDataBO;
import org.xy.live.gitf.provider.dao.mapper.RedPacketConfigMapper;
import org.xy.live.gitf.provider.dao.po.RedPacketConfigPO;
import org.xy.live.gitf.provider.service.IRedPacketConfigService;
import org.xy.live.im.constants.AppIdEnum;
import org.xy.live.im.dto.ImMsgBody;
import org.xy.live.im.router.constants.ImMsgBizCodeEnum;
import org.xy.live.im.router.interfaces.ImRouterRpc;
import org.xy.live.living.interfaces.dto.LivingRoomReqDTO;
import org.xy.live.living.interfaces.rpc.ILivingRoomRpc;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class RedPacketConfigServiceImpl implements IRedPacketConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedPacketConfigServiceImpl.class);

    @Resource
    private RedPacketConfigMapper redPacketConfigMapper;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private GiftProviderCacheKeyBuilder giftProviderCacheKeyBuilder;

    @DubboReference
    private ImRouterRpc imRouterRpc;

    @DubboReference
    private ILivingRoomRpc livingRoomRpc;

    @Resource
    private MQProducer mqProducer;

    @Resource
    private RedissonClient redissonClient;

    // 限制最大重试次数为3
    private int maxCnt = 3;

    /**
     * 记录最大的红包金额脚本
     */
    private static final String MAX_COUNTER_LUA_SCRIPT =
            "local key = KEYS[1]\n" +
                    "local newValue = tonumber(ARGV[1])\n" +
                    "local currentValue = redis.call('GET', key)\n" +
                    "if not currentValue then\n" +
                    "    redis.call('SET', key, newValue)\n" +
                    "    return 1\n" +
                    "end\n" +
                    "currentValue = tonumber(currentValue)\n" +
                    "if newValue > currentValue then\n" +
                    "    redis.call('SET', key, newValue)\n" +
                    "    return 1\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";



    @Override
    public RedPacketConfigPO queryByAnchorId(Long anchorId) {
        LambdaQueryWrapper<RedPacketConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RedPacketConfigPO::getAnchorId, anchorId);
        queryWrapper.eq(RedPacketConfigPO::getStatus, RedPacketStatusEnum.WAIT.getCode());
        queryWrapper.orderByDesc(RedPacketConfigPO::getCreateTime); // 可能一个主播有多条记录，所以这里降序查询最新的那条
        queryWrapper.last("limit 1");
        return redPacketConfigMapper.selectOne(queryWrapper);
    }

    @Override
    public RedPacketConfigPO queryByConfigCode(String code) {
        LambdaQueryWrapper<RedPacketConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RedPacketConfigPO::getConfigCode, code);
        queryWrapper.eq(RedPacketConfigPO::getStatus, RedPacketStatusEnum.IS_PREPARED.getCode());
        queryWrapper.orderByDesc(RedPacketConfigPO::getCreateTime); // 可能一个主播有多条记录，所以这里降序查询最新的那条
        queryWrapper.last("limit 1");
        return redPacketConfigMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean addOne(RedPacketConfigPO redPacketConfigPO) {
        redPacketConfigPO.setConfigCode(UUID.randomUUID().toString());
        return redPacketConfigMapper.insert(redPacketConfigPO) > 0;
    }

    @Override
    public boolean updateById(RedPacketConfigPO redPacketConfigPO) {
        return redPacketConfigMapper.updateById(redPacketConfigPO) > 0;
    }

    @Override
    @Transactional
    public boolean prepareRedPacket(Long anchorId) {
        // 防止重复生成以及参数错误的情况
        RedPacketConfigPO redPacketConfigPO = this.queryByAnchorId(anchorId);
        if (redPacketConfigPO == null) {
            LOGGER.error("参数错误或是已经生成过了");
            return false;
        }
        // 开始生成
        Integer totalPrice = redPacketConfigPO.getTotalPrice();
        Integer totalCount = redPacketConfigPO.getTotalCount();
        String configCode = redPacketConfigPO.getConfigCode();
        // 加锁，防止有多个线程重复执行初始化操作
        RLock lock = redissonClient.getLock(giftProviderCacheKeyBuilder.buildRedPacketInitLock(configCode));
        try {
            boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            try {
                if (!locked) {
                    LOGGER.error("获取锁失败");
                    return false;
                }

                // 可能已经构建好了，可以直接查询判断一下  (判断一下redis就行，不用判断mysql)
                String packetPrepareSuccessKey = giftProviderCacheKeyBuilder.buildRedPacketPrepareSuccess(configCode);
                if (redisTemplate.opsForValue().get(packetPrepareSuccessKey) != null) {
                    LOGGER.error("已经生成过了，不需要重复生成");
                    return false;
                }

                // 修改数据库(事务内，如果失败了就会回滚)
                redPacketConfigPO.setStatus(RedPacketStatusEnum.IS_PREPARED.getCode());
                this.updateById(redPacketConfigPO);

                // 开始进行缓存构建
                List<Integer> readPackList = this.createRedPacketPriceList(totalPrice, totalCount);
                // 这个红包集合可能会很大，所以这里需要拆分一下
                List<List<Integer>> readPackSplitList = ListUtils.splitList(readPackList, 100);
                // 将红包塞到redis的list集合中
                String cacheKey = giftProviderCacheKeyBuilder.buildRedPacketList(configCode);
                for (List<Integer> readPackItemList : readPackSplitList) {
                    redisTemplate.opsForList().leftPushAll(cacheKey, readPackItemList.toArray());
                }
                redisTemplate.expire(cacheKey, 1, TimeUnit.DAYS); // 1天之后过期

                // redis标记一下已经初始化完成
                redisTemplate.opsForValue().set(packetPrepareSuccessKey, 1, 1, TimeUnit.DAYS);
                return true;

            } finally {
                // 锁释放
                lock.unlock();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RedPacketReceiveDTO receiveRedPacket(RedPacketConfigReqDTO redPacketConfigReqDTO) {
        LOGGER.info("[RedPacketReceiveDTO]  redPacketConfigReqDTO is {}", redPacketConfigReqDTO);
        // 在redis中弹出一个红包
        String code = redPacketConfigReqDTO.getRedPacketConfigCode();
        String cacheKey = giftProviderCacheKeyBuilder.buildRedPacketList(code);
        Object readPacketObj = redisTemplate.opsForList().leftPop(cacheKey);
        if (readPacketObj == null) {
            return null;
        }
        int price = (Integer) readPacketObj; // 红包金额

        // 发送一条mq消息去异步处理数据状态记录之类的
        ReceiveRedPacketHandlerMq receiveRedPacketHandlerMq = new ReceiveRedPacketHandlerMq();
        receiveRedPacketHandlerMq.setCode(code);
        receiveRedPacketHandlerMq.setPrice(price);
        receiveRedPacketHandlerMq.setUserId(redPacketConfigReqDTO.getUserId());
        Message message = new Message();
        message.setBody(JSON.toJSONString(receiveRedPacketHandlerMq).getBytes());
        message.setTopic(GiftProviderTopicNames.RECEIVE_RED_PACKET_HANDLER);
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("用户抢红包处理消息mq投递结果：{}", sendResult);
        } catch (Exception e) {
            LOGGER.error("用户抢红包处理消息mq投递失败：", e);
        }

        return new RedPacketReceiveDTO(price, "恭喜领到红包" + price + "虚拟币");
    }

    /**
     * 处理抢红包时的参数记录
     * @param code
     * @param price
     * @param userId
     */
    public void receiveRedPacketHandler(String code, int price, Long userId) {
        LOGGER.info("[receiveRedPacketHandler] 传递进来的参数：code is {}, price is {}, userId is {}", code, price, userId);
        // 记录抢夺了多少个红包
        String redPacketTotalGetCountKey = giftProviderCacheKeyBuilder.buildRedPacketTotalGetCount(code);
        LOGGER.info("[receiveRedPacketHandler] redPacketTotalGetCountKey is {}", redPacketTotalGetCountKey);
        redisTemplate.opsForValue().increment(redPacketTotalGetCountKey);
        redisTemplate.expire(redPacketTotalGetCountKey, 1, TimeUnit.DAYS);

        // 记录总共领了多少线
        String redPacketTotalGetPriceKey = giftProviderCacheKeyBuilder.buildRedPacketTotalGetPrice(code);
        LOGGER.info("[receiveRedPacketHandler] redPacketTotalGetPriceKey is {}", redPacketTotalGetPriceKey);
        redisTemplate.opsForValue().increment(redPacketTotalGetPriceKey, price);
        redisTemplate.expire(redPacketTotalGetPriceKey, 1, TimeUnit.DAYS);

        // 记录每个用户今天总共在直播间领到了多少金额(这个的作用主要是为了方便拓展，因为以后可能会有一张表专门用来汇总一个用户每一天获抢到的红包金额总数是多少)
        String userTotalGetPriceKey = giftProviderCacheKeyBuilder.buildUserTotalGetPrice(userId);
        LOGGER.info("[receiveRedPacketHandler] userTotalGetPriceKey is {}", userTotalGetPriceKey);
        redisTemplate.opsForValue().increment(userTotalGetPriceKey, price);
        redisTemplate.expire(userTotalGetPriceKey, 1, TimeUnit.DAYS);

        // 记录这个用户在这场红包雨中领取到了多少钱
        String getRedPacketPriceKey = giftProviderCacheKeyBuilder.buildGetRedPacketPrice(userId, code);
        LOGGER.info("[receiveRedPacketHandler] getRedPacketPriceKey is {}", getRedPacketPriceKey);
        redisTemplate.opsForValue().increment(getRedPacketPriceKey, price);
        redisTemplate.expire(getRedPacketPriceKey, 1, TimeUnit.DAYS);

        // 记录最大的红包金额（涉及到先将redis中的数据拿出来，再比较写回 ——》lua）
        String packetMaxPriceKey = giftProviderCacheKeyBuilder.buildRedPacketMaxPrice(code);
        LOGGER.info("[receiveRedPacketHandler] packetMaxPriceKey is {}", packetMaxPriceKey);
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript();
        redisScript.setScriptText(MAX_COUNTER_LUA_SCRIPT);
        redisScript.setResultType(Long.class); // 返回值类型
        Long maxUpdateResult = redisTemplate.execute(
                redisScript,
                Collections.singletonList(packetMaxPriceKey), // 键
                String.valueOf(price) // 传递当前红包金额作为比较参数
        );
        redisTemplate.expire(packetMaxPriceKey, 1, TimeUnit.DAYS);

        // 用户领到红包之后需要将金额打到用户账上（发送一条mq进行处理 只需要将用户Id和金额）
        RedPacketMq redPacketMq = new RedPacketMq();
        redPacketMq.setUserId(userId);
        redPacketMq.setPrice(price);
        Message message = new Message();
        message.setBody(JSON.toJSONString(redPacketMq).getBytes());
        message.setTopic(GiftProviderTopicNames.ADD_RED_PACKET_PRICE);
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("用户金额增长mq投递结果：{}", sendResult);
        } catch (Exception e) {
            LOGGER.error("用户金额增长mq投递失败：", e);
        }
    }


    // 需要使用广播机制去通知到直播间的用户，同时前台需要显示出红包雨开始的提示之类的
    @Override
    public Boolean startRedPacket(RedPacketConfigReqDTO reqDTO) {
        String code = reqDTO.getRedPacketConfigCode();
        // 判断一下红包雨活动是不是已经准备好了（初始化完成）
        if (Boolean.FALSE.equals(redisTemplate.hasKey(giftProviderCacheKeyBuilder.buildRedPacketPrepareSuccess(code)))) {
             LOGGER.error("红包雨活动还没初始化完成");
             return false;
        }
        // 因为我们这里需要去基于广播机制通知到用户进行参与抢红包的活动，但是广播的行为也只能发送一次，所以这里判断一下
        // 判断是否广播过了
        String redPacketNotifyKey = giftProviderCacheKeyBuilder.buildRedPacketNotify(code);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redPacketNotifyKey))) {
            LOGGER.error("已经通知过直播间的用户了");
            return false;
        }
        // 开始进行广播通知用户抢红包
        RedPacketConfigPO redPacketConfigPO = this.queryByConfigCode(code);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("redPacketConfigPO", redPacketConfigPO);
        LivingRoomReqDTO livingRoomReqDTO = new LivingRoomReqDTO();
        livingRoomReqDTO.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
        livingRoomReqDTO.setRoomId(reqDTO.getRoomId());
        List<Long> userIds = livingRoomRpc.queryUserIdsByRoomId(livingRoomReqDTO);
        if (CollectionUtils.isEmpty(userIds)) {
            LOGGER.error("直播间没有用户，不需要广播");
            return false;
        }
        this.batchSendImMsg(userIds, ImMsgBizCodeEnum.START_RED_PACKET.getCode(), jsonObject);
        // 修改状态
        redPacketConfigPO.setStatus(RedPacketStatusEnum.IS_SEND.getCode());
        this.updateById(redPacketConfigPO);
        // 标记一下已经广播过了
        redisTemplate.opsForValue().set(redPacketNotifyKey, 1, 1, TimeUnit.DAYS);

        // 30秒后的延迟同步消息
        sendSyncReadPackDataMsg(code, 1);
        return true;
    }


    // 同步红包活动关键数据处理者
    public void SyncRedPackDataHandler(String code, int cnt) {
        LOGGER.info("[SyncRedPackDataHandler] 开始执行同步逻辑");
        // 判断是否已经同步过
        String cacheKey = giftProviderCacheKeyBuilder.buildSyncRedPackDataFlag(code);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            LOGGER.info("[SyncRedPackDataHandler] cacheKey 存在，数据已经同步过，无需重复同步");
            return;
        }

        // 进行同步操作
        LambdaQueryWrapper<RedPacketConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RedPacketConfigPO::getConfigCode, code);
        queryWrapper.eq(RedPacketConfigPO::getStatus, RedPacketStatusEnum.IS_SEND.getCode());
        RedPacketConfigPO redPacketConfigPO = redPacketConfigMapper.selectOne(queryWrapper);
        if (redPacketConfigPO == null || redPacketConfigPO.getId() == null) {
            LOGGER.info("[SyncRedPackDataHandler] redPacketConfigPO 信息不完整， redPacketConfigPO is {}", redPacketConfigPO);
            return;
        }

        // 加锁
        String lock = giftProviderCacheKeyBuilder.buildSyncRedPackDataLock(code);
        Boolean getLockSuccess = redisTemplate.opsForValue().setIfAbsent(lock, 1, 5, TimeUnit.MINUTES);
        LOGGER.info("[SyncRedPackDataHandler] 获取锁情况是 {}", getLockSuccess);
        if (Boolean.TRUE.equals(getLockSuccess)) {
            try {

                // 二次判断
                cacheKey = giftProviderCacheKeyBuilder.buildSyncRedPackDataFlag(code);
                if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                    LOGGER.info("[SyncRedPackDataHandler] cacheKey 存在，数据已经同步过，无需重复同步");
                    return;
                }

                // 总获取数量
                String redPacketTotalGetCountKey = giftProviderCacheKeyBuilder.buildRedPacketTotalGetCount(code);
                int totalGet = Integer.parseInt(redisTemplate.opsForValue().get(redPacketTotalGetCountKey).toString());
                redPacketConfigPO.setTotalGet(totalGet);

                // 总获取金额
                String redPacketTotalGetPriceKey = giftProviderCacheKeyBuilder.buildRedPacketTotalGetPrice(code);
                int totalPrice = Integer.parseInt(redisTemplate.opsForValue().get(redPacketTotalGetPriceKey).toString());
                redPacketConfigPO.setTotalGetPrice(totalPrice);

                // 最大红包金额
                String packetMaxPriceKey = giftProviderCacheKeyBuilder.buildRedPacketMaxPrice(code);
                int maxPrice = Integer.parseInt(redisTemplate.opsForValue().get(packetMaxPriceKey).toString());
                redPacketConfigPO.setMaxGetPrice(maxPrice);


                LOGGER.info("[SyncRedPackDataHandler] totalGet is {}, totalPrice is {}, maxPrice is {}", totalGet, totalPrice, maxPrice);

                // 执行更新操作
                int row = redPacketConfigMapper.updateById(redPacketConfigPO);
                if (row <= 0) {
                    // 重试
                    sendSyncReadPackDataMsg(code, cnt + 1);
                    return;
                }

                // 标识一下修改完成
                redisTemplate.opsForValue().set(cacheKey, "1", 1, TimeUnit.DAYS);
            } catch (NumberFormatException e) {
                LOGGER.error("[SyncRedPackDataHandler] 红包信息同步操作执行失败，errMsg is {}", e.getMessage());
            } finally {
                redisTemplate.delete(lock);
            }
        }
    }

    /**
     * 使用二倍均值法
     * @param totalPrice
     * @param totalCount
     * @return
     */
    private List<Integer> createRedPacketPriceList(Integer totalPrice, Integer totalCount) {
        if (totalPrice < totalCount || totalCount < 0) {
            throw new RuntimeException("当前红包金额或是红包数量设置不合理，请检查");
        }
        List<Integer> res = new ArrayList<>();
        // 前n - 1个用二倍均值法进行生成
        for (int i = 0; i < totalCount - 1; i++) {
            // 当前最大金额就是 (剩余的金额 除以 剩余人数) 再去乘以2，也就是每个人最大只能领到剩余的两份
            int maxPrice = (totalPrice / (totalCount - i)) * 2;
            int currentPrice = ThreadLocalRandom.current().nextInt(1, maxPrice);
            res.add(currentPrice);
            totalPrice -= currentPrice;
        }
        // 最后一个将剩余的数量塞进去
        res.add(totalPrice);
        return res;
    }


    /**
     * 批量发送im消息
     */
    private void batchSendImMsg(List<Long> userIdList, Integer bizCode, JSONObject jsonObject) {
        List<ImMsgBody> imMsgBodies = new ArrayList<>();

        userIdList.forEach(userId -> {
            ImMsgBody imMsgBody = new ImMsgBody();
            imMsgBody.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
            imMsgBody.setBizCode(bizCode);
            imMsgBody.setData(jsonObject.toJSONString());
            imMsgBody.setUserId(userId);
            imMsgBody.setMsgId(UUID.randomUUID().toString());
            imMsgBodies.add(imMsgBody);
        });
        imRouterRpc.batchSendMsg(imMsgBodies);
    }


    /**
     * 30秒后的延迟同步消息
     * @param code
     */
    private void sendSyncReadPackDataMsg(String code, int cnt) {
        if (cnt > maxCnt) {
            LOGGER.warn("[sendSyncReadPackDataMsg] 当前已经超过最大重试次数，发送失败，cnt is {}", cnt);
            return;
        }
        SyncRedPackDataBO syncRedPackDataBO = new SyncRedPackDataBO(code, cnt);
        Message message = new Message();
        message.setTopic(GiftProviderTopicNames.SYNC_RED_PACK_DATA_TOPIC);
        message.setBody(JSON.toJSONBytes(syncRedPackDataBO));
        message.setDelayTimeLevel(4); // 大概30s
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("[sendSyncReadPackDataMsg] sendResult is {}", sendResult);
        } catch (Exception e) {
            LOGGER.error("[sendSyncReadPackDataMsg] errMsg is {}", e.getMessage());
        }
    }

}
